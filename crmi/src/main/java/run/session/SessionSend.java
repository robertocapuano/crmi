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
package run.session;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.transport.*;
import run.reference.RemoteServiceReference;

/**
 * INIT -> ACKI
 * ACKI -> DATA
 * DISC -> ACKD
 * ACKD -> NOP
 * RN -> DATA/DISC
 * DATA -> RN
 */

public final class SessionSend extends Session
{
       private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * Dimensione della finestra di invio.
   */
  protected int window_size;

  /**
   * Pool di un solo packet per gli RN
   */
//  protected PacketOne ctrl_packet;


  /**
   * Pool di packet per la finestra
   */
  protected PacketPool window_pool;

  /**
   * Ultimo request number ricevuto
   */
  protected int rn;

  /**
   * indici della window
   */
  protected int sn_min;
  protected int sn_max;


  private int debug_roundtripdelay = 0;
  private int debug_sleeptime = 0;
  private int debug_sleep_counter = 0;
  private int debug_send_counter, debug_sendtime;
  private int debug_sent_packet_counter =0;

  // metodi
  public SessionSend( ThreadGroup tgroup, SessionSendPool ssp )
  {
    super( tgroup, ssp, "SessionSend" );
    // in idle
    status = Opcodes.IDLE;
    // avvia
    start();
  }

  /**
   * Crea una nuova sessione per l'invio di un callgram
   */
  synchronized void initSession( int _session_id, int _ref_session_id, Transport _transport, NetAddress _remote_address, CallgramListener _listener )
  {
    int _window_size = run.Utility.getIntProperty( "run.session.sessionsend.windowsize", _transport.getWindowSize() );
    initSession( _session_id, _ref_session_id, _transport, _remote_address, _listener, _window_size );
  }

  /**
   * Crea una nuova sessione per l'invio di un callgram
   */
  synchronized void initSession( int _session_id, int _ref_session_id, Transport _transport, NetAddress _remote_address, CallgramListener _listener, int _window_size )
  {
    super.initSession( _session_id, _ref_session_id, _transport, _remote_address, _listener );

    if (_window_size!=0)
      window_size = _window_size;
    else
      window_size = getTransport().getWindowSize();

    // in idle
    status = Opcodes.IDLE;

    if ( window_pool==null || window_size!=window_pool.getPoolSize() || transport.getPacketSize()!=window_pool.getPacketSize() )
      window_pool = new PacketPool( transport.getPacketSize(), window_size );
    // pool di un solo packet
//    if (ctrl_packet==null || !ctrl_packet.isFree() )
//      ctrl_packet = new PacketOne( transport.getPacketSize() );
  }

  /**
   * Invio del callgram
   */
  public synchronized void send( Callgram _callgram )
  {
    callgram = _callgram;
    callgram.setSession( this );

    operation = _callgram.operation;
    return_value = _callgram.getReturnValue();

    setListener( _callgram.getListener() );

    callgram_buffer = callgram.getContainer().getByteFrame(); // getFrame().toArray();
    // la dimensione effettiva dei dati puo' essere < di quella prevista
    callgram_size = callgram.getContainer().getSize();
    log.info( ">>callgram_size:" + callgram_size );

//    int allocated_size = max_data_size * packet_total_number;

    int max_data_size = transport.getMaxDataSize();
    // approssimato all'intero superiore
    packet_total_number = (callgram_size + max_data_size-1) / max_data_size;

    log.info( "* packet_total_number: " + packet_total_number );
    log.info( "* packet max_data_size: " + max_data_size );
    log.info( "* window_size: " + window_size );
    // sequence number azzerato
    rn = 0;
    sn_min = 0;
    sn_max = set_sn_max();

    debug_roundtripdelay = (int) -System.currentTimeMillis();

    // invio del packet di inizializzazione in base al tipo di connessione
    switch (operation)
    {
      case Callgram.CALL:
        Packet init_packet = init( window_pool.get() );
        transport.send( init_packet );
	break;
      case Callgram.ERROR:
      case Callgram.REMOTE_EXCEPTION:
      case Callgram.RETURN:
	Packet ret_packet = ret( window_pool.get() );
	transport.send( ret_packet );
	break;
      default:
        log.info("callgram.operation errato");
	break;
    }

    // Avvio del thread per la gestione del protocollo
    notifyAll();
  }

  protected final int set_sn_max()
  {
    return sn_max = Math.min( packet_total_number, sn_min+window_size );
  }

