package clio;

import run.Transportable;

import java.util.*;
import java.io.*;
import java.net.*;

import de.fub.bytecode.*;
import de.fub.bytecode.classfile.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Clio: Class Loader IntrOspector
 * La classe Clio implementa un classloader
 *
 * 1) Cerca la classe, mediante lookupClassFile.
 * 2) Se la trova la carica in memoria e definisce la Class
 * 3) Se la classe implementa Transportable allora genera il JavaClass
 * 4) Modifica il JavaClass
 * 5) dump.
 *
 * Bisogna generare prima Class eppoi JavaClass, fare una intr
 * o viceversa?
 * Partiamo dall'euristica che siano di piu' le classi non Transportable quindi per cui non serve la JavaClass.
 *
 * Oss.: non usiamo il de.fub.bytecode.Repository in quanto la modifica di una classe avviene solo una volta.
 */

public class Clio extends ClassLoader // implements Serializable
{
         private final Logger log = LogManager.getLogger(this.getClass() );
 
  private Map class_repository = new HashMap();

  public Clio()
  {
  }

  protected synchronized Class loadClass( String name, boolean resolve ) throws ClassNotFoundException
  {
    JavaClass j_cl;
    Class cl = (Class) class_repository.get( name );

    if ( cl == null)
    {
      ClassPath.ClassFile cpcf = Repository.lookupClassFile( name /* + ".class"*/ );

      if (cpcf == null)
      {
	return findClass( name );
//	return getParent().findClass( name );
//	throw new ClassNotFoundException( name );
      }
      else // cpcf valido
      {
	// definiamo Class ed eventualmente JavaClass con una sola lettura dal file system (!)
	try
	{
	  InputStream is = cpcf.getInputStream();
	  int c_len = (int) cpcf.getSize();
	  byte c_buffer[] = new byte[c_len];
	  // lettura dal file system
	  is.read( c_buffer, 0, c_len );

	  // Produce la Class
	  cl = defineClass( name, c_buffer, 0, c_len );

	  class_repository.put( name, cl );

	  Class[] c_interfaces = cl.getInterfaces();

	  int i;

	  for ( i=0; i<c_interfaces.length && ( c_interfaces[i] != Transportable.class ) ; ++i );

	  if ( i<c_interfaces.length )
	  {
	    // la classe implementa Transportable

	    // Produce la JavaClass
	    ByteArrayInputStream bais = new ByteArrayInputStream( c_buffer, 0, c_len );
	    ClassParser c_parser = new ClassParser( bais, name );
	    j_cl = c_parser.parse();

	    RWSGenerator rws = new RWSGenerator( j_cl );
	    rws.generate();
	  }
	}
	catch (IOException io_exception)
	{
	  throw new ClassNotFoundException( io_exception.getMessage() );
	}


      }


    }

    if (resolve) resolveClass( cl );

    return cl;

  }

  public void add( Class new_class )
  {
  }

  private byte[] loadClassBytes( String c_name )
  {
//    URL load_url = new URL( "file:./"+ name + ".class2" );
//    InputStream is = load_url.openStream();
    FileInputStream fis_class;
    File f_class = new File( "./" + c_name + ".class2" );
    int c_len = (int) f_class.length();

    try { fis_class = new FileInputStream( f_class ); }
    catch ( FileNotFoundException fnfe ) { return null; }

    byte[] c_buffer = new byte[ c_len ];

    try { fis_class.read( c_buffer, 0, c_len ); }
    catch ( IOException ioe ) { return null; }
    return c_buffer;
  }

  private void doIt( JavaClass j_cl )
  {
    Method[] m = j_cl.getMethods();
    for ( int i=0; i<m.length; ++i )
      System.out.println( m[i] );
  }


/*
  private Class javaClassToClass()
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream( baos );

    j_cl.dump( dos );

    byte[] class_dump = baos.toByteArray();

    Class d_cl = defineClass( name, class_dump, 0, class_dump.length );

    return d_cl;
  }
*/
}