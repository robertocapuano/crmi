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

import java.util.*;

import run.reference.*;
import run.session.*;

/*
** Utilizza UDP con due sliding window per effettuare l'invio dei dati
** Transport vede solo i pacchetti ed implementa la Sliding Window
** Ricostruisce un callgram, a quel punto chiama il TransportListener interessata a quella chiamata.
*/

public abstract class Transport extends Thread //implements Runnable
{
  /**
   * Dimensione di default della sliding window
   */
  public static final int DEFAULT_WINDOW_SIZE = 16;

  // dati di istanza

  /**
   * Tipo di trasporto: UDP/VIA
   */
  protected byte type_of_transport;

  /**
   * Indirizzo locale
   */
  protected NetAddress local_address;

  /**
   * Dimensione dei packet
   */
  protected int packet_size;

  /**
   * Dimensione suggerita della sliding window
   */
  protected int window_size;

  /**
   * Pool dei packets
   */
  protected PacketPool packet_pool;

  // metodi di istanza

  public Transport( byte _type_of_transport, NetAddress _local_address, int _packet_size, int _window_size )
  {
    type_of_transport = _type_of_transport;
    local_address = _local_address;
    packet_size = _packet_size;
    window_size = _window_size;
    packet_pool = new PacketPool( packet_size );
  }

  public final int getWindowSize()
  {
    return window_size;
  }

   /**
   * Registra un dispatcher per la notifica
  public synchronized void registerCallback( CallgramListener listener )
  {
    callback = listener;
  }
   */

  /**
   * Indirizzo associato al transport
   */

  public final NetAddress getLocalAddress()
  {
    return local_address;
  }

  /**
   * Restituisce il tipo di transport in base a NetAddress.IP_ADDRESS, NetAddress.VIA_ADDRESS
   */
  public final byte getType( )
  {
    return type_of_transport;
  }

  /**
   * Dimesione dei pacchetti
   */
  public final int getPacketSize()
  {
    return packet_size;
  }

  /**
   * Massima quantita' di dati trasmettibile in un pacchetto
   */
  public final int getMaxDataSize()
  {
    return packet_size - Packet.SIZE_OF_DATA_HEADER;
  }

  /**
   * Invia il Packet per la trasmissione
   */
  public abstract void send( Packet p );

  /**
   * Gestione del trasporto
   */
  public abstract void run();

  /**
   * Debug
   */
  public String toString()
  {
    String res = "<run.transport.Transport>\n";
    res += local_address.toString() + "\n";
//    res += packet_pool.toString() + "\n";
    res += "packet_size: " + packet_size + "\n";
    res += "window_size: " + window_size + "\n";
    res += "type_of_transport: " + type_of_transport + "\n";
    return res;
  }
}
