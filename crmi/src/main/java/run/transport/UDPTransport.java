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
package run.transport;

import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.Utility;
import run.serialization.*;
import run.session.*;

/**
 * Gestisce la FIFO dell'invio/ricezione dei datagrammi UDP
 *
 * Arriva un datagramma UDP, se e' associata ad una sessione esistente
 * 1) lo passa alla sessione esistente
 *
 * Altrimenti
 * 1) crea la sessione,
 * 2) associa il numero di sessione con la sessione, in modo che altri pacchetti UDP vengono memorizzati nella stessa sessione.
 * 3) Passa il datagram alla sessione
 *
 * NOTA: Vi sono due istanze di UDP: per ethernet e per myrinet
 */

public final class UDPTransport extends Transport
{
  private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * FastEthernet
   */
  public final static int ETHERNET_PACKET_SIZE = 1500-28;
  public final static int ETHERNET_WINDOW_SIZE = DEFAULT_WINDOW_SIZE; // 16
  public final static int ETHERNET_PORT = 4096;

  /**
   * Loopback
   */
  public final static int LOOPBACK_PACKET_SIZE = ETHERNET_PACKET_SIZE;
  public final static int LOOPBACK_WINDOW_SIZE = DEFAULT_WINDOW_SIZE; // 16
  public final static int LOOPBACK_PORT = 2048;

  /**
   * Myrinet
   */
  public final static int MYRINET_PACKET_SIZE = 3752-28;
  public final static int MYRINET_WINDOW_SIZE = DEFAULT_WINDOW_SIZE; // 16
  public final static int MYRINET_PORT = 8192;

  /**
   * TIMEOUT per questo trasporto
   */
  private static int TIME_OUT = 10;

  /**
   * dimensione del buffer di input e' configurato su Linux.
   */
  protected static int SOCKET_BUFFER_SIZE = 128*1024;

  /**
   * Dimensione del buffer di output
   */
  protected static int DATAGRAM_BUFFER_SIZE = 128*1024;
  /**
   * Coda FIFO in uscita
   */
  protected LinkedList FIFO_out;
  /**
   * Coda FIFO in ingresso
   */
//  protected LinkedList FIFO_in;

  /**
   * Socket UDP
   */
  protected DatagramSocket socket;

  /**
   * Thread per la gestione dell'IO
   */
  protected ThreadGroup fifo_threadg;
  protected Thread fifo_in_thread;
  protected Thread fifo_out_thread;

  private int debug_total_packet_sent = 0;
  private int debug_total_packet_received = 0;

  /**
   * Dimensione dei pacchetti
   */
  public UDPTransport( IPAddress _local_address, int _packet_size, int _window_size, int buffer_size ) throws TransportException
  {
    super( NetAddress.IP_ADDRESS, _local_address,  _packet_size, _window_size );

    log.info( "************** buffer_size:" + buffer_size );
    FIFO_out = new LinkedList();
//    FIFO_in = new LinkedList();
    IPAddress ip_address = (IPAddress) local_address;

    try
    {
      socket = new DatagramSocket( ip_address.getPort(), ip_address.toInetAddress() );
      socket.setSendBufferSize(buffer_size);
      socket.setReceiveBufferSize(buffer_size);
    }
    catch (SocketException se ) { throw new TransportException( "Errore sull'apertura del socket: " + se.toString() ); }

    // chiamata di metodi in caso di ini_port ==0
    // non so se sia necessiario!
/*
    InetAddress local_inet_address = socket.getInetAddress();
    int local_port = socket.getPort();
    local_address = new IPAddress( local_inet_address, local_port );
*/

    if ( ip_address.getPort()==0)
      ip_address.setPort( socket.getLocalPort() );

    ThreadGroup fifo_threadg = new ThreadGroup( "UDP Transport, FIFO Group" );
    fifo_in_thread = new FIFOInThread( fifo_threadg );
    fifo_out_thread = new FIFOOutThread( fifo_threadg );
    start();
  }


