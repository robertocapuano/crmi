package run.session;

//import java.net.DatagramPacket;
import java.util.BitSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.exec.*;
import run.serialization.*;
import run.reference.*;
import run.transport.*;

/**
 *
 * Implementazione un sistema che permetta di evitare l'invio multiplo di RN.
 *
 * INIT -> ACKI
 * DISC -> ACKD
 * DATA -> RN
 * RET  -> ACKR
 *
 * Per la comunicazione usa un pool di un solo packet (definito pool per compatibilita' con il Transport)
 *
 * Implementare l'invio di RN al ricevimento di una finestra di pacchetti, oppure allo scadere del TIMEOUT
 */


public final class SessionReceive extends Session
{
         private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * Buffer di memoria consecutivo con i packet
   */
   protected byte[] callgram_buffer;

  /**
   * Servizio richiesto
   */
  protected String services_name;

  /**
   * Nome del metodo
   */
  protected String service_name;

  /**
   * Metodo chiamato
   */
  protected String service_signature;

  /**
   * Ultimo pacchetto richiesto
   */
  protected int rn;

  /**
   * Pool di un solo packet per gli RN
   */
  protected PacketOne ctrl_packet;

  /**
   * Mappa dei packets ricevuti
   */
  protected BitSet packets_map;

  private int debug_datapacket_dropped =0;
  private int debug_received=0;

  /**
   * Crea una nuova sessione alla ricezione della Init
   */
  public SessionReceive( ThreadGroup tgroup, SessionReceivePool sr_pool  )
  {
    super( tgroup, sr_pool, "SessionReceive" );
    // in idle
    status = Opcodes.IDLE;
    // avvia
    start();
  }

  synchronized void initSession( int _session_id, int _ref_session_id, Transport  _transport, NetAddress _remote_address, CallgramListener _listener )
  {
    super.initSession( _session_id, _ref_session_id, _transport, _remote_address, _listener );

    rn = 0;
    callgram = null;

    // in idle
    status = Opcodes.IDLE;

    // pool di un solo packet
    if (ctrl_packet==null || !ctrl_packet.isFree() )
      ctrl_packet = new PacketOne( transport.getPacketSize() );
//    rn_packet.setRN( remote_address, session_id, packet_total_number, 0 );
  }

  /**
   * Ricezione di un packet.
   */
  synchronized Packet receive( Packet in_packet )
  {
    debug_received++;
//    log.info( "session_id: " + in_packet.getSessionId() );
    switch ( in_packet.getOpcode() )
    {
      case Opcodes.INIT:
	log.info("SessionReceive: ricevuto INIT");
	if (status==Opcodes.IDLE)
	{
	  Packet acki_packet = init( in_packet );
	  notifyAll();
	  return acki_packet;
	}
	else
	  return retrasmit( in_packet );
      case Opcodes.ACKI:
	log.info("SessionReceive error: ricevuto opcode ACKI" );
	return retrasmit( in_packet );
      case Opcodes.RET:
//	log.info("SessionReceive: ricevuto RET");
	if (status==Opcodes.IDLE)
	{
	  Packet ackr_packet = ret( in_packet );
	  notifyAll();
	  return ackr_packet;
	}
	else
	  return retrasmit( in_packet );
      case Opcodes.ACKR:
	log.info("SessionReceive ERROR: ricevuto opcode ACKR" );
	return retrasmit( in_packet );
      case Opcodes.DISC:
	log.info("SessionReceive: ricevuto DISC");
	log.info( "status: " + status );
	log.info( "rn: " + rn );
	log.info( "packet_total_number: " + packet_total_number );
	if (status!=Opcodes.IDLE && status!=Opcodes.ACKD && rn==packet_total_number) // e' il primo DISC e abbiamo ricevuto tutti i pacchetti
	{
	  Packet ackd_packet = disc( in_packet );
	  // libera le risorse
	  packets_map = null;
	  buildCallgram();
	  // notifica al CallThread nel caso di prima chiamata o al Pusher nel caso di valore di ritorno.
	  log.info( "SessionReceive notify con session_id:" + session_id + "ref_session_id: " + ref_session_id + " status: " + status );
	  log.info( "* Data packet dropped: " + debug_datapacket_dropped );
	  log.info( "* Packet received: " + debug_received );
	  // chiama il Callthread in caso di una CALL
	  // chiama il pusher in caso di una RET
	  notifyListener();
	  return ackd_packet;
	}
	else
	  return retrasmit( in_packet );
      case Opcodes.ACKD:
	log.info("SessionReceive error: ha ricevuto opcode ACKD" );
	return retrasmit( in_packet );
      case Opcodes.DATA:
//	log.info("SessionReceive: ricevuto DATA " + in_packet.getSN());
	if (status!=Opcodes.IDLE && status!=Opcodes.ACKD)
	  return data( in_packet );
	else
	  return retrasmit( in_packet );
      case Opcodes.RN:
 	log.info("SessionReceive error: ricevuto opcode RN" );
	return retrasmit(in_packet);
      default:
	log.info("SessionReceive error: Opcode sconosciuto");
//	return nop_packet;
	return retrasmit(in_packet);
    } // end switch
  }

