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

import java.net.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import run.*;
import run.serialization.Container;
import run.serialization.SizeOf;

//import run.serialization.*;

/**
 * Rappresenta un datagramma decodificato
 * Formato di un datagramma UDP di controllo
 *
 * sn: sequence number
 * rn: request number
 *
 * Inizio sessione
 * (byte)INIT  (int)session (int)totalnumber (int)callgram_size (utf32)services_name (utf32)service_name (utf32)service_signature
 *
 * ACKI
 * (byte)ACKI (int)session
 *
 * RET
 * (byte)RET (int)newsession (int)totalnumber (int)callgram_size (int)return_value (int)ref_session
 *
 * ACKR
 * (byte)ACKR (int)session
 *
 * Formato di un datagramma UDP di dati
 * (byte)DATA (int)session (int)totalnumber (int)sn (byte)data[]
 * 1            4             4                 4                []
 *
 * Rn
 * (byte)RN (int)session (int)totalnumber (int)rn
 *
 *
 * Fine sessione
 * (byte)DISC (int)session
 *
 * ACKD
 * (byte)ACKD (int)session
 *
 */

public class Packet
{
      private final static Logger log = LogManager.getLogger(IPAddress.class );
 
  // costanti
  protected final static int START_OF_OPCODE = 0;
  protected final static int START_OF_SESSION = SizeOf.BYTE;
  protected final static int START_OF_TOTAL_NUMBER = SizeOf.BYTE + SizeOf.INT;

  // per i packet dati
  protected final static int START_OF_PACKET_NUMBER = SizeOf.BYTE + 2 * SizeOf.INT;
  protected final static int START_OF_DATA = SizeOf.BYTE + 3* SizeOf.INT;

  // per i packet init/ret
  protected final static int START_OF_CALLGRAM_SIZE = SizeOf.BYTE + 2* SizeOf.INT;
  protected final static int START_OF_SERVICES_NAME = SizeOf.BYTE + 3* SizeOf.INT;

  // per i packet ret
  protected final static int START_OF_RETURN_VALUE = SizeOf.BYTE + 3* SizeOf.INT;
  protected final static int START_OF_REF_SESSION_ID = SizeOf.BYTE + 4* SizeOf.INT;

  // dimensione dei packet escluso la parte variabile (dati/stringhe)
  protected final static int SIZE_OF_INIT_HEADER = SizeOf.BYTE + 3 * SizeOf.INT;
  protected final static int SIZE_OF_RET_HEADER = SizeOf.BYTE + 5 * SizeOf.INT;
  protected final static int SIZE_OF_DATA_HEADER = SizeOf.BYTE + 3 * SizeOf.INT;
  protected final static int SIZE_OF_DISC = SizeOf.BYTE + SizeOf.INT; // effettivi
  protected final static int SIZE_OF_ACKI = SizeOf.BYTE + SizeOf.INT; // "
  protected final static int SIZE_OF_ACKD = SizeOf.BYTE + SizeOf.INT; // "
  protected final static int SIZE_OF_RN = SizeOf.BYTE + 3*SizeOf.INT; // "
//  protected final static int SIZE_OF_RET_HEADER = SizeOf.BYTE + 3 * SizeOf.INT;
//  protected final static int SIZE_OF_ACKR = SizeOf.BYTE + SizeOf.INT; // "

  // dati di istanza

  /**
   * Contiene l'immagine dei dati da copiare, la dimensione e' di PACKET_SIZE meno la dimensione dell'header
   */
  protected byte[] data;

  /**
   * dimensione del packet pari al datagramma udp
   */
  protected int packet_size;
  /**
   * dimensione massima dei dati
   */
  protected int max_data_size;

 // controllo
  protected NetAddress remote_address;

  // comuni
  byte opcode;
  int session_id;

  /**
   * call & ackn
   */
  int total_number;
  /**
   * call & ackn
   * number puo' indicare RN oppure SN
   */
  int number;
  /**
   * Per i pacchetti dati: dimensione effettiva dei dati
   */
  int data_size = 0;

  // init
  int callgram_size = 0;
  String services_name;
  String service_name;
  String service_signature;

  /**
   * ret
   */
  int return_value = 0;
  int ref_session_id = 0;

  /**
   * packet pool associato
   */
  PacketPool packet_pool;

  /**
   * Transport associato
   */
  Transport transport;

  public Packet( PacketPool _packet_pool, int _packet_size )
  {
    this( _packet_size );
    packet_pool = _packet_pool;
  }

  public Packet( int _packet_size )
  {
    packet_size = _packet_size;
    max_data_size = packet_size - SIZE_OF_DATA_HEADER;
    data = new byte[ max_data_size ];
  }

  public Packet( )
  {
    packet_size = 0;
    max_data_size = 0;
    data = null;
  }