  /**
   * Accoda il Packet per la trasmissione
   */
  public void send( Packet p )
  {
    synchronized ( FIFO_out )
    {
      FIFO_out.add( p );
      FIFO_out.notifyAll();
    }
  }

  public void setFromDatagram( DatagramPacket in_datagram, Packet in_packet, Container.ContainerInputStream cis ) throws SerializationException
  {
    // true in quanto contiene stringhe che sono "oggetti"
    cis.reset( false );

    IPAddress remote_address = new IPAddress( in_datagram.getAddress(), in_datagram.getPort() );

    // campi comuni
    byte opcode = cis.readByte();
    int session_id = cis.readInt();

    // campi non comuni
    int total_number, callgram_size;

    switch ( opcode )
    {
      case Opcodes.INIT:
	total_number = cis.readInt();
	callgram_size = cis.readInt();
	String services_name = cis.readStringUTF8();
	String service_name = cis.readStringUTF8();
	String service_signature = cis.readStringUTF8();

	in_packet.setInit( remote_address, session_id, total_number, callgram_size, services_name, service_name, service_signature );
	break;
      case Opcodes.RET:
        total_number = cis.readInt();
	callgram_size = cis.readInt();
	int return_value = cis.readInt();
	int ref_session_id = cis.readInt();
	in_packet.setRet(remote_address, session_id, ref_session_id, total_number, callgram_size, return_value );
	break;
      case Opcodes.DATA:
        total_number = cis.readInt();
	int sn = cis.readInt();
	int data_size = in_datagram.getLength() - Packet.SIZE_OF_DATA_HEADER;

	in_packet.setData( remote_address, session_id, total_number, sn, in_datagram.getData(), Packet.START_OF_DATA, data_size );
	break;
      case Opcodes.RN:
        total_number = cis.readInt();
	int rn = cis.readInt();

	in_packet.setRN( remote_address, session_id, total_number, rn );
	break;
      // non hanno campi specifici
      case Opcodes.DISC:
	in_packet.setDisc( remote_address, session_id );
	break;
      case Opcodes.ACKI:
	in_packet.setAckI( remote_address, session_id );
	break;
      case Opcodes.ACKD:
	in_packet.setAckD( remote_address, session_id );
	break;
      case Opcodes.ACKR:
	in_packet.setAckR( remote_address, session_id );
	break;
      default:
	log.info("Packet con opcode sconosciuto");
    }
  }

  public void setToDatagram( DatagramPacket out_datagram, Packet out_packet, Container.ContainerOutputStream cos ) throws SerializationException
  {
    // dimensione del datagram
    int length = 0;
    // true in quanto contiene stringhe che sono "oggetti"
    cos.reset( false );

    IPAddress remote_address = (IPAddress) out_packet.getRemoteAddress();
    out_datagram.setAddress( remote_address.toInetAddress() );
    out_datagram.setPort( remote_address.getPort() );

    // campi comuni
    cos.writeByte( out_packet.getOpcode() );
    cos.writeInt( out_packet.getSessionId() );

    switch ( out_packet.getOpcode() )
    {
      case Opcodes.INIT:
        cos.writeInt( out_packet.getTotalNumber() );
	cos.writeInt( out_packet.getCallgramSize() );
	cos.writeStringUTF8( out_packet.getServicesName() );
	cos.writeStringUTF8( out_packet.getServiceName() );
	cos.writeStringUTF8( out_packet.getServiceSignature() );
	length = cos.close();
	break;
      case Opcodes.RET:
        cos.writeInt( out_packet.getTotalNumber() );
	cos.writeInt( out_packet.getCallgramSize() );
	cos.writeInt( out_packet.getReturnValue() );
	cos.writeInt( out_packet.getRefSessionId() );
	length = cos.close();
	break;
      case Opcodes.DATA:
        cos.writeInt( out_packet.getTotalNumber() );
	cos.writeInt( out_packet.getSN() );
	System.arraycopy( out_packet.getData(), 0, out_datagram.getData(), Packet.START_OF_DATA, out_packet.getDataSize() );
	length = cos.close() + out_packet.getDataSize();
	break;
      case Opcodes.RN:
        cos.writeInt( out_packet.getTotalNumber() );
	cos.writeInt( out_packet.getRN() );
	length = cos.close();
	break;
      case Opcodes.ACKI:
      case Opcodes.ACKR:
      case Opcodes.DISC:
      case Opcodes.ACKD:
	// non hanno campi specifici
	length = cos.close();
	break;
      default:
	log.info("Packet con opcode sconosciuto");
    }
    out_datagram.setLength( length );

  }



