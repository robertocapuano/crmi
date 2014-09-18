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
package rio.registry;


import run.stub_skeleton.*;
import run.reference.*;
import run.transport.*;
import run.Services;

import run.RemoteException;
import run.serialization.SerializationException;
import run.exec.ExecException;
import run.transport.TransportException;

import clio.ClusterClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Interroga il repository remoto
 * Il Registry fornisce servizi ad alto livello.
 */

public class Registry
{
                    private final static Logger log = LogManager.getLogger(Registry.class );
 
  /**
   * Registra il servizio nel registry remoto sul nodo "host".
   * Associa l'istanza dell'oggetto che implementa l'interfaccia  Services al service name, il nome dei metodi lo ricava per introspezione
   * 1. Crea il RegistryServicesReference con
   *    - il services_name fornito,
   *    - nomi dei metodi
   *    - firma dei metodi (ricavati per introspezione su Services),
   *    - nome della classe che implementa i servizi,
   *    - ed indirizzo locale ricavato dal **transport** che collega alla stessa rete che collega l'host remote,
   * 2. Crea le classi stub/skeleton tramite SSGenerator
   * 3. Istanzia uno Skeleton, lo skeleton memorizza un riferimento al servizio.
   *    Questo e' il parametro services.
   * 4. Crea il LocalServicesReference con
   *    - il services_name
   *    - il nome e la firma dei metodi ricavati dal punto 1
   *    - il riferimento allo skeleton.
   * 5. Memorizza nel Registry locale l'associazione:
   *    service_names -> LocalServiceReference
   * 6. Se "host" e' remoto, invia RegistryServicesReference al Registry remoto,
   *    in modo da essere rintracciato.
   *
   */
  public static void register( NetAddress registry_address, String services_name, Services services ) throws RegistryException
  {
    try
    {
      log.info( "Registry(): registrazione remota del servizio" );
      // parte remota
      NetAddress localhost = TransportTable.routeToHost( registry_address );
      RegistryServicesReference registry_ref = new RegistryServicesReference( localhost, services_name, services.getClass() );
      Out.register( registry_address, registry_ref );


      log.info( "Registry(): registrazione locale del servizio" );
      // parte locale
      // Relazioni tra le classi:
      // services_name -> LocalServicesReference -- > Skeleton --> Services

      // lo skeleton contiene il riferimento all'istanza del servizio locale.
      // Skeleton -> Services
      Skeleton skeleton = Local.newSkeleton( registry_ref, services );
      LocalServicesReference local_ref = registry_ref.toLocalServicesReference( skeleton );
      // LocalServicesReference --> Skeleton
  //    local_ref.setSkeleton( skeleton );
      // services_name --> LocalServicesReference
//      log.info( local_ref.toString() );
      Local.register( local_ref );
    }
    catch (SerializationException serialization_exception) { throw new RegistryException( "serialization exception: " + serialization_exception.getMessage() ); }
    catch (TransportException transport_exception) { throw new RegistryException( "transport exception: " + transport_exception.getMessage() ); }
    catch (ExecException exec_exception ) { throw new RegistryException( "exec exception: " + exec_exception.getMessage() ); }
    catch (RemoteException remote_exception ) { throw new RegistryException( "remote exception: " + remote_exception.getMessage() ); }
  }

  /**
   * Restituisce lo stub che implementa l'interfaccia di servizi
   * 1. Contatta il registry remoto sul nodo "host" passandogli il services_name
   * 2. Il repository Remoto contiene l'associazione da services_name --> RemoteServiceReference
   * 3. Restituisce il RegistryServicesReference, che contiene il nome della classe che implementa Services
   * 4. Tramite CLIO carica lo Stub, inizializza lo stub con il riferimento remoto RegistryServicesReference
   * 5. Istanzia uno stub
   * 6. Restituisce lo stub
   */
  public static Services lookup( NetAddress registry_address, String services_name ) throws RegistryException
  {
    try
    {
      RegistryServicesReference registry_ref = Out.lookup( registry_address, services_name );
      // Services (memorizzato dall'applicazione) == Stub --> RegistryServicesReference
      // lo stub contiene il riferimento remoto: RegistryServicesRef
      Stub services_stub = Local.newStub( registry_ref );
      return services_stub;
    }
    catch (TransportException transport_exception) { throw new RegistryException( transport_exception.getMessage() ); }
    catch (SerializationException serialization_exception) { throw new RegistryException( serialization_exception.getMessage() ); }
    catch (ExecException exec_exception ) { throw new RegistryException( exec_exception.getMessage() ); }
    catch (RemoteException remote_exception ) { throw new RegistryException( remote_exception.getMessage() ); }
  }


