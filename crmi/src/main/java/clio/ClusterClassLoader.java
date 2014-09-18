/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package clio;

import java.util.*;
import java.net.URL;
import java.io.*;

import de.fub.bytecode.*;
import de.fub.bytecode.classfile.*;

import com.borland.primetime.util.Debug;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.stub_skeleton.Stub;
import run.stub_skeleton.Skeleton;
import run.Utility;

/**
 * ClusterClassLoader contiene i metodi per la condivisione della classi sul Cluster
 *
 * Delegation Model
 * 1) System ClassLoader cerca la classe in rt.jar
 * 2) ExtClassLoader cerca la classe "modificata" in java.ext.dirs
 * 3) AppClassLoader cerca la classe
 *
 * ClassLoader
 * loadClass implementa il delegation model, si effettua l'overiding solo se vi vuole elinare il delgation tree
 * findClass e' il metodo da "overridare" ufficialmente, chiamato da loadClass() dopo che ha effettuato il delegation tree.
 *
 *
 * 1) ClusterClassLoader estende ClassLoader, utilizza la classe Repository di BCEL.
 *    Repository usa gli URL delle directory del CLASSPATH (java.class.path)
 * 2) Agli URL aggiunge quello del repository delle classi dell'utente.
 *
 * -- AppClassLoader hiding:
 * 3) ClusterClassLoader.loadClass() interrompe il delegation tree e carica le applicazioni dal CLASSPATH
 *
 * -- AppClassLoader removing:
 * 3) * ClusterClassLoader viene creato con indicativo di parent pari al ExtClassLoader, in questo modo le classi dell'applicazione
 *    saranno caricate con il nostro ClusterClassLoader
 *
 *
 */

public class ClusterClassLoader extends ClassLoader
{
   private final Logger log = LogManager.getLogger(this.getClass() );
  
  /**
   * URL del repository delle classi dell'utente
   */
  private static File repository_url;

  /**
   * BCEL.ClassPath relativo solo al repository dell'utente
   */
  private static ClassPath repository_classpath;

  /**
   * Istanza del ClusterClassLoader
   */
  private final static ClusterClassLoader cluster_class_loader = new ClusterClassLoader( );

  // Iniziliazzatore dell'URL del repository
  static
  {
    String home_dir = System.getProperty( "user.home" );
    String default_repository_dir = home_dir +  "/repository/";
    String repository_dir = Utility.getStringProperty( "clio.repository.path", default_repository_dir );

    repository_url = new File( repository_dir );
    repository_classpath = new ClassPath( repository_url.getAbsolutePath() );
  }

  /**
   * Tabella delle classi caricate
   */
  private final Map class_repository = new HashMap();

  /**
   * Ritorna l'unica istanza del ClusterClassLoader
   */
  public final static ClusterClassLoader getClusterClassLoader()
  {
    return cluster_class_loader;
  }

  /**
   * Inizializza il ClusterClassLoader.
   * Precarica nel repository interno le classi:
   * 1) Transportable
   * 2) FastTransportable
   * 3) Services
   * 4) Stub
   * 5) Skeleton
   *
   * Imposta come parte l'ExtClassLoader
   */
  public ClusterClassLoader( )
  {
    super( getSystemClassLoader().getParent() );
    Class Transportable_class = javaClassToClass( TypeRepository.Transportable_jclass );
    class_repository.put( Transportable_class.getName(), Transportable_class );

    Class FastTransportable_class = javaClassToClass( TypeRepository.FastTransportable_jclass );
    class_repository.put( FastTransportable_class.getName(), FastTransportable_class );

    Class Services_class = javaClassToClass( TypeRepository.Services_jclass );
    class_repository.put( Services_class.getName(), Services_class );

    Class Stub_class = javaClassToClass( TypeRepository.Stub_jclass );
    class_repository.put( Stub_class.getName(), Stub_class );

    Class Skeleton_class = javaClassToClass( TypeRepository.Skeleton_jclass );
    class_repository.put( Skeleton_class.getName(), Skeleton_class );
  }