  /**
   * Ricezione di un packet.
   */
  synchronized Packet receive( Packet in_packet )
  {
    switch ( in_packet.getOpcode() )
    {
      case Opcodes.INIT:
	log.info("SessionSend error: arrivato un packet INIT ad una SessionSend");
	return retrasmit(in_packet);
      case Opcodes.ACKI:
//	log.info("SessionSend: ricevuto ACKI");
	if (status==Opcodes.INIT)
	{
	  debug_roundtripdelay += System.currentTimeMillis();
	  log.info( "* init roundtrip delay ms: " + debug_roundtripdelay );
	  Packet acki_packet = acki( in_packet );
	  return send_window( acki_packet );
	}
	else
	  return retrasmit( in_packet );
      case Opcodes.DISC:
	log.info("SessionSend error: arrivato un packet DISC ad una SessionSend");
	return retrasmit( in_packet );
      case Opcodes.ACKD:
//	log.info("SessionSend: ricevuto ACKD");
	if (status==Opcodes.DISC)
	{
	  Packet ackd_packet = ackd( in_packet );
	  log.info( "* total sleeptime: " + debug_sleeptime + " #sleep: " + debug_sleep_counter + " average sleeptime ms: " + ( debug_sleep_counter!=0 ? debug_sleeptime/debug_sleep_counter : 0) );
	  log.info( "* total time between send: " + debug_sendtime + " #send: " + debug_send_counter + " average time between send ms: " + ( debug_send_counter!=0 ? debug_sendtime/debug_send_counter : 0) );
	  log.info( "* total packet sent: " + debug_sent_packet_counter + " average packet sent per send_window: " + ( debug_send_counter!=0 ? debug_sent_packet_counter/debug_send_counter : 0) );
	  log.info( "SessionSend notify con session_id:" + session_id + "ref_session_id: " + ref_session_id + " status: " + status );
	  // Chiama il pusher per una CALL, CallThread per un RETURN
	  notifyListener();
	  return ackd_packet;
	}
	else
	  return retrasmit( in_packet );
      case Opcodes.DATA:
	log.info("SessionSend error: arrivato un packet DATA");
	return retrasmit( in_packet );
      case Opcodes.RN:
//	log.info("SessionSend: ricevuto RN " + in_packet.getRN());
	if (status!=Opcodes.IDLE && status!=Opcodes.ACKD && status!=Opcodes.DISC)
	  return rn( in_packet );
	else
	  return retrasmit( in_packet );
      case Opcodes.RET:
	log.info( "SessionSend error: arrivato RET" );
	return retrasmit( in_packet );
      case Opcodes.ACKR:
//	log.info("SessionSend: ricevuto ACKR");
	if (status==Opcodes.RET)
	{
	  Packet ackr_packet = ackr( in_packet );
//	  log.info( ackr_packet.toString() );
//	  log.info( in_packet.toString() );
//	  Debug.
	  return send_window( ackr_packet );
//	  send_window(  );
//	  return nop_packet;
	}
	else
	  return retrasmit( in_packet );
      default:
	log.info("SessionSend error: arrivato pacchetto con opcode sconosciuto");
	return retrasmit( in_packet );
//	return nop_packet;
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
	return init( in_packet );
      case Opcodes.ACKI:
	return send_window( in_packet );
      case Opcodes.DISC:
//	log.info( "retrasmit disconnect");
	return disc( in_packet );
      case Opcodes.ACKD:
	return ackd( in_packet );
      case Opcodes.DATA:
	return data( in_packet, rn );
      case Opcodes.RET:
	return ret( in_packet );
      case Opcodes.ACKR:
	return send_window( in_packet );
      // casi che non si possono verificare
      case Opcodes.RN:
	return data( in_packet, rn );
      default:
	log.info("status error: " + status);
	return nop_packet;
    } // end switch
  }