  /*
   * Versione debug, host=localhost
  public static void register( String services_name, Services services ) throws RegistryException
  {
    try
    {
      // parte globale sul registry remoto
//      java.net.InetAddress ip_localhost = java.net.InetAddress.getByName("127.0.0.1");
//      NetAddress registry_address = new IPAddress( ip_localhost,
//      getTrouteToHost( new IPAddress(
      NetAddress localhost = TransportTable.getTransport(0).getLocalAddress(); // 127.0.0.1
      RegistryServicesReference registry_ref = new RegistryServicesReference( localhost, services_name, services.getClass() );
      // bypassa In,Out
      Global.register( registry_ref );

      // parte locale
      // Relazioni tra le classi:
      // services_name -> LocalServicesReference -- > Skeleton --> Services

      // lo skeleton contiene il riferimento all'istanza del servizio locale.
      // Skeleton -> Services
      Skeleton skeleton = Local.newSkeleton( registry_ref, services );
      LocalServicesReference local_ref = registry_ref.toLocalServicesReference( skeleton );
      // LocalServicesReference --> Skeleton
  //    local_ref.setSkeleton( skeleton );
      // services_name --> LocalServicesReference
      Local.register( local_ref );
    }
//    catch (TransportException transport_exception) { throw new RegistryException( transport_exception.getMessage() ); }
//    catch (ExecException exec_exception ) { throw new RegistryException( exec_exception.getMessage() ); }
    catch (RemoteException remote_exception ) { throw new RegistryException( remote_exception.getMessage() ); }
  }
 /*
   */

  /*
   * Versione debug, host=localhost
  public static void register( String services_name, Services services, NetAddress service_address) throws RegistryException
  {
    try
    {
      // parte globale sul registry remoto
//      java.net.InetAddress ip_localhost = java.net.InetAddress.getByName("127.0.0.1");
//      NetAddress registry_address = new IPAddress( ip_localhost,
//      getTrouteToHost( new IPAddress(
      RegistryServicesReference registry_ref = new RegistryServicesReference( service_address, services_name, services.getClass() );
      // bypassa In,Out
      Global.register( registry_ref );

      // parte locale
      // Relazioni tra le classi:
      // services_name -> LocalServicesReference -- > Skeleton --> Services

      // lo skeleton contiene il riferimento all'istanza del servizio locale.
      // Skeleton -> Services
//      Skeleton skeleton = Local.newSkeleton( registry_ref, services );
//      LocalServicesReference local_ref = registry_ref.toLocalServicesReference( skeleton );
      // LocalServicesReference --> Skeleton
  //    local_ref.setSkeleton( skeleton );
      // services_name --> LocalServicesReference
//      Local.register( local_ref );
    }
//    catch (TransportException transport_exception) { throw new RegistryException( transport_exception.getMessage() ); }
//    catch (ExecException exec_exception ) { throw new RegistryException( exec_exception.getMessage() ); }
    catch (RemoteException remote_exception ) { throw new RegistryException( remote_exception.getMessage() ); }
  }

  public static Services lookup( String services_name ) throws RegistryException
  {
    try
    {
      RegistryServicesReference registry_ref = Global.lookup( services_name );
      // Services (memorizzato dall'applicazione) == Stub --> RegistryServicesReference
      // lo stub contiene il riferimento remoto: RegistryServicesRef
      Stub services_stub = Local.newStub( registry_ref );
      return services_stub;
    }
    catch (RemoteException remote_exception ) { throw new RegistryException( remote_exception.getMessage() ); }
  }

 */

}
