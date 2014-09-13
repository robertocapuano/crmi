package run.reference;

import run.stub_skeleton.Skeleton;

/**
 * Lato Server.
 * Riferimento al servizio. Contiene tutta la lista dei metodi del servizio.
 */

public class LocalServicesReference
{
  /**
   * Nome del servizio
   */
  String services_name;

  /**
   * Nomi e firme dei metodi disponibili per il servizio
   */
  Service[] service_list;

  /**
   * Reference all'oggetto che implementa il servizio.
   */
  Skeleton skeleton;

  public LocalServicesReference( String _services_name, Service[] _methods, Skeleton _skeleton )
  {
    services_name = _services_name;
    service_list = _methods;
    skeleton = _skeleton;
  }

  public final String getServicesName()
  {
    return services_name;
  }

  public final Skeleton getSkeleton()
  {
    return skeleton;
  }

  public final void setSkeleton( Skeleton _skeleton )
  {
    skeleton = _skeleton;
  }

  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( '<' );
    res.append( getClass().getName() );
    res.append( ">\n" );

    res.append( "services_name: " );
    res.append( services_name );
    res.append( '\n' );

    for ( int i=0; i<service_list.length; ++i )
      res.append( service_list[i].toString() );

    return res.toString();
  }

}
