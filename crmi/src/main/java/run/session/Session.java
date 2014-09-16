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
package run.session;

import java.net.DatagramPacket;
import java.util.*;
import java.net.*;


import run.transport.*;
import run.exec.ThreadPool;

/**
 * Collega il Transport (indirizzo local)
 * con un nodo remoto (RemoteServicesReference)
 * Una sessione e' legata al trasporto (indirizzo locale)
 * e mediante questo alla session_table;
 *
 * Una sessione permette di identificare una serie di datagrammi UDP con un
 * Callgramm, e da questo al CallgramListener.
 * La sessione identifica una chiamata, quindi serve per comunicazione full-duplex
 * La sessione e' orientata alla chiamata, quindi mette in evidenza due momenti
 *
 * Per il chiamante.
 * L'invio del primo Callgram con il Marshalling degli argomenti della chiamata
 * La ricezione della risposta o dell'Exception.
 *
 * Per il chiamato.
 * La ricezione della chiamata,
 * l'invio della risposta.
 *
 *
 *
 *  e la invia sulla rete. In base all'OPCODE del callgram decide quale indirizzo usare:
 *  server o client, e in base al tipo di indirizzo quale rete usare.
 *  Legge un callgramma in ingresso se e' presente
 *  Gestisce lo sliding window in entrambi i versi.
 *  La sessione gestisce la connessione, l'invio dei pacchetti, la ritrasmissione e la disconnessione.
 *  Tutto questo con l'ausilio del UDPTransport
 *
 *
 * INIT -> ACKI
 * ACKI -> CALL
 * DISC -> ACKD
 * ACKD -> NOP  nessuna risposta la connesione e' chiusa
 * ACKN -> CALL invio di un pacchetto
 * CALL -> ACKN riconoscimento del pacchetto
 *
 */

abstract class Session extends Thread
{
  /**
   * TIME_OUT della connessione per un rinvio dell'ultimo pacchetto
   */
  protected final static int TIME_OUT = 10;

  /**
   * Pacchetto usato per segnalare nessuna risposta.
   */
  protected final static Packet nop_packet = new Packet().setNop( null );


  /**
   * Trasporto associato alla sessione, contiene l'indirizzo locale
   */
  protected Transport transport;

  /**
   * Numero della sessione generato dal chiamante.
   * Il modo per generare un numero di sessione unico: random
   */
  protected int session_id;

  /**
   * Riferimento ad una sessione precedente: usato per la restituzione di un risultato
   */
  protected int ref_session_id;

  /**
   * Numero totali pacchetti da ricevere/inviare
   */
  protected int packet_total_number;

  /**
   * Callgram ricostruito/da inviare
   */
  protected Callgram callgram;

  /**
   * Dimensione del callgram da inviare/ricevere
   */
  protected int callgram_size;

  /**
   * Buffer immagine del callgram
   */
  protected byte[] callgram_buffer;

  /**
   * Lista dei datagram da inviare/ricevuti
   * E' un arraylist per l'accesso via ack n.
   */
  protected ArrayList packet_list;

  /**
   * Indirizzo remoto
   */
  protected NetAddress remote_address;

  /**
   * Callback usato solo nel caso di ricevuto del callgram
   */
  protected CallgramListener listener;

  /**
   * Valore di ritorno della chiamata
   */
  protected int return_value;

  /**
   * Operazione trasmessa con il callgramm: CALL/RETURN
   */
  protected byte operation;

  /**
   * status della connessione
   */
  volatile byte status;

  /**
   * Pool della sessione
   */
  ThreadPool session_pool;

  protected Session( ThreadGroup tgroup, ThreadPool _session_pool, String session_type )
  {
    super( tgroup, session_type );
    session_pool = _session_pool;
  }

  void initSession(  int _session_id, int _ref_session_id, Transport _transport, NetAddress _remote_address, CallgramListener _callback )
  {
    transport = _transport;
    session_id = _session_id;
    ref_session_id = _ref_session_id;
    remote_address = _remote_address;
    listener = _callback;
  }

  protected synchronized int rand( int min, int max )
  {
    float r = SessionTable.random.nextFloat();
    float range = (float) (max-min);

    int res = (int) (range * r);

    res += min;

//    Debug.assert( res>=min && res<=max, "////////////////res out of limits///////////////");
//    Debug.println( "sn_min: " + min + "sn: " + res + "sn_max: " +max );
    return res;
  }

  /*
  protected final int getPacketSize()
  {
    return transport.getPacketSize();
  }
*/

  /**
   * Gestione della sessione
   */
  public abstract void run();

  /**
   * Get dell'indirizzo remoto
   */
  protected final NetAddress getRemoteAddress( )
  {
    return remote_address;
  }

  /**
   * Set dell'indirizzo remoto
   */
  protected final void setRemoteAddress( NetAddress _remote_address )
  {
    remote_address = _remote_address;
  }

  /**
   * Segnala al listener (dispatcher nel caso di un nuovo callgram) che e' arrivato/spedito un callgram
   */

  protected final void notifyListener()
  {
//    com.borland.primetime.util.Debug.println( callgram.toString() );
    listener.execCall( callgram );
  }

  /**
   * Restituisce il listener
   */
  public final CallgramListener getListener( )
  {
    return listener;
  }

  /**
   * Restituisce l'id della sessione
   */
  public final int getSessionId()
  {
    return session_id;
  }

  /**
   * Restituisce l'id della sessione a cui fa riferimento
   */
  public final int getRefSessionId()
  {
    return ref_session_id;
  }

  /**
   * Restituisce il transport associato alla sessione
   */
  public final Transport getTransport()
  {
    return transport;
  }

  /**
   * set del listener
   */
  protected final void setListener( CallgramListener _listener )
  {
    listener = _listener;
  }


  /**
   * Gestisce la ricezione di un packet (di dati o controllo) e restituisce la risposta.
   *
   */
  abstract Packet receive( Packet p );

  /**
   * Ritrasmissione dell'ultimo pacchetto
   */
  abstract Packet retrasmit( Packet in_packet );

  // Gestione della connessione

  /**
   * Init della connessione
   */
  protected abstract Packet init( Packet init_packet );

  /**
   * Riconoscimento di init
   */
  protected abstract Packet acki( Packet acki_packet );

  /**
   * Invio di DISC
   */
  protected abstract Packet disc( Packet disc_packet );

  /**
   *  Sconnessione: ACKD
   */
  protected abstract Packet ackd( Packet ackd_packet );


  /**
   * Call
   */
  protected abstract Packet data( Packet call_packet, int sn );

  /**
   * RN n
   */
  protected abstract Packet rn( Packet rn_packet );

  /**
   * RET
   */

  protected abstract Packet ret( Packet ret_packet );

  /*
   * ACKR
   */
  protected abstract Packet ackr( Packet ackr_packet );

  public String toString()
  {
    String res = "<" + getClass().getName() + ">\n";
    res += "session_id: " + session_id + "\n";
    res += "ref_session_id: " + ref_session_id + "\n";
    res += "status: " + status + "\n";
    res += "return_value: " + return_value + "\n";
    res += "operation: " + operation + "\n";
    res += "packet_total_number: " + packet_total_number + "\n";
    res += "callgram_size: " + callgram_size + "\n";
//    res += "callgram: " + callgram + "\n";
    res += "listener: " + listener + "\n";
    res += "remote_address: " +remote_address + "\n";
    return res;
  }
}