  /**
   * Ritrasmette l'ultimo pacchetto
   */
  Packet retrasmit( Packet in_packet )
  {
    switch (status)
    {
      case Opcodes.IDLE:
	return nop_packet;
      case Opcodes.INIT:
	return acki( in_packet );
      case Opcodes.ACKI:
//	log.info( "retrasmit rn" );
	return rn( in_packet );
      case Opcodes.ACKD:
//	return nop_packet;
	return ackd( in_packet );
      case Opcodes.RN:
//	log.info( "rn retrasmit rn" );
	return rn( in_packet );
      case Opcodes.RET:
	return ackr( in_packet );
      case Opcodes.ACKR:
//	log.info( "retrasmit rn" );
	return rn( in_packet );
      case Opcodes.DATA:
//	log.info( "data retrasmit rn" );
	return rn( in_packet );
      case Opcodes.DISC:
	return ackd( in_packet );
      default:
	log.info("status error: " + status);
	return nop_packet;
    } // end switch
  }


  protected void buildCallgram()
  {
    // i casi Callgram.CALL/Callgram.RETURN sono gestiti da init/ret
    if (return_value>0)
      operation = Callgram.REMOTE_EXCEPTION;
    else
    if (return_value<0)
    {
      operation = Callgram.ERROR;
      callgram_size = 0;
    }

    log.info("#callgram_size: " + callgram_size );
    byte[] fitted_buffer = new byte[callgram_size];
    System.arraycopy( callgram_buffer, 0, fitted_buffer, 0, callgram_size );

    Service called_service = new Service( service_name, service_signature );
    RemoteServiceReference called_service_reference = new RemoteServiceReference( null, services_name, called_service );

    callgram = new Callgram( operation, return_value, called_service_reference, this, getListener(), new Container( new Frame( fitted_buffer ) ) );
  }

