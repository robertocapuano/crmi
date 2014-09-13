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