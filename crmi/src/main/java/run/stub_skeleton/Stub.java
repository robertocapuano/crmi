package run.stub_skeleton;

import run.Services;
import run.reference.RemoteServiceReference;

import rio.registry.RegistryServicesReference;


/**
 * classe generata
 * da un registry intelligente (centralizzato) <---
 *
 * Relazioni tra le classi:
 * Services (memorizzato dall'applicazione) == Aegis_Stub == Stub --> RegistryServicesReference
 *
 */


public abstract class Stub implements Services
{
  /**
   * Suffisso delle classi Stub
   */
  public static final String CLASSNAME_SUFFIX = "_Stub";

  /**
   * Indirizzo del server remoto.
   */
  protected RegistryServicesReference registry_services_reference;

  public Stub( RegistryServicesReference ini_reference )
  {
    registry_services_reference = ini_reference;
  }

/*  public static String getClassname( String services_classname )
  {
    return services_classname + CLASSSNAME_SUFFIX;
  }
*/
  public String getClassname(  )
  {
    return getClass().getName();
  }

  public String toString()
  {
    StringBuffer res = new StringBuffer();
    res.append( '<' );
    res.append( getClass().getName() );
    res.append( ">\n" );
    res.append( registry_services_reference.toString() );
    return res.toString();
  }
}