  /**
   *      Gestione della sessione
   */
  public void run()
  {
//    PacketOne ctrl_packet = new PacketOne();

    while (true)
    {
      synchronized (this)
      {
	try
	{
	  if ( status==Opcodes.IDLE || status==Opcodes.ACKD )
	    wait( );
	  else
	    wait( TIME_OUT );
	}
	catch (InterruptedException ie ) { }

	// dopo il time_out viene rispedito l'ultimo pacchetto di controllo
	switch (status)
	{
	  case Opcodes.ACKI:
	    if ( ctrl_packet.isFree() )
	    {
//	      log.info( "SessionReceive.run(): ACKI");
	      transport.send( acki( ctrl_packet.get() ) );
	    }
	    else
	    {
	      // NOP
	    }
	    break;
	  case Opcodes.ACKR:
	    if ( ctrl_packet.isFree() )
	    {
//	      log.info( "SessionReceive.run(): ACKR");
	      transport.send( ackr( ctrl_packet.get() ) );
	    }
	    else
	    {
	      // NOP
	    }
	    break;
	  case Opcodes.RN:
	  case Opcodes.DATA:
	    if ( ctrl_packet.isFree() )
	    {
//	      log.info("SR run(): retrasmit rn");
	      transport.send( rn( ctrl_packet.get() ) );
	    }
	    else
	    {
	      // NOP
	    }
	    break;
	  case Opcodes.RET:
	  case Opcodes.INIT:
	  case Opcodes.DISC:
	    log.info( "SessionReceive error: risvegliato in status: " + status);
	  case Opcodes.ACKD:
	  case Opcodes.IDLE:
	    // idle
	    log.info( "SessionReceive.run(): risvegliato ma status in idle" );
	    break;
	  default:
	    log.info( "SessionReceive.run(): status errato: " + status );
	    break;
	} // end switch
      } // end synchronized
    } // end forever
  } // end run

  /**
   * aggiunge il packet alla sessione, gestisce una bit map dei pacchetti gia' copiati.
   */
  int add( Packet data_packet )
  {
    int j;

    int n = data_packet.getSN();

    if ( !packets_map.get(n) )
    {
      // se non e' stato ancora memorizzato...

      packets_map.set( n );
      int max_data_size = data_packet.getMaxDataSize();

      System.arraycopy( data_packet.getData(), 0, callgram_buffer, n*max_data_size, max_data_size );

      // Se ha bufferizzato gia' altri pacchetti li salta
      for ( j=rn; j<packet_total_number && packets_map.get(j); ++j );

      // puo' essere rn+1
      return j;
    }
    else
    {
      debug_datapacket_dropped++;
      return rn;
    }
  }


  /**
   * Ricezione di Init della connessione
   */
  protected Packet init( Packet init_packet )
  {
    status = Opcodes.INIT;

    packet_total_number = init_packet.getTotalNumber();

    callgram_size = init_packet.getCallgramSize();

    // mappa dei packets ricevuti
    packets_map = new BitSet( packet_total_number );

    int max_data_size = transport.getMaxDataSize();
//    PacketSize() - Packet.SIZE_OF_DATA_HEADER;
    int allocated_size = max_data_size * packet_total_number;
    callgram_buffer = new byte[ allocated_size ];

    operation = Callgram.CALL;
    return_value = 0;

    remote_address = init_packet.getRemoteAddress();
    services_name = init_packet.getServicesName();
    service_name = init_packet.getServiceName();
    service_signature = init_packet.getServiceSignature();

    return acki( init_packet );
  }

  /**
   * Invio di acki
   */
  protected Packet acki( Packet acki_packet )
  {
    if (status==Opcodes.INIT || status==Opcodes.ACKI)
    {
      status = Opcodes.ACKI;
      acki_packet.setAckI( remote_address, session_id );
  //    acki_packet.setOpcode( Opcodes.ACKI );

      return acki_packet;
    }
    else
      return nop_packet;
  }

  /**
   * Ricezione di Ret della connessione
   */
  protected Packet ret( Packet ret_packet )
  {
    status = Opcodes.RET;

    packet_total_number = ret_packet.getTotalNumber();

    callgram_size = ret_packet.getCallgramSize();

    // mappa dei packets ricevuti
    packets_map = new BitSet( packet_total_number );

    int max_data_size = transport.getMaxDataSize();
    int allocated_size = max_data_size * packet_total_number;
    callgram_buffer = new byte[ allocated_size ];

    operation = Callgram.RETURN;
    return_value = ret_packet.getReturnValue();
    ref_session_id = ret_packet.getRefSessionId();
    services_name = null;
    service_name = null;
    service_signature = null;

    return ackr( ret_packet );
  }