  /**
   * Ordine di ricerca:
   * 1. Cerca nel repository interno al class loader
   * 2. Cerca nel repository dell'utente
   * 3. Cerca nel bootclasspath/extclasspath/appclasspath
   * 4. Verifichiamo se e' una classe generabile: stub/skeleton
   * 5. Ci arrendiamo...
   */
  public Class findClass(String class_name) throws ClassNotFoundException
  {
    JavaClass j_cl;
    Class cl;

    // 1. Cerca nel repository interno al class loader
    cl = (Class) class_repository.get( class_name );
    if ( cl == null )
    {
      // 2. Cerca nel repository dell'utente (FastTransportable, _Stub, _Skeleton)
      j_cl = loadFromUserRepository( class_name );
      if ( j_cl == null )
      {
	// 3. Cerca nel bootclasspath/extclasspath/appclasspath
	j_cl = Repository.lookupClass( class_name );
	if ( j_cl == null )
	{
	  // non e' stata trovata: unico caso possibile deve essere generata:
	  // 4. Verifichiamo se e' una classe generabile: stub/skeleton
	  if ( class_name.endsWith(Stub.CLASSNAME_SUFFIX) || class_name.endsWith(Skeleton.CLASSNAME_SUFFIX) )
	  {
	    // findClass() termina qui: la classe, in quanto generata, e' gia' aggiornata
	    j_cl = generateStubSkeleton( class_name );
	  } // end stub/skeleton
	  else
	  {
	    // 5. Ci arrendiamo...
	    throw new ClassNotFoundException( class_name );
	  } // end failed
	} // end boot/ext/app classpath
	else
	{
	  // la classe e' stata trovata nel boot/ext/app classpath
	  // Se non implementa FastTransportable ma solo Transportable la dobbiamo modificare

	  // Ritorna le interfaccie direttamente implementate
	  String[] interface_implemented = j_cl.getInterfaceNames();

	  boolean fasttransportable_implemented = false;
	  boolean transportable_implemented = false;

	  for ( int i=0; i< interface_implemented.length; ++i )
	    if (interface_implemented[i].equals("run.FastTransportable"))
	      fasttransportable_implemented = true;

	  if ( !fasttransportable_implemented )
	  {
	    for ( int i=0; i< interface_implemented.length; ++i )
	      if (interface_implemented[i].equals("run.Transportable"))
		transportable_implemented = true;

	    if ( transportable_implemented )
	    {
	      // altrimenti deve essere modificata.
	      JavaClass modified_j_cl = RWSGenerator.modify( j_cl );
	      storeIntoUserRepository( modified_j_cl );
	      Repository.removeClass( j_cl );
	      Repository.addClass(modified_j_cl);
	      j_cl = modified_j_cl;
  //	    Debug.println( "" + j_cl );
	    } // end if
	  } // end if

	} // end del controllo sulle classi del boot/ext/app classpath
      } // end of user repository
      else
      {
	// la classe e' stata trovata nell'user repository
        // abbiamo quindi la j_cl, va aggiornata?
	// aggiungiamo la classe al repository per permettere l'uso di instanceOf()
	Repository.addClass( j_cl );
	long jcl_time;

	try
	{
	  jcl_time = repository_classpath.getClassFile(class_name).getTime();
	}
	catch ( IOException io_exception )
	{
	  Debug.println( "jcl_time: " + io_exception.toString() );
	  jcl_time = 0;
	}

	// usiamo instanceOf() in quanto non e' detto che j_cl sia estensione diretta di Stub, Skeleton, Transportable
	if (j_cl.instanceOf( TypeRepository.Stub_jclass) || j_cl.instanceOf( TypeRepository.Skeleton_jclass) )
	{
	  String service_name = class_name.substring( 0, class_name.lastIndexOf( '_' ) );

	  // la classe service, si trova nel classpath
	  long service_time = Repository.lookupClassFile( service_name ).getTime();

	  if (service_time>jcl_time)
	  {
	    // lo stub/skeleton va aggiornato.
	    // Rimuoviamo le classi...
	    Repository.removeClass( service_name+Stub.CLASSNAME_SUFFIX );
	    Repository.removeClass( service_name+Skeleton.CLASSNAME_SUFFIX );
	    j_cl = generateStubSkeleton( class_name );
	  }
	} // end stub/skeleton
	else
	if ( j_cl.instanceOf( TypeRepository.FastTransportable_jclass) )
	{
	  // verifichiamo se la classe necessita di aggiornamento
	  // la classe originaria non e' caricata, si accede solo al file .class
	  long master_time = Repository.lookupClassFile( class_name ).getTime();

	  if (master_time>jcl_time)
	  {
	    Repository.removeClass(j_cl);
	    j_cl = Repository.lookupClass(class_name);
	    JavaClass modified_j_cl = RWSGenerator.modify( j_cl );
	    storeIntoUserRepository( modified_j_cl );
	    Repository.removeClass( j_cl );
	    Repository.addClass(modified_j_cl);
	    j_cl = modified_j_cl;
	  }
	} // end FastTransportable

//	Debug.println( "" + j_cl );
      } // end of update

      Debug.assert( j_cl!=null );
      // j_cl e' aggiornata e momorizzata nel Repository

//      Method[] m = j_cl.getMethods();
//      for ( int i=0; i<m.length; ++i )
//	Debug.println( i + ": " + m[i].getName() );
      cl = javaClassToClass( j_cl );
      class_repository.put( cl.getName(), cl );
    } // end class_repository

    return cl;
  }

