package run.reference;

import java.net.InetAddress;

import run.serialization.Container;
import run.transport.NetAddress;

/**
 * Lato client.
 * Rappresenta un riferimento (indirizzo) di un oggetto (servizio) remoto.
 * Utilizzato per l'invocazione remota.
 */

public class RemoteServiceReference
{

  /**
   * Indirizzo dell'host: usato dal transport del lato client per trasmettere il callgramm
   */
  private NetAddress address;

  /**
   * Servizi (oggetto)
   */
  private String services_name;

  /**
   * Nome e firma del metodo del servizio invocato
   */
  Service service;

  public RemoteServiceReference( NetAddress service_net_address, String _services_name, Service _service_descriptor )
  {
    address = service_net_address;
    services_name = _services_name!= null ? _services_name : "";
    service = _service_descriptor!=null ? _service_descriptor : new Service("","");
  }

  public final String getServiceName()
  {
    return service.name;
  }

  public final String getServiceSignature()
  {
    return service.signature;
  }

  public final String getServiceNumericSignature()
  {
    return (service.signature.hashCode()+"").replace('-', '_');
  }

  public final String getServicesName()
  {
    return services_name;
  }

  public final NetAddress getAddress()
  {
    return address;
  }

  public String toString()
  {
    String res = "<";
    res += "Services: " + getServicesName() + ", ";
    res += "ServiceName: " + getServiceName() + ", ";
    res += "ServiceSignature: " + getServiceSignature() + ", ";
    res += "RemoteAddress: " + getAddress() + ", ";
    res += ">\n";

    return res;
  }

}