  /**
   * Invio di ackr
   */
  protected Packet ackr( Packet ackr_packet )
  {
    if ( status==Opcodes.RET || status==Opcodes.ACKR )
    {
      status = Opcodes.ACKR;
      ackr_packet.setAckR( remote_address, session_id );
  //    acki_packet.setOpcode( Opcodes.ACKI );

      return ackr_packet;
    }
    else
      return nop_packet;
  }


  /**
   * Invio di RN n
   */
  protected  Packet rn( Packet rn_packet )
  {
    if (status!=Opcodes.ACKD)
    {
      status = Opcodes.RN;

      rn_packet.setRN( remote_address, session_id, packet_total_number, rn );

      return rn_packet;
    }
    else
      return nop_packet;
  }

  /**
   * Ricevimento di un pacchetto dati
   */
  protected  Packet data( Packet data_packet )
  {
    return data( data_packet, data_packet.getSN() );
  }

  protected Packet data( Packet data_packet, int n )
  {
//    int n = data_packet.getNumber();

//    assert( n<packet_total_number && (status==Opcodes.DATA || status==Opcodes.RN || status==Opcodes.ACKI) );

    if (n>=rn)
    {
      status = Opcodes.DATA;

      // memorizza il packet anche se non ha number>rn
      rn = add( data_packet );
    }
    else
      debug_datapacket_dropped++;

//      return rn( call_packet );

    // questo codice serve ad evitare di fare il queueing di piu' RN
    // il prezzo da pagare e' la synchronize qui ed in UDPTransport.FIFOOutThread sul packet


    synchronized (ctrl_packet)
    {
      if ( ctrl_packet.isFree() )
      {
	// restituiamo al pool il data_packet
	data_packet.free();
//	log.info("data(): retrasmit rn");
	ctrl_packet.get();
	return rn( ctrl_packet );
      }
      else
      {
	if ( ctrl_packet.getOpcode()==Opcodes.RN && ctrl_packet.getRN()<rn)
	  rn( ctrl_packet );
	return nop_packet;
      } // end if..else
    } // end synchronized
  } // end data

  /**
   * Ricezione di DISC
   */
  protected  Packet disc( Packet disc_packet )
  {
    // sosno stati ricevuti tutti gli ackn?
    assert( rn==packet_total_number );

    status = Opcodes.DISC;

    return ackd( disc_packet );
  }

  /**
   *  Invio di ACKD: sconnesso
   */
  protected  Packet ackd( Packet ackd_packet )
  {
    assert( status==Opcodes.DISC || status==Opcodes.ACKD);

    status = Opcodes.ACKD;

    ackd_packet.setAckD( remote_address, session_id );
//    ackd_packet.setOpcode( Opcodes.ACKD );

    return ackd_packet;
  }


  public String toString()
  {
    String res = super.toString();
    res += "rn: " + rn + "\n";
    res += "services_name: " + services_name + "\n";
    res += "service_name: " + service_name + "\n";
    res += "service_signature: " + service_signature + "\n";
    res += "Data packet dropped: " + debug_datapacket_dropped;
    res += "Packet received: " + debug_received;
    res += "\n";

    return res;
  }



}
/*
  void initSession( Packet init_packet, CallgramListener _listener )
  {
    super.initSession( init_packet.getSessionId(), init_packet.getTransport(), init_packet.getRemoteAddress(), _listener );

    rn = 0;
    packet_total_number = init_packet.getTotalNumber();

    callgram = null;
    callgram_size = init_packet.getCallgramSize();

    // mappa dei packets ricevuti
    packets_map = new BitArray( packet_total_number );

    int max_data_size = getPacketSize() - Packet.SIZE_OF_CALL_HEADER;
    int allocated_size = max_data_size * packet_total_number;
    callgram_buffer = new byte[ allocated_size ];

    services_name = init_packet.getServicesName();
    method_signature = init_packet.getMethodSignature();

    // pool di un solo packet
    if (rn_packet==null || !rn_packet.isFree() )
      rn_packet = new PacketOne();
//    rn_packet.setRN( remote_address, session_id, packet_total_number, 0 );
  }
*/
