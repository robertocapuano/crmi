package run.transport;

/**
 * Implementa la logica di un pacchetto che appartiene ad un PacketPool di un solo elemento.
 */

public class PacketOne extends Packet
{
  protected boolean free;

  public PacketOne( int _packet_size)
  {
    super( _packet_size );
    free = true;
  }

  public boolean isFree()
  {
    return free;
  }

  public synchronized void free()
  {
    free = true;
  }

  public synchronized Packet get()
  {
    if (free)
    {
      free = false;
      return this;
    }
    else
      return null;
  }

  public synchronized void put()
  {
    free = true;
  }

}