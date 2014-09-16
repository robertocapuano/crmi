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

import run.stub_skeleton.*;
import run.exec.ExecException;
import run.exec.Manager;
import run.reference.*;
import run.session.Callgram;
import run.serialization.Container;
import run.serialization.SizeOf;
import run.serialization.SerializationException;
import run.transport.TransportException;
import run.transport.NetAddress;

import run.Services;
import run.RemoteException;

/**
 * Invia le richieste a remoto
 */

class Out extends Stub implements Services
{
  /**
   * ottimizzazione
   */
  private final static Class Global_class = Global.class;

 /**
   * Servizi forniti al sistema
   */
//  static Service[] services;

  /**
   * Servizi forniti dal Registry
  RegistryServicesReference registry_service_reference;
   */

//  static
//  {
    // aggiunge i servizi del registry per i riferimenti remoti.
//    Service register_svc = new Service( "register", "(Lrio/registry/RegistryServicesReference;)V" );
//    Service lookup_svc = new Service("lookup", "(Ljava/lang/String;)Lrio/registry/RegistryServicesReference;" );
//    services = new Service[] { register_svc, lookup_svc };

//    registry_service_reference = new RegistryServicesReference( null, "Registry", services );
//  }


  /**
   * 1. contatta l'host mandandogli RegistrySR
   * 2. Riceve OK
   */
  static void register( NetAddress remote_registry_address, RegistryServicesReference service_ref ) throws RemoteException, TransportException, SerializationException, ExecException
  {
    RegistryServicesReference remote_registry_reference = new RegistryServicesReference( remote_registry_address, "Registry", Global_class );
    // Lo stub individua sempre tutti i servizi
    Out remote_Registry = new Out( remote_registry_reference );

    remote_Registry.register( service_ref );
  }

  /**
   * 1. Contatta "host" chiedendogli RegistrySR
   */
  static RegistryServicesReference lookup( NetAddress remote_registry_address, String services_name ) throws RemoteException, SerializationException, TransportException, ExecException
  {
    RegistryServicesReference remote_registry_reference = new RegistryServicesReference( remote_registry_address, "Registry", Global_class );
    Out remote_Registry = new Out( remote_registry_reference );

    return remote_Registry.lookup( services_name );
  }

  public Out( RegistryServicesReference registry_ref ) // reference al registry remoto
  {
    super( registry_ref );
  }

  /**
   * Richiesta del servizio remoto: register
   */
  void register( RegistryServicesReference registry_ref ) throws RemoteException, SerializationException, TransportException, ExecException
  {
    int register_framesize = SizeOf.object( registry_ref );

    Container container = new Container( register_framesize );

    // true indica che serializzeremo anche oggetti quindi creare la reference_table
    Container.ContainerOutputStream cos = container.getContainerOutputStream( true );

    cos.writeObject( registry_ref );
    cos.close();

    RemoteServiceReference remote_service_reference = registry_services_reference/*campo ereditato*/.toRemoteServiceReference( Global.register_service.getName(), Global.register_service.getSignature() );
//     "(Lrio/registry/RegistryServicesReference;)V" );
    Callgram callgram = new Callgram( Callgram.CALL, remote_service_reference, null, container );

    Callgram rcv_callgram = Manager.execRemoteCall( callgram );
    callgram.free();
    callgram = null;

    Container res = rcv_callgram.getContainer();

    return;
  }

  /**
   * 1. Contatta "host" chiedendogli RegistrySR
   */
  RegistryServicesReference lookup( String services_name ) throws RemoteException, SerializationException, TransportException, ExecException
  {
    int lookup_framesize = SizeOf.string( services_name );

    Container container = new Container( lookup_framesize );

    // true indica che serializzeremo anche oggetti quindi creare la reference_table
    Container.ContainerOutputStream cos = container.getContainerOutputStream( true );

    cos.writeString( services_name );
    cos.close();

    RemoteServiceReference remote_service_reference = registry_services_reference/* campo ereditato*/.toRemoteServiceReference( Global.lookup_service.getName(), Global.lookup_service.getSignature() );
    Callgram callgram = new Callgram( Callgram.CALL, remote_service_reference, null, container );

    Callgram rcv_callgram = Manager.execRemoteCall( callgram );
    callgram.free();
    callgram = null;

    Container res = rcv_callgram.getContainer();

    // argomento true: il risultato e' un oggetto
    Container.ContainerInputStream cis = res.getContainerInputStream( true );

    RegistryServicesReference registry_ref = (RegistryServicesReference) cis.readObject(  );
    cis.close();

    return registry_ref;
  }

}


