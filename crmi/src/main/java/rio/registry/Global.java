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

import java.util.*;

import run.reference.*;
import run.Services;
import run.RemoteException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * services_name --> RegistryServicesReference -> RemoteServiceReference -> .. rete ... -> LocalServicesReference
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
 */

public class Global implements Services
{
            private final Logger log = LogManager.getLogger(this.getClass() );
 
  static Service register_service = new Service( "register", "(Lrio/registry/RegistryServicesReference;)V" );
  static Service lookup_service = new Service("lookup", "(Ljava/lang/String;)Lrio/registry/RegistryServicesReference;" );
  static Service[] global_services = new Service[] { register_service, lookup_service };

  /**
   * Mappa services_name --> RegistryServicesReference
   */
  protected static Map global_servicetable = new HashMap();

  /**
   * Registry globale
   * Registra il Servizio individuato da registry_ref
   */
  public static void register( RegistryServicesReference registry_ref ) throws RemoteException//  throws RegistryException
  {
    log.info( "Global.register() invoked: " + registry_ref.toString() );
    String services_name = registry_ref.getServicesName();

    if ( global_servicetable.get( services_name) == null )
    {
      global_servicetable.put( services_name, registry_ref );
    }
    else
    {
      throw new RemoteException( "Services already bounded: " + services_name );
    }
  }

  /**
   * Registry globale
   * Richiesta di un servizio
   */
  public static RegistryServicesReference lookup( String services_name ) throws RemoteException//  throws RegistryException
  {
    log.info( "Global.lookup() invoked: " + services_name );

    RegistryServicesReference registry_ref = (RegistryServicesReference) global_servicetable.get( services_name );

    if (registry_ref != null)
    {
      log.info( registry_ref.toString() );
      return registry_ref;
    }
    else
    {
      throw new RemoteException( "Unknown Services: " + services_name );
    }
  }

}