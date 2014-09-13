package run.stub_skeleton;

import run.*;
import run.serialization.Container;
import run.reference.RemoteServiceReference;

/**
 *  Relazioni tra le classi:
 *  services_name -> LocalServicesReference -- > Skeleton --> Services
 */

public abstract class Skeleton
{
  /**
   * Suffisso delle classi Skeleton
   */
  public static final String CLASSNAME_SUFFIX = "_Skeleton";

  /**
   * Riferimento al servizio a cui lo skeleton e' associato
   */
  protected Services services;
//  protected LocalServicesReference services_reference;

  public Skeleton( Services _services )
  {
//    remote_reference = remote_ref;
    services = _services;
  }

/*  public static String getClassname( String services_classname )
  {
    return services_classname + CLASSNAME_SUFFIX;
  }
*/

  public String getClassname( )
  {
    return getClass().getName();
  }

  public final Services getServices()
  {
    return services;
  }

  public final void setServices( Services services_ref )
  {
    services = services_ref;
  }

}
