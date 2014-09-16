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
package run.transport;

import java.net.InetAddress;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.exec.*;
import run.Utility;

public class TransportTable
{
  private final static Logger log = LogManager.getLogger(TransportTable.class );
 
  /**
   * Lista dei transports disponibili
   */
  private static List transport_list = new ArrayList();

  /**
   * Inizializza la lista dei transport interrogando la macchina nativa sul tipo di reti disponibili
   */
  static
  {
    // servirebbe un metodo nativo per leggere l'MTU dell'interfaccia!
    try
    {
      final int buffer_size = Utility.getIntProperty("run.transport.udptransport.buffersize", UDPTransport.SOCKET_BUFFER_SIZE );

      int loopback_port = Utility.getIntProperty("run.transport.udptransport.loopback.port", UDPTransport.LOOPBACK_PORT );
      String loopback_address = Utility.getStringProperty("run.transport.udptransport.loopback.address", "127.0.0.1" );
      IPAddress loopback_ip_address = new IPAddress( InetAddress.getByName( loopback_address ), loopback_port, new byte[] { (byte) 255, 0,0,0 } );
      int loopback_window_size = Utility.getIntProperty( "run.transport.udptransport.loopback.windowsize", UDPTransport.LOOPBACK_WINDOW_SIZE );
      Transport loopback_transport = new UDPTransport( loopback_ip_address, UDPTransport.ETHERNET_PACKET_SIZE, loopback_window_size, buffer_size );
      addTransport( loopback_transport );

      int ethernet_port = Utility.getIntProperty("run.transport.udptransport.ethernet.port", UDPTransport.ETHERNET_PORT );
      String ethernet_address = Utility.getStringProperty("run.transport.udptransport.ethernet.address", "10.0.2.6" );
      IPAddress ethernet_ip_address = new IPAddress( InetAddress.getByName( ethernet_address ), ethernet_port, new byte[] { (byte) 255, (byte) 255, (byte) 255,0 } );
      int ethernet_window_size = Utility.getIntProperty( "run.transport.udptransport.ethernet.windowsize", UDPTransport.ETHERNET_WINDOW_SIZE );
      Transport ethernet_transport = new UDPTransport( ethernet_ip_address, UDPTransport.ETHERNET_PACKET_SIZE, ethernet_window_size, buffer_size );
      addTransport( ethernet_transport );

      int myrinet_port = Utility.getIntProperty("run.transport.udptransport.myrinet.port", UDPTransport.MYRINET_PORT );
      String myrinet_address = Utility.getStringProperty("run.transport.udptransport.myrinet.address", "10.0.4.6" );
      IPAddress myrinet_ip_address = new IPAddress( InetAddress.getByName( myrinet_address ), myrinet_port, new byte[] { (byte) 255, (byte) 255, (byte) 255,0 } );
      int myrinet_window_size = Utility.getIntProperty( "run.transport.udptransport.myrinet.windowsize", UDPTransport.MYRINET_WINDOW_SIZE );
      Transport myrinet_transport = new UDPTransport( myrinet_ip_address, UDPTransport.MYRINET_PACKET_SIZE, myrinet_window_size, buffer_size );
      addTransport( myrinet_transport);
/**/
    }
    catch (Exception exception)
    { log.info( exception.toString() ); }
  }

  /**
   * Restituisce una istanza di trasporto per un tipo di rete.
   * @param type tipo di trasporto
   */
  public static Transport getTransport( NetAddress remote_address ) throws TransportException
  {
    Iterator i;

    for ( i = transport_list.iterator(); i.hasNext(); )
    {
      Transport t = (Transport) i.next();
      NetAddress local_address = t.getLocalAddress();

      if ( local_address.isReachable( remote_address ) )
	return t;
    }

    throw new TransportException( "Transport: " + remote_address + " is unreachable" );
  }

  /**
   * Metodo di debug
   */
  public final static Transport getTransport( int index )
  {
    return (Transport) transport_list.get(index);
  }

  /**
   * Restituisce una istanza di trasporto per un tipo di rete.
   * @param type tipo di trasporto
   */
/*
  public static Transport getTransport( byte type ) throws TransportException
  {
    Iterator i;

    for ( i = transport_list.iterator(); i.hasNext(); )
    {
      Transport t = (Transport) i.next();
      if ( t.getType() == type )
	return t;
    }

    throw new TransportException( "Transport: " + type + " not found" );
  }
*/

  /**
   * Aggiunge il transport alla lista
   */
  protected static void addTransport( Transport t )
  {
    transport_list.add( t );
  }

  /**
   * Restituisce la lista dei transport
   */
  protected static Transport[] getTransportList()
  {
    // forma suggerita dall'API per creare un oggetto Transport[] invece di Object[]
    return (Transport[]) transport_list.toArray( new Transport[0] );
  }

  /**
   * Restituisce l'indirizzo di rete dell'interfaccia che permette di raggiungere remote_address
   */
  public final static NetAddress routeToHost( NetAddress remote_address ) throws TransportException
  {
    Transport transport = getTransport( remote_address );

    return transport.getLocalAddress();
  }

  /**
   * Restituisce l'indirizzo di localhost
   */
  public final static NetAddress getLocalHostAddress( )
  {
    return ((Transport) transport_list.get(0)).getLocalAddress();
  }

  // Sezione debug

  public static String getStatus()
  {
    Transport[] tl = getTransportList();
    String res = "<TransportTable[]>\n";

    int i;

    for ( i=0; i< tl.length; ++i )
    {
      res += "TransportTable[" + i + "]\n";
      res += tl[i].toString();
    }
    return res;
  }

  /**
   * main: alpha test dei transport istanziati
   */
  public static void main( String[] args )
  {
    System.out.println( getStatus() );

  }
  /**
   * Main: effettua l'apha test dei Trasport Istanziati
   * Sintassi: java TransportTable.Main (client/server) 127.0.0.1
  public static void main( String[] args )
  {
    Transport[] transport_list = getTransportList();
//    java.net.InetAddress inet_address = java.net.InetAddress.getByName( args[1]  );

//    IPAddress ip_address = new IPAddress(
    if ( args[0].equalsIgnoreCase("client") )
    {
      for ( int i=0; i<transport_list.length; ++i )
      {
        PacketOne packet = new PacketOne( transport_list[i].getPacketSize() );
	packet.setInit( transport_list[i].getAddress(), 0, 10, 0, "", "" );
	packet



	transport_list[i].se
    }
    else
    if ( args[0].equalsIgnoreCase("server") )
    {
    }
    return;
  }
   */

}