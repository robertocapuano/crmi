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