  Class javaClassToClass( JavaClass j_cl)
  {
    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream( baos );

      j_cl.dump( dos );

      byte[] class_dump = baos.toByteArray();

      Class cl = defineClass( j_cl.getClassName(), class_dump, 0, class_dump.length );
      return cl;
    }
    catch (IOException ioe)
    {
      Debug.println( "javaClassToClass():" +  ioe.toString() );
      return null;
    }
  }

  JavaClass loadFromUserRepository( String class_name )
  {
    try
    {
      ClassPath.ClassFile cpcf = repository_classpath.getClassFile(class_name);
      InputStream is = cpcf.getInputStream();
      ClassParser c_parser = new ClassParser( is, class_name );

      JavaClass j_cl = c_parser.parse();
      return j_cl;
    }
    catch (IOException io_exception)
    {
//      Debug.println( "loadFromUserRepository(): " + io_exception.toString() );
      return null;
    }
  }

  private void storeIntoUserRepository( JavaClass j_cl )
  {
    try
    {
      String class_name = j_cl.getClassName();

      File entry = new File ( repository_url, "" );
      String class_path = class_name.replace( '.', '/' ) + ".class";
      StringTokenizer st = new StringTokenizer( class_path, "/" );

      while ( st.hasMoreTokens() )
      {
	String entry_dir = st.nextToken();
	entry = new File( entry, entry_dir );
	// che non sia l'ultimo.
	if (st.hasMoreTokens() )
	{
	  entry.mkdir();
	}
      }

//      File class_url = new File( repository_url, class_path );
      File class_url = entry;
      FileOutputStream fos = new FileOutputStream( class_url );
      DataOutputStream dos = new DataOutputStream( fos );

      j_cl.dump( dos );
      fos.close();
    }
    catch (IOException io_exception)
    {
      Debug.println( "storeIntoUserRepository(): " + io_exception.toString() );
      return;
    }
  }

 /**
   * Genera JavaClass, Class e le memorizza nei rispettivi Repository.
   * Sono generate sia Stub/Skeleton per il servizio.
   */
  private JavaClass generateStubSkeleton( String class_name ) throws ClassNotFoundException
  {
    String service_name = class_name.substring( 0, class_name.lastIndexOf( '_' ) );

    // Server: la classe del servizio gia' deve essere stata caricata
    // ****** client: la classe si trova nel classpath, non e' detto...
    JavaClass service_jclass = Repository.lookupClass( service_name );

    JavaClass stub_jclass = StubGenerator.generateStubClass( service_jclass );
    JavaClass skeleton_jclass = SkeletonGenerator.generateSkeletonClass( service_jclass );

    storeIntoUserRepository( stub_jclass );
    storeIntoUserRepository( skeleton_jclass );

    Repository.addClass( stub_jclass );
    Repository.addClass( skeleton_jclass );

    if ( class_name.endsWith( Stub.CLASSNAME_SUFFIX) )
    {
      // CROSS-REFERENCE
      Class skeleton_class = javaClassToClass( skeleton_jclass );
      class_repository.put( skeleton_class.getName(), skeleton_class );
      return stub_jclass;
    }
    else
    {
      // CROSS-REFERENCE
      Class stub_class = javaClassToClass( stub_jclass );
      class_repository.put( stub_class.getName(), stub_class );
      return skeleton_jclass;
    }
  }

  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( '<' );
    res.append( getClass().getName() );
    res.append( ">\n" );

    res.append( "repository_url: " );
    res.append( repository_url );
    res.append( '\n' );
    res.append( "class_repository: " );
    res.append( class_repository );
    res.append( '\n' );
    return res.toString();
  }

/*
  boolean selftest()
  {
    boolean res = true;

    JavaClass j_cl, load;

    j_cl = Repository.lookupClass( "clio.XX.XXX" );
    storeIntoUserRepository(j_cl);
    load = loadFromUserRepository("clio.XX.XXX");

    j_cl = Repository.lookupClass( "clio.X" );
    storeIntoUserRepository(j_cl);
    load = loadFromUserRepository("clio.X");

    j_cl = Repository.lookupClass( "X" );
    storeIntoUserRepository(j_cl);
    load = loadFromUserRepository("X");

    return res;
  }
*/



}




	/*
	else
	{
	  // la classe e' stata letta dal repository dell'utente.
	  // quindi puo' necessitare di un aggiornamento.
	  // legge l'orario delle classe
	  try
	  {
	    jcl_time = repository_classpath.getClassFile(class_name).getTime();
	}
	*/

