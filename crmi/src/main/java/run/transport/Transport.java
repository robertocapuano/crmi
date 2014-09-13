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