  /**
   * Gestione della sessione
   */
  public void run()
  {

    int old_sn_min;
    long debug_before_sleep, debug_after_sleep;
    long debug_before_send, debug_after_send;

    debug_before_send = System.currentTimeMillis();

    while (true)
    {
      synchronized (this)
      {
	old_sn_min = sn_min;

	debug_before_sleep = System.currentTimeMillis();

	try
	{
	  if (status==Opcodes.IDLE || status==Opcodes.ACKD)
	    wait();
	  else
	    wait( TIME_OUT );
	}
	catch (InterruptedException ie ) { log.info( "SessionSend: " + ie ); }

	debug_after_sleep = System.currentTimeMillis();

	debug_sleeptime += (int) (debug_after_sleep - debug_before_sleep);
	debug_sleep_counter++;

	// dopo il time_out viene rispedito l'ultimo pacchetto di controllo
	switch (status)
	{
	  case Opcodes.INIT:
	    if ( window_pool.size()==window_size /*&& ctrl_packet.isFree() */)
	    {
//	      log.info( "SS run(): send INIT" );
//	      transport.send( init( ctrl_packet.get() ) );
	      transport.send( init( window_pool.get() ) );
	    }
	    else
	    {
	      // NOP
	    }
	    break;
	  case Opcodes.ACKI:
	    log.info( "SessionSend: status inconsistente" );
	    break;
	  case Opcodes.RET:
	    if ( window_pool.size()==window_size /*&& ctrl_packet.isFree()*/ )
	    {
//	      log.info( "SessionSend.run(): send RET" );
//	      transport.send( ret( ctrl_packet.get()) );
	      transport.send( ret( window_pool.get() ) );
	    }
	    else
	    {
	      // NOP
	    }
	    break;
	  case Opcodes.ACKR:
	    log.info( "SessionSend status inconsistente" );
	    break;
	  case Opcodes.DISC:
	    if ( window_pool.size()==window_size /*&& ctrl_packet.isFree()*/ )
	    {
//	      log.info( "SessionSend run(): send DISC" );
//	      transport.send( disc( ctrl_packet.get()) );
	      transport.send( disc( window_pool.get() ) );
	    }
	    else
	    {
	      // NOP
	    }
	    break;
	  case Opcodes.RN:
	  case Opcodes.DATA:
	    if (sn_min<sn_max && window_pool.size()==window_size )
	    {
	      debug_after_send = System.currentTimeMillis();

	      debug_sendtime += (int) (debug_after_send - debug_before_send);
	      debug_send_counter++;
	      debug_before_send = debug_after_send;
	      debug_sent_packet_counter += send_window( );

//	      transport.send( data( window_pool.get(), sn_min ) );
//	      if (old_sn_min!=sn_min)
//	      else
//	      {
//	        log.info( "SS run(): send DATA" );
//		int sn = rand( sn_min, sn_max );
//		transport.send( data( window_pool.get(), sn ) );
//	      }

//	      log.info( "SS run(): send DATA" );

//	      transport.send( data( ctrl_packet.get(), sn_min ) );
	    }
/*
	    // passo 5 dell'algoritmo
	    // se sn_min<sn_max e nell'ultimo periodo sn_min e' rimasto invariato e non abbiamo pacchetti in trasmissione
	    if (sn_min<sn_max && old_sn_min==sn_min && window_pool.size()==window_size )
	    {
//	      log.info( "SS run(): send DATA" );

	      transport.send( data( window_pool.get(), sn_min ) );
//	      transport.send( data( ctrl_packet.get(), sn_min ) );
	    }
	    else
	    if ( sn_min<sn_max && window_pool.size()==window_size )
	    {
//	      log.info( "SS run(): send DATA 2" );
	      int sn = rand( sn_min, sn_max );
//	      int sn = sn_min;
//	      if (sn<sn_max)
	      transport.send( data( window_pool.get(), sn ) );
//	      transport.send( data( ctrl_packet.get(), sn ) );
	    }
*/
	    break;
	  case Opcodes.IDLE:
	  case Opcodes.ACKD:
	    // idle
	    log.info( "SessionSend.run(): risvegliato ma status in idle" );
	    break;
	  default:
	    log.info( "SessionSend error: Opcode non gestito" );
	    break;
	} // end switch


      } // end synchronized

    } // end forever


  }

  final Packet send_window( Packet last_packet )
  {
    int last = Math.min( sn_max-1, sn_min + window_pool.size() );

    if (last>=0)
    {
      for ( int i=sn_min; i<last; ++i )
      {
	transport.send( data( window_pool.get(), i ) );
      }
      return data( last_packet, last );
    }
    else
    {
      // se last<0 non vi sono dati da inviare
      // quindi si invia una disc
      return disc( last_packet );
    }

  }

  final int send_window( )
  {
    int last = Math.min( sn_max, sn_min + window_pool.size() );

    if (last>=0)
    {
      for ( int i=sn_min; i<last; ++i )
      {
	transport.send( data( window_pool.get(), i ) );
      }
      return last-sn_min;
    }
    return 0;
  }