  /**
   *  Delega la ricezione/invio dei packet ai due threads
   */

  public void run()
  {
    fifo_in_thread.start();
    fifo_out_thread.start();
  } // end run()


// Gestione del socket

  /**
   * Fifo Output thread
   */

  private class FIFOInThread extends Thread
  {
    FIFOInThread( ThreadGroup g )
    {
      super( g, "FIFOInThread" );
    }

    public void run()
    {
      Packet in_packet, out_packet;
      byte[] in_buffer = new byte[DATAGRAM_BUFFER_SIZE];
      DatagramPacket in_datagram = new DatagramPacket( in_buffer, DATAGRAM_BUFFER_SIZE );

      Frame frame = new Frame( in_buffer );
      Container container = new Container( frame );
      Container.ContainerInputStream cis = container.getContainerInputStream( true );

      while (true)
      {
	in_datagram.setLength(DATAGRAM_BUFFER_SIZE);

	try
	{
	  socket.receive( in_datagram );
	  ++debug_total_packet_received;
	}
	catch ( IOException ioe ) { log.info( ioe.toString() ); }

	in_packet = packet_pool.get();

	/**
	 * INIT -> ACKI
	 * ACKI -> CALL
	 * DISC -> ACKD
	 * ACKD -> NOP
	 * RN -> DATA
	 * DATA -> RN
	 * RET -> ACKR
	 */

	try
	{
	  setFromDatagram( in_datagram, in_packet, cis );
	  in_packet.setTransport( UDPTransport.this );

	  // 1. in_datagram ed out_datagram puntano allo stesso oggetto ma e' modificato
	  // 2. dispatch smista il packet
	  // 3. in genere in_packet ed out_packet sono uguali se non nel caso in cui out_packet==nop_packet
	  out_packet = SessionTable.dispatch( in_packet );
//	  out_packet = selftest_receive( in_packet );

	  if ( out_packet.getOpcode() != Opcodes.NOP )
	  {
//          Debug
//	    if (!((Math.random()*1000)%2==0))
	    send( out_packet );
	  }
	  else
	  {
	    // in caso di NOP il pacchetto e' scartato.
	    in_packet.free();
	  }

	  	    assert packet_pool.size() <= packet_pool.getPoolSize() : "Pool: " + out_packet.getPacketPool() +  " out_pool:" + packet_pool + "  packet_pool.size(): " + packet_pool.size()  + " packet_pool.getPoolSize(): " + packet_pool.getPoolSize() ;
//	    assert( packet_pool.size() <= packet_pool.getPoolSize(), "packet_pool.size(): " + packet_pool.size()  + " packet_pool.getPoolSize(): " + packet_pool.getPoolSize() );

	}
	catch (SerializationException se )
	{
	  log.info( se.toString() );
	}


      } // end forever
    } // end run


  } // end FIFOInThread


  /**
   * Fifo Output thread
   */

  private class FIFOOutThread extends Thread
  {
    FIFOOutThread( ThreadGroup g )
    {
      super( g, "FIFOOutThread" );
    }

