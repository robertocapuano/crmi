/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> Capuano <Roberto Capuano <roberto@2think.it>@2think.it>
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
package rio.registry;

import java.util.ArrayList;
/*
import java.lang.reflect.*;
*/


import de.fub.bytecode.*;
import de.fub.bytecode.classfile.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.stub_skeleton.Skeleton;
import run.serialization.*;
import run.transport.NetAddress;

import run.Services;
import run.FastTransportable;
import run.reference.*;
import run.serialization.SizeOf;

/**
 * Rappresenta il record di un servizio implementato su un host.
 * Il record e' memorizzato nel Registry. Viene spedito attraverso la rete, da un registry ad un altro.
 * Tramite i callgram.
 * Il constructor si occupa di analizzare la classe per ricavare le firme dei servizi.
 */

public class RegistryServicesReference implements FastTransportable
{
                       private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * Indirizzo dell'host su cui e' implementato il servizio
   */
  NetAddress remote_address;

  /**
   * Nome dei servizi implementati. Es.: SlideShow
   */
  String services_name;

  /**
   * Nome della classe che implementa i servizi, il file Class sara' ricavato via ClassLoader
   * Il tipo serve al registry per caricare lo Stub.
   * Es.: aegis.Aegis
   */
  String services_classname;

  /**
   * Nomi e firme dei metodi disponibili sul servizio.
   * Es.: get(),           (V)V
   *      String[] list()  (V)[java/lang/String;
   */
  Service[] service_list;


/*
  public RegistryServicesReference( LocalServicesReference lsr )
  {

  }
*/

  RegistryServicesReference( NetAddress _remote_address, String _services_name, Class services_impl )
  {
    remote_address = _remote_address;
    services_name = _services_name;
    services_classname = services_impl.getName();
//    String services_impl_classname =

    // services_full_classname == "aegis.Server"
    //    String services_full_classname = services.getClass().getName();
    // services_classname == "Server"
    //    services_classname = services_full_classname.substring( services_full_classname.lastIndexOf('.')+1, services_full_classname.length() );
    // services_classname = "interface Rs"
    //    services_classname = services.getClass().getInterfaces[0];


    // by-passiamo ClusterClassLoader per avere alla JavaClass
    JavaClass services_clazz = Repository.lookupClass(services_classname);

    try
    {

      if (services_clazz==null)
      {
	services_clazz = new ClassParser( services_classname ).parse();
      }

      Method[] method_list = services_clazz.getMethods();

//      Method[] method_list = services_impl.getMethods();

      ArrayList service_arraylist = new ArrayList();

      for ( int i=0; i<method_list.length; ++i )
      {
	ExceptionTable exception_table = method_list[i].getExceptionTable();
//	Class exception_table[] = method_list[i].getExceptionTypes();
	if (exception_table!=null)
	{
	  String[] exception_names = exception_table.getExceptionNames();

	  search_remote_exception:
	    for ( int j=0; j<exception_names.length; ++j )
  //	  for ( int j=0; j<exception_table.length; ++j )
	    {
	      // alla ricerca di metodi remoti
	      if (exception_names[j].equals("run.RemoteException"))
  //	    if (exception_table[j].equals("run.RemoteException"))
	      {
		service_arraylist.add( new Service( method_list[i].getName(), method_list[i].getSignature() ) );
		break search_remote_exception;
	      }
	    } // end search_remote_exception
	}
      }

      service_list = (Service[]) service_arraylist.toArray( new Service[0] );
    }
    catch (java.io.IOException ioe )
    {
      log.info( "Class: " + services_classname + " not loadable" );
    }



//    services_list = new Service[methods.length];

//    for ( int i=0; i<methods.length; ++i )
//    {
//      services_list[i] = new Service( methods[i].getName, methods[i].getSignature() );
//    }

  }

  public final String getServicesName()
  {
    return services_name;
  }

  public final String getServicesClassname()
  {
    return services_classname;
  }

  /**
   * Fase di registrazione, lato server.
   */
  LocalServicesReference toLocalServicesReference( Skeleton _skeleton )
  {
    LocalServicesReference lsr = new LocalServicesReference( services_name, service_list, _skeleton );
    return lsr;
  }

  /**
   * Fase di invocazione, lato client.
   */
  public RemoteServiceReference toRemoteServiceReference( String name, String signature )
  {
    // codice per il debug
    int i;

    for ( i=0; i<service_list.length; ++i )
    {
      if (service_list[i].getName().equals( name ) && service_list[i].getSignature().equals(signature) )
      {
	break;
      }
    }

    // precondizione: il servizio esiste
    Debug.assert( i<service_list.length );

    RemoteServiceReference rsr = new RemoteServiceReference( remote_address, services_name, service_list[i] );

    return rsr;
  }

  /**
   * Fase di invocazione, lato client.
   */
  RemoteServiceReference toRemoteServiceReference( int i )
  {
    Debug.assert( i< service_list.length );

    RemoteServiceReference rsr = new RemoteServiceReference( remote_address, services_name, service_list[i] );

    return rsr;
  }

  public void setRemoteAddress( NetAddress _remote_address )
  {
    remote_address = _remote_address;
  }

  /**
   * Serializzazione: costruttore di default
   */
  public RegistryServicesReference()
  {
  }

  /**
   * Serializzazione: writeObject()
   */
  public final void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeObject( remote_address );
    cos.writeString( services_name );
    cos.writeString( services_classname );
    cos.writeArray( service_list );
  }

  /**
   * Serializzazione: readObject()
   */
  public final void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
    remote_address = (NetAddress) cis.readObject();
    services_name = cis.readString();
    services_classname = cis.readString();
    service_list = (Service[]) cis.readArray( );
  }

  public final int sizeOf()
  {
    return SizeOf.object( remote_address ) + SizeOf.string(services_name) + SizeOf.string(services_classname) + SizeOf.array(service_list);
  }

  public final static long SUID = clio.SUID.getSUID( RegistryServicesReference.class );
//  public final static long SUID = java.io.ObjectStreamClass.lookup( RegistryServicesReference.class ).getSerialVersionUID();

  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( '<' );
    res.append( getClass().getName() );
    res.append( ">\n" );
    res.append( remote_address.toString() );
    res.append( '\n' );
    res.append( "services_name: " );
    res.append( services_name );
    res.append( '\n' );
    res.append( "services_classname: " );
    res.append( services_classname );
    res.append( '\n' );

    for ( int i=0; i<service_list.length; ++i )
      res.append( service_list[i].toString() );
    return res.toString();
  }
}