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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import run.Utility;

import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

/**
 * IPv4 Address
 * 4 bytes per l'indirizzo
 * 2 bytes per la porta.
 */

public class IPAddress extends NetAddress
{
         private final static Logger log = LogManager.getLogger(IPAddress.class );
 
  /**
   * Uso di InetAddress
   */
  private InetAddress inet_address;

  /**
   * Numero di porta
   */
  private int port;

  /**
   * Netmask, facoltativa
   */
  byte[] net_mask;

  public IPAddress( InetAddress i_addr, int _port )
  {
    inet_address = i_addr;
    port = _port;
  }

  public IPAddress( InetAddress i_addr, int _port, byte[] _net_mask )
  {
    inet_address = i_addr;
    port = _port;
    net_mask = _net_mask;
  }

  public InetAddress toInetAddress()
  {
    return inet_address;
  }

  public final byte[] getByteAddress()
  {
    return inet_address.getAddress();
  }

  public final byte[] getBytePort()
  {
    byte[] p = new byte[2];

    p[0] = (byte) ((port >>> 8) & 0xFF);
    p[1] = (byte) (port & 0xFF);

    return p;
  }

  public final void setPort( int _port )
  {
    port = _port;
  }

  public final int getPort()
  {
    return port;
  }

  public final byte getType()
  {
    return IP_ADDRESS;
  }

  public final byte[] getByteArray()
  {
    byte[] addr = getByteAddress();
    byte[] p = getBytePort();

    byte[] result = new byte[ sizeOf() ];

    int i;

    for ( i=0; i<addr.length; ++i )
      result[i] = addr[i];

    for ( int c=0 ; c<p.length; ++i, ++c )
      result[i] = p[c];

    return result;
  }

  /**
   * L'indirizzo remote e' raggiungibile da questa rete ?
   */
  public boolean isReachable( NetAddress remote_net_address )
  {
    if (this==remote_net_address)
      return true;
    else
    if (remote_net_address.getClass() != getClass() )
      return false;
    else
    {
      IPAddress remote_ip_address = (IPAddress) remote_net_address;
      byte[] byte_address = getByteAddress();
      byte[] remote_byte_address = remote_ip_address.getByteAddress();
      byte[] mask;

      // controllo netmask
      if (net_mask==null && remote_ip_address.net_mask==null)
      {
	// usa le classi
	if ((byte_address[0]&128) == 0 )
	{
	  // classe A
	  return byte_address[0]==remote_byte_address[0];
	}
	else
	if ((byte_address[0]&192) == 128 )
	{
	  // classe B
	  return byte_address[0]==remote_byte_address[0] && byte_address[1]==remote_byte_address[1];
	}
	else
	if ((byte_address[0]&192) == 192 )
	{
	  // classe C
	  return byte_address[0]==remote_byte_address[0] && byte_address[1]==remote_byte_address[1] && byte_address[2]==remote_byte_address[2];
	}
	else
	{
	  log.info( remote_net_address.toString() + ": Classe sconosciuta" );
	  return false;
	}
      } // end classi
      else
      if (net_mask!=null && remote_ip_address.net_mask!=null)
      {
	for ( int i=0; i<4; ++i )
	{
	  // se le net_mask sono diverse gli indirizzi sono diversi?
	  if ( net_mask[i] != remote_ip_address.net_mask[i] )
	    return false;
	}
	mask = net_mask;
      }
      else
      if ( net_mask != null )
	mask = net_mask;
      else
	mask = remote_ip_address.net_mask;

      for ( int i=0; i<4; ++i )
      {
	if ( (byte_address[i]&mask[i]) != (remote_byte_address[i]&mask[i]) )
	  return false;
      }

      return true;
    } // end else

  }

  public String toString( )
  {
    return inet_address.toString() + " Port: " + port + " ";
  }

  public boolean equals( IPAddress x )
  {
    return this==x || (port==x.port && inet_address.getHostAddress().equals( x.inet_address.getHostAddress()) /* net_mask ininfluente&& java.util.Arrays.equals(net_mask,x.net_mask)*/ );
  }

  /**
   *  Serializzazione: default constructor
   */
  public IPAddress()
  {
  }

  /**
   * Serializzazione: writeObject()
   */
  public final void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    byte[] address = inet_address.getAddress();

    cos.writeByte( address[0] );
    cos.writeByte( address[1] );
    cos.writeByte( address[2] );
    cos.writeByte( address[3] );

    cos.writeInt( port );
//    cos.writeByteArray( net_mask );
  }


  /**
   * Serializzazione: readObject()
   */
  public final void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {

    // Indirizzo IP
    // compone la string 192.41.231.0 come una serie di byte + "." + byte + "."  + byte + "." + byte
    String ip = ""+ cis.readUByte() + "." + cis.readUByte() + "." + cis.readUByte() + "." + cis.readUByte();
    try { inet_address = InetAddress.getByName( ip ); }
    catch (java.net.UnknownHostException uhe) { throw new SerializationException( uhe.toString() ); }
    // Porta
    port = cis.readInt();
    // La netmask non viene serializzata in quanto dovrebbe essere sempre null
//    net_mask = cis.readByteArray();
  }

  public final int sizeOf()
  {
    return 4 * SizeOf.BYTE  + SizeOf.INT /* + 4 * SizeOf.BYTE*/;
  }

  public final static long SUID = clio.SUID.getSUID( IPAddress.class );
//  public final static long SUID = java.io.ObjectStreamClass.lookup( IPAddress.class ).getSerialVersionUID();
//  public final static long SUID = java.io.ObjectStreamClass.lookup( IPAddress.class ).getSerialVersionUID();

  // debug
/*
  public static void main( String[] args ) throws Exception
  {
    IPAddress ip_address = new IPAddress( InetAddress.getByName("192.41.42.255"), 2049, new byte[] { (byte) 255, (byte) 255, (byte) 255, 0 } );
    boolean res = ip_address.selftest();
    log.info( "Selftest " + ip_address.getClass().getName() + ":<"+ res +">");
  }


  private boolean selftest() throws Exception
  {
    boolean res = true;
    res &= selftest_get();
    res &= selftest_ser();
    return res;
  }

  private boolean selftest_get() throws Exception
  {
    log.info( "selftest_get: " );
    Debug.assert( java.util.Arrays.equals( getByteAddress(), new byte[] { (byte) 192, (byte) 41, (byte) 42, (byte) 255 } ) );
    Debug.assert( getPort() == 2049);
    Debug.assert( java.util.Arrays.equals( getBytePort(), new byte[] { 0x8, 0x1 } ) );
    NetAddress remote = new IPAddress( InetAddress.getByName("192.41.42.100"), 1029 );
    Debug.assert( isReachable(remote) );
    log.info( "ok" );
    return true;
  }

  private boolean selftest_ser() throws SerializationException
  {
    log.info( "selftest_set: " );
    Container c = new Container( 1024 );
    Container.ContainerOutputStream cos = c.getContainerOutputStream(true);

    cos.writeObject(this);
    cos.close();
    Container.ContainerInputStream cis = c.getContainerInputStream(true);

    IPAddress post_ip = (IPAddress) cis.readObject( IPAddress.class );
    Debug.assert( equals( post_ip ) );

    log.info( "ok" );
    return true;
  }
*/
}