  /**
   * Restituisce il packet al pool
   */
  public void free()
  {
    assert( packet_pool!=null );

    packet_pool.put( this );
  }

  private final Packet set(  byte _opcode, NetAddress _remote_address, int _session_id, int _total_number, int _callgram_size, String _services_name, String _service_name, String _service_sig, int _data_size, int _number, int _return_value, int _ref_session_id )
  {
    remote_address = _remote_address;

    opcode = _opcode;
    session_id = _session_id;
    total_number = _total_number;

    callgram_size = _callgram_size;
    services_name = _services_name;
    service_name = _service_name;
    service_signature = _service_sig;

    data_size = _data_size;
    number = _number;

    return_value = _return_value;
    ref_session_id = _ref_session_id;

    return this;
  }

  public final void setOpcode( byte _opcode )
  {
    opcode = _opcode;
  }

  // facilities
  public final Packet setInit( NetAddress _remote_address, int _session_id, int _total_number, int _callgram_size, String _services_name, String _service_name, String _service_sig  )
  {
    return set( Opcodes.INIT, _remote_address, _session_id, _total_number, _callgram_size, _services_name, _service_name, _service_sig ,0, 0, 0,0 );
  }

  public final Packet setRet( NetAddress _remote_address, int _new_session_id, int _old_session_id, int _total_number, int _callgram_size, int _return_value )
  {
    return set( Opcodes.RET, _remote_address, _new_session_id, _total_number,_callgram_size, null, null, null, 0,0, _return_value, _old_session_id );
  }

  public final Packet setData( NetAddress _remote_address, int _session_id, int _total_number, int _sn, byte[] src_data, int offset, int length )
  {
    assert length<=max_data_size : "Length: " + length + "max_data_size: " + max_data_size ;
    System.arraycopy( src_data, offset, data, 0, length );

    return set( Opcodes.DATA, _remote_address, _session_id, _total_number, 0, null, null, null, length, _sn, 0,0 );
  }

  public final Packet setDisc( NetAddress _remote_address, int _session_id )
  {
    return set(Opcodes.DISC, _remote_address, _session_id, 0, 0, null, null, null, 0, 0, 0,0 );
  }

  public final Packet setAckI( NetAddress _remote_address, int _session_id )
  {
    return set( Opcodes.ACKI, _remote_address, _session_id, 0, 0, null, null, null, 0, 0, 0,0 );
  }

  public final Packet setAckD( NetAddress _remote_address, int _session_id )
  {
    return set( Opcodes.ACKD, _remote_address, _session_id, 0, 0, null, null, null, 0, 0, 0, 0);
  }

  public final Packet setAckR( NetAddress _remote_address, int _session_id )
  {
    return set( Opcodes.ACKR, _remote_address, _session_id, 0, 0, null, null, null, 0, 0, 0, 0);
  }

  public final Packet setRN( NetAddress _remote_address, int _session_id, int _total_number, int _rn )
  {
    return set( Opcodes.RN, _remote_address, _session_id, _total_number, 0, null, null, null, 0, _rn, 0, 0 );
  }

  public final Packet setNop( NetAddress _remote_address)
  {
    return set( Opcodes.NOP, _remote_address, 0, 0, 0, null, null, null, 0, 0, 0,0 );
  }

  // setter
  final void setTransport( Transport _transport )
  {
    transport = _transport;
  }

  // getter
  public final PacketPool getPacketPool()
  {
    return packet_pool;
  }

  public final Transport getTransport( )
  {
    return transport;
  }

  public final byte getOpcode()
  {
    return opcode;
  }

  public final int getSessionId()
  {
    return session_id;
  }

  public final int getTotalNumber()
  {
    return total_number;
  }

  public final int getRN()
  {
    return number;
  }

  public final int getSN()
  {
    return number;
  }

  public final int getPacketNumber()
  {
    return number;
  }

  public final byte[] getData()
  {
    return data;
  }

  public final int getCallgramSize()
  {
    return callgram_size;
  }

  public final String getServicesName()
  {
    return services_name;
  }

  public final String getServiceName()
  {
    return service_name;
  }

  public final String getServiceSignature()
  {
    return service_signature;
  }

  public final NetAddress getRemoteAddress()
  {
    return remote_address;
  }

  public final int getMaxDataSize()
  {
    return max_data_size;
  }

  public final int getDataSize()
  {
    return data_size;
  }

  public final int getPacketSize()
  {
    return packet_size;
  }

  public final int getReturnValue()
  {
    return return_value;
  }

  public final int getRefSessionId()
  {
    return ref_session_id;
  }

