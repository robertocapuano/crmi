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

import java.lang.reflect.*;
import java.util.*;


import run.Services;
import run.reference.*;
import run.stub_skeleton.*;

import clio.ClusterClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contiene l'associazione tra servizi ed oggetti locali.
 * Viene interrogato dal Dispatcher per ricevere il LocalServiceReference
 *
 * <PRE>
 *
 *   Service                  *Global              Client
 *     |                         /\                  \/
 *     \/                         |                  |
 *  *Registry.register          *In             *Registry.lookup
 *     |                          |                  |
 *     |                          |                  |
 *    *Out                      *Local             *Out
 *     |                          |                  |
 *    Manager                  Dispather           Manager
 *       |                        |                  |
 *       --------------------------------------------|
 *
 *
 *      Service                  Client
 *         |                       |
 *         /\                      \/
 *       Skeleton                Stub
 *          |                      |
 *          |                      |
 *        *Local                 *Local
 *          |                      |                      |
 *      Dispatcher               Manager
 *          |                       |
 *          -------------------------
 *
 *
 *
 *
 *
 * </PRE>
 * service_name -> LocalServicesReference -> Aegis_Skeleton
 * I metodi dello Skeleton sono dichiarati in LocalServicesReference
 */

public class Local implements Services
{
                  private final static  Logger log = LogManager.getLogger(Local.class );
 
  /**
   * Mappa services_name --> LocalServicesReference
   */
  protected static Map local_servicetable = new HashMap();

  static
  {
    // aggiunge i servizi del registry, al pool di servizi locali
    // i servizi del registry sono hardcoded nella tabella dei servizi
//    Service register_svc = new Service( "register", "(Lrio/registry/RegistryServicesReference;)V" );
//    Service lookup_svc = new Service("lookup", "(Ljava/lang/String;)Lrio/registry/RegistryServicesReference;" );
//    Service[] services = new Service[] { register_svc, lookup_svc };

//    Service register_svc = new Service( "register", "(Lrun/reference/LocalServicesReference;)V" );
//    Service lookup_svc = new Service("lookup", "(Lrun/reference/RemoteServiceReference;)Lrun/reference/LocalServicesReference;" );
//    Service[] services = new Service[] { register_svc, lookup_svc };

    // "Registry" -> LocalServicesReference -> In
    LocalServicesReference local_ref = new LocalServicesReference( "Registry", Global.global_services, new In( null /*servizi sono tutti static*/ ) );
    local_servicetable.put( "Registry", local_ref );
  }


  /**
   * Interrograzioni su servizi locali, Memorizza un servizio locale
   * Associa services_name al LSR
   */
  public static void register( LocalServicesReference lsr ) throws RegistryException
  {
    String services_name = lsr.getServicesName();
    log.info( "register(): services_name: " + services_name );

    if ( local_servicetable.get( services_name )==null )
      local_servicetable.put( services_name, lsr );
    else
      throw new RegistryException( "Services already bounded: " + services_name );
  }

  /**
   * Interrogazioni su servizi locali, restituisce LSF dato RSF interrogato dal Dispatcher
   */
  public static LocalServicesReference lookup( RemoteServiceReference remote_ref ) throws RegistryException
  {
    String services_name = remote_ref.getServicesName();

    log.info( "lookup(): services_name: " + services_name );
    LocalServicesReference local_ref = (LocalServicesReference) local_servicetable.get( services_name );
    if (local_ref!=null)
      return local_ref;
    else
      throw new RegistryException( "Unknown services exception: " + services_name );
  }

  static Stub newStub( RegistryServicesReference registry_ref ) throws RegistryException
  {
    Class stub_class;
    String services_classname = registry_ref.getServicesClassname();

    try
    {
      stub_class = Class.forName( services_classname+Stub.CLASSNAME_SUFFIX );
//      stub_class = ClusterClassLoader.getClusterClassLoader().loadClass( services_classname+Stub.CLASSNAME_SUFFIX );
      Class[] params = new Class[] { RegistryServicesReference.class };
      Constructor constructor = stub_class.getConstructor( params );

      Stub service_stub = (Stub) constructor.newInstance( new Object[] { registry_ref } );

      return service_stub;
    }
    catch (ClassNotFoundException cnfe )
    {
      // Questo errore non si dovrebbe mai verificare
      throw new RegistryException(  "services_classname: " + services_classname + " not found"  );
    }
    catch ( Exception reflection_exception ) { throw new RegistryException( reflection_exception.getMessage() ); }
  }

  static Skeleton newSkeleton( RegistryServicesReference registry_ref, Services _services ) throws RegistryException
  {
    Class skeleton_class;
    // services_classname = "Rs"
    String services_classname = registry_ref.getServicesClassname();

    try
    {
      // Skeleton.getClassname() == "Aegis"
//      skeleton_class = ClusterClassLoader.getClusterClassLoader().loadClass( services_classname + Skeleton.CLASSNAME_SUFFIX  );
      skeleton_class = Class.forName( services_classname + Skeleton.CLASSNAME_SUFFIX  );
      Class[] params = new Class[] { Services.class };
      Constructor constructor = skeleton_class.getConstructor( params );

      Skeleton service_skeleton = (Skeleton) constructor.newInstance( new Object[] { _services } );

      return service_skeleton;
    }
    catch ( ClassNotFoundException cnfe )
    {
      // Questo errore non si dovrebbe mai verificare
      throw new RegistryException( "services_classname: " + services_classname + " not found"  );
    }
    catch ( Exception reflection_exception ) { throw new RegistryException( reflection_exception.getMessage() ); }
  }

}