    public void run()
    {
      Packet out_packet;

      byte[] out_buffer = new byte[DATAGRAM_BUFFER_SIZE];
      DatagramPacket out_datagram = new DatagramPacket( out_buffer, DATAGRAM_BUFFER_SIZE );

      Frame frame = new Frame( out_buffer );
      Container container = new Container( frame );
      Container.ContainerOutputStream cos = container.getContainerOutputStream( false );

      while (true)
      {
	synchronized ( FIFO_out )
	{
	  while ( FIFO_out.size()==0 )
	  {
	    try { FIFO_out.wait( TIME_OUT ); }
	    catch ( InterruptedException ie ) { log.info( ie.toString() ); }
	  }

	  out_packet = (Packet) FIFO_out.removeFirst();
//	    assert( out_packet!=null, "out_packet==null" );
	} // end synchronized ( FIFO_out)

	try
	{
	  // la synchronized qui serve per evitare di sovrascrivere packet di tipo RN mentre questi e' in trasmissione
	  // vedi SessionReceive
	  synchronized (out_packet)
	  {
	    setToDatagram( out_datagram, out_packet, cos );
	    // si restituisce il packet al pool a cui appartiene, non e' detto che sia this.packet_pool
	    out_packet.free();

	    assert packet_pool.size() <= packet_pool.getPoolSize() : "Pool: " + out_packet.getPacketPool() +  " out_pool:" + packet_pool + "  packet_pool.size(): " + packet_pool.size()  + " packet_pool.getPoolSize(): " + packet_pool.getPoolSize() ;
	    // si invia il datagram
//	    if (out_packet.getOpcode()!=Opcodes.ACKD)
	    socket.send( out_datagram );
	    ++debug_total_packet_sent;
	  } // end synchronized (out_packet)
	}
	// setToDatagram()
	catch ( SerializationException se ) { log.info( se.toString() ); }
	// socket.send()
	catch ( IOException ioe ) { log.info( ioe.toString() ); }

      } // end forever

    } // end run

  }

// debug
  public String toString()
  {
    String res = "Type: UDPTransport\n";
    res += super.toString();
    res += "Total packet sent: " + debug_total_packet_sent;
    res += " total packet received: " + debug_total_packet_received + "\n";
//    try { res += "Socket send buffer size: " + socket.getSendBufferSize() + " Socket receive buffer size: " + socket.getReceiveBufferSize() + "\n"; }
//    catch ( SocketException se ) { res += "Socket information unavailable\n"; }

//    Packet[] pl;
//    Packet[] pl = (Packet[]) FIFO_in.toArray( new Packet[0] );
//    res += "FIFO_in: ";

//    for (int i=0; i<pl.length; ++i )
//      res += pl[i].toString();
//    res += "\n";

//    pl = (Packet[]) FIFO_out.toArray( new Packet[0] );
//    res += "FIFO_out: ";
//    for (int i=0; i<pl.length; ++i )
//      res += pl[i].toString();
//    res += "\n";

//    res += "fifo_in_thread: " + fifo_in_thread.toString() + "\n";
//    res += "fifo_out_thread: " + fifo_out_thread.toString() + "\n";
    return res;
  }

/*

  public static void main( String[] args ) throws Exception
  {
    UDPTransport t = (UDPTransport) TransportTable.getTransport(0);

    log.info( t.toString() );

    boolean res = t.selftest();
    log.info( "Selftest " + t.getClass().getName() + ":<"+ res +">");
  }


  private boolean selftest() throws Exception
  {
    boolean res = true;
    res &= selftest_packet();
//    res &= selftest_send();
//    res &= selftest_receive();
    return res;
  }

  private boolean selftest_receive() throws Exception
  {
    while (counter!=last)
    {
      try {  Thread.sleep(10); } catch (Exception e ) {}
    }

    return pl1.equals( pl2 );
  }

  static ArrayList pl1 = new ArrayList();
  static ArrayList pl2 = new ArrayList();
  static int counter = 0;
  static int last =0;

  private static Packet selftest_receive( Packet p)
  {
    final Packet nop_packet = new Packet().setNop( null );
    final int packet_size = TransportTable.getTransport(0).getPacketSize();
    Packet p2 = new Packet( packet_size );
    p2.assign( p );
    pl2.add(p2);
    ++counter;
    return nop_packet;
  }

  private boolean selftest_send() throws Exception
  {
    log.info("selftest_send");
    PacketPool packet_pool = new PacketPool( getPacketSize(), 16 );
    Packet packet;

    packet = packet_pool.get();
    packet.setInit( getLocalAddress(), 0, 1, 1024, "selftest", "selftest_packet", "(V)V" );
    pl1.add(packet);
    ++last;
    send( packet );

    packet = packet_pool.get();
    byte[] out_data = new byte[1459];
    for ( int i=0; i<out_data.length; ++i)
      out_data[i] = (byte) (i%127);
    packet.setData(getLocalAddress(), 0, 2, 1, out_data, 0, out_data.length );
    ++last;
    pl1.add(packet);
    send( packet );

    packet = packet_pool.get();
    packet.setRN( getLocalAddress(), 0, 1, 2 );
    ++last;
    pl1.add(packet);
    send( packet );

    packet = packet_pool.get();
    packet.setRet( getLocalAddress(), 0, 1, 2, 1024, 23 );
    ++last;
    pl1.add(packet);
    send( packet );

    packet = packet_pool.get();
    packet.setAckD( getLocalAddress(), 1 );
    ++last;
    pl1.add(packet);
    send( packet );
    return true;
  }

  private boolean selftest_packet() throws Exception
  {
    log.info( "selftest_packet" );

    byte[] buffer = new byte[INPUT_BUFFER_SIZE];
    DatagramPacket dp = new DatagramPacket( buffer, buffer.length );

    Frame frame = new Frame( buffer );
    Container container = new Container( frame );
    Container.ContainerOutputStream cos = container.getContainerOutputStream( true );
    Container.ContainerInputStream cis = container.getContainerInputStream( true );

    Packet in_packet = new Packet( getPacketSize() );
    Packet out_packet = new Packet( getPacketSize() );

    out_packet.setInit(getLocalAddress(), 0, 1, 1024, "selftest", "selftest_packet", "(V)V" );
    setToDatagram(dp, out_packet, cos );
    setFromDatagram(dp,in_packet, cis );

    assert( in_packet.equals(out_packet) );
    log.info( "init_packet set ok" );

    byte[] out_data = new byte[1024];
    for ( int i=0; i<out_data.length; ++i)
      out_data[i] = (byte) (i%127);
    out_packet.setData(getLocalAddress(), 0, 2, 1, out_data, 0, out_data.length );

    setToDatagram(dp, out_packet, cos );
    setFromDatagram(dp,in_packet, cis );
    assert( out_packet.equals(in_packet) );
    log.info( "data_packet set ok" );

    out_packet.setRN( getLocalAddress(), 0, 1, 2 );
    setToDatagram(dp, out_packet, cos );
    setFromDatagram(dp,in_packet, cis );
    assert( in_packet.equals(out_packet) );
    log.info( "rn_packet set ok" );

    out_packet.setRet( getLocalAddress(), 0, 1, 2, 1024, 23 );
    setToDatagram(dp, out_packet, cos );
    setFromDatagram(dp,in_packet, cis );
    assert( in_packet.equals(out_packet) );
    log.info( "ret_packet set ok" );

    out_packet.setAckD( getLocalAddress(), 1 );
    setToDatagram(dp, out_packet, cos );
    setFromDatagram(dp,in_packet, cis );
    assert( in_packet.equals(out_packet) );
    log.info( "ackd_packet set ok" );

//    send(p);
    return true;
  }
*/
}


