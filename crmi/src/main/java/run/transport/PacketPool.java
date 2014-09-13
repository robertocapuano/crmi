package run.transport;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PacketPool
{
      private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * Dimensione frame ethernet
   */
  protected final static int DEFAULT_PACKET_SIZE=1472;

  /**
   *   Numero minimo di packet
   */
  protected final static int DEFAULT_POOL_SIZE = 256;

  protected ArrayList packet_list;
  protected int packet_size;
  protected int pool_size;

  public PacketPool()
  {
    this( DEFAULT_PACKET_SIZE, run.Utility.getIntProperty( "run.transport.packetpool.poolsize", DEFAULT_POOL_SIZE ) );
  }

  public PacketPool( int _packet_size )
  {
    this( _packet_size, run.Utility.getIntProperty( "run.transport.packetpool.poolsize", DEFAULT_POOL_SIZE ) );
  }

  public PacketPool( int _packet_size, int _pool_size )
  {
    pool_size = _pool_size;
    packet_list = new ArrayList( (int) pool_size*3/2 ); // ci riserviamo un po' di tolleranza

    packet_size = _packet_size;
    for ( int i=0; i<pool_size; ++i )
      packet_list.add( new Packet( this, packet_size ) );
  }

  /**
   * Limita la creazione di pacchetti.
   */

  public synchronized Packet get()
  {
    int free_size;

    while ( (free_size = packet_list.size()) == 0 )
      try { wait(); }
      catch (InterruptedException ie ) { log.info( ie.toString() ); }

    return (Packet) packet_list.remove( free_size-1 );
  }


  public synchronized Packet getOrCreate()
  {
    int free_size = packet_list.size();
    if (free_size>0)
      return (Packet) packet_list.remove( free_size-1 );
    else
    {
      ++pool_size;
      return new Packet( packet_size );
    }
  }

  public int getPacketSize()
  {
    return packet_size;
  }

  public synchronized void put( Packet packet )
  {
    packet_list.add( packet );
    notifyAll();
  }

  public synchronized int size()
  {
    return packet_list.size();
  }

  public synchronized int getPoolSize()
  {
    return pool_size;
  }

  public String toString()
  {
    String res = "<" + getClass().getName() + ">\n";
    res += "free: " + size() + "\n";
    res += "capacity: " + getPoolSize() + "\n";
    res += "PacketSize: " + getPacketSize() + "\n";

    return res;
  }

}