  /**
   * equals
   */
  public boolean equals( Object o )
  {
    if (this==o)
      return true;

    if (!(o instanceof Packet))
      return false;

    Packet p = (Packet) o;

    return  p!=null && (
	    getCallgramSize()==p.getCallgramSize() &&
	    getOpcode()==p.getOpcode() &&
	    ( getOpcode()!=Opcodes.DATA || java.util.Arrays.equals( getData(), p.getData() ) ) &&
	    getDataSize()==p.getDataSize() &&
	    getMaxDataSize()==p.getMaxDataSize() &&
	    getPacketNumber()==p.getPacketNumber() &&
	    getPacketSize()==p.getPacketSize() &&
	    ( getRemoteAddress()==p.getRemoteAddress() || (getRemoteAddress()!=null && getRemoteAddress().equals(p.getRemoteAddress()) ) ) &&
	    getReturnValue()==p.getReturnValue() &&
	    getRN()==p.getSN() &&
	    getTotalNumber()==p.getTotalNumber() &&
//	    getTransport()==p.getTransport() &&
	    (getServicesName()==p.getServicesName() || (getServicesName()!=null && getServicesName().equals( p.getServicesName() )) )&&
	    (getServiceName()==p.getServiceName() || (getServiceName()!=null && getServiceName().equals( p.getServiceName()) ) )&&
	    (getServiceSignature()==p.getServiceSignature() || (getServiceSignature()!=null && getServiceSignature().equals( p.getServiceSignature()) ) ) &&
	    getSessionId() == p.getSessionId() &&
	    getRefSessionId() == p.getRefSessionId()
	    );
  }

  /**
   * toString
   */
  public String toString()
  {
    String res = "Packet:\n";
    res += "opcode: " + opcode + "\n";
    res += "packet_size: " + packet_size + "\n";
    res += "max_data_size: " + max_data_size + "\n";
    res += "total_number: " + total_number + "\n";
    res += "data_size: " + data_size + "\n";
    res += "number: " + number + "\n";
    res += "session_id: " + session_id + "\n";
    res += "return_value: " + return_value + "\n";
    res += "ref_session: " + ref_session_id + "\n";
    res += "callgram_size: " + callgram_size + "\n";
    res += "services_name: " + services_name + "\n";
    res += "service_name: " + service_name + "\n";
    res += "service_signature: " + service_signature + "\n";
    res += "data: " + data + "\n";
//    res += "packet_pool: " + packet_pool.toString()  + "\n";
    return res;
  }

  public Packet assign( Packet p )
  {
    if (packet_size==p.packet_size)
    {
      callgram_size = p.callgram_size;
      System.arraycopy(p.data, 0, data, 0, p.data.length);
      data_size = p.data_size;
      max_data_size = p.max_data_size;
      number = p.number;
      opcode = p.opcode;
      remote_address = p.remote_address;
      return_value = p.return_value;
      ref_session_id = p.ref_session_id;
      services_name = p.services_name;
      service_name = p.service_name;
      service_signature = p.service_signature;
      session_id = p.session_id;
      total_number = p.total_number;
      transport = p.transport;
    }
    return this;
  }
  /**
   * alpha test
   */
  public static void main( String args[] ) throws run.serialization.SerializationException
  {
    Packet p = new Packet( UDPTransport.LOOPBACK_PACKET_SIZE );
    boolean res = p.selftest();
    log.info( "Selftest " + p.getClass().getName() + ":<"+ res +">");
  }

  private boolean selftest() throws run.serialization.SerializationException
  {
    boolean res = true;
    res &= selftest_init();
    res &= selftest_data();
    res &= selftest_rn();
    res &= selftest_ret();
    return res;
  }

  private boolean selftest_init()
  {
    NetAddress localhost = TransportTable.getLocalHostAddress();

    setInit(localhost, 0, 1, 4*10, "SelfTest", "f", "(V)V" );
    log.info( toString() );
    return true;
  }

  private boolean selftest_data() throws run.serialization.SerializationException
  {
    NetAddress localhost = TransportTable.getLocalHostAddress();

    Container c = new Container( 1024 );
    Container.ContainerOutputStream cos = c.getContainerOutputStream(true);
    int[] ia = new int[10];
    cos.writeArray( ia );
    cos.close();
    byte data_stream[] = c.getByteFrame();
    setData( localhost, 1, 1, 0, data_stream, 0, SizeOf.array(ia) );
    log.info( toString() );
    return true;
  }

  private boolean selftest_rn()
  {
    NetAddress localhost = TransportTable.getLocalHostAddress();

    setRN(localhost, 0, 2, 1 );
    log.info( toString() );
    return true;
  }

  private boolean selftest_ret()
  {
    NetAddress localhost = TransportTable.getLocalHostAddress();

    setRet(localhost, 0, 1, 2, 40, -1 );
    log.info( toString() );
    return true;
  }

}