  /**
   * Invio di Init della connessione
   */
  protected Packet init( Packet init_packet )
  {
    status = Opcodes.INIT;
    RemoteServiceReference remote_ref = callgram.getRemoteServiceReference();
    String services_name = remote_ref.getServicesName();
    String service_name = remote_ref.getServiceName();
    String service_signature = remote_ref.getServiceSignature();
    init_packet.setInit( remote_address, session_id, packet_total_number, callgram_size, services_name, service_name, service_signature );
    return init_packet;
  }

  /**
   * Riconoscimento di init
   */
  protected Packet acki( Packet acki_packet )
  {
    if ( status == Opcodes.INIT /*|| status==Opcodes.ACKI */)
      status = Opcodes.ACKI;
    return acki_packet;
  }

  /**
   * Invio di Ret della connessione
   */

  protected Packet ret( Packet ret_packet )
  {
    status = Opcodes.RET;
    ret_packet.setRet( remote_address, session_id, ref_session_id, packet_total_number, callgram_size, return_value );
    return ret_packet;
  }

  /**
   * Riconoscimento di RET
   */
  protected Packet ackr( Packet ackr_packet )
  {
    if ( status == Opcodes.RET )
      status = Opcodes.ACKR;
    return ackr_packet;
  }

  /**
   * Invio di DISC
   */
  protected Packet disc( Packet disc_packet )
  {
    // soso stati spediti tutti i pacchetti
    if ( sn_max==packet_total_number )
    {
      status = Opcodes.DISC;
      disc_packet.setDisc( remote_address, session_id );
      return disc_packet;
    }
    else
      return nop_packet;
  }

  /**
   *  Sconnessione: ACKD
   */
  protected Packet ackd( Packet ackd_packet )
  {
    // siamo in fase di disconnnessione?
    if ( status==Opcodes.DISC || status==Opcodes.ACKD)
    {
      status = Opcodes.ACKD;
      return nop_packet;
    }
    else
      return nop_packet;
  }

  /**
   * Data
   */
  protected Packet data( Packet data_packet, int n )
  {
    if (n<packet_total_number && status!=Opcodes.DISC && status!=Opcodes.ACKD && n>=0)
    {
      status = Opcodes.DATA;
      int max_data_size = data_packet.getMaxDataSize();
      int data_size = n<packet_total_number-1 ? max_data_size : callgram_size - n*max_data_size;
      data_packet.setData( remote_address, session_id, packet_total_number, n, callgram_buffer, n*max_data_size, data_size );

      return data_packet;
    }
    else
      return nop_packet;
  }

  /**
   * RN n
   */

  protected Packet rn( Packet rn_packet )
  {
    int last_rn = rn_packet.getRN();

    if (last_rn<rn)
      return nop_packet;
    else
    {
      status = Opcodes.RN;

      rn = last_rn;

      if (rn<sn_min)
      {
	// rn scartato
	return nop_packet;
      }
      else
      {
	if (rn>=packet_total_number)
	  // **** Il peer ha ricevuto tutti i pacchetti
	  // **** Unico punto di disconnessione.
	  {
//	    log.info( "rn disconnect");
	    return disc( rn_packet );
	  }
	else
	{
	  sn_min = rn;
	  set_sn_max();

//	  return send_window( rn_packet );
	  return nop_packet;
	}
      }
    }
  } // end rn()


  public String toString()
  {
    String res = super.toString();
    res +=  "INIT roundtrip delay ms: " + debug_roundtripdelay + "\n";
    res += "total sleeptime: " + debug_sleeptime + " #sleep: " + debug_sleep_counter + " average sleeptime ms: " + ( debug_sleep_counter!=0 ? debug_sleeptime/debug_sleep_counter : 0)  + "\n";
    res += "total time between send: " + debug_sendtime + " #send_windod(): " + debug_send_counter + " average time between send ms: " + ( debug_send_counter!=0 ? debug_sendtime/debug_send_counter : 0) + "\n";
    res += "total packet sent: " + debug_sent_packet_counter + " average packet sent per send_window: " + ( debug_send_counter!=0 ? debug_sent_packet_counter/debug_send_counter : 0) + "\n";
//    res += "rn: " + rn + "\n";
//    res += "sn_min: " + sn_min + "\n";
//    res += "sn_max: " + sn_max + "\n";
    return res;
  }

}

