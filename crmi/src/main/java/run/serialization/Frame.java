package run.serialization;

/**
 * Frame rappresenta i dati raw inviati/da inviare a remoto
 *
 */

public class Frame
{
  // dati contenuti nel frame
  protected byte[] byte_frame;
  protected int size;

  public Frame( int _size )
  {
    size = _size;
    byte_frame = new byte[size];
  }

  public Frame( byte[] _byte_frame )
  {
    byte_frame = _byte_frame;
    size = _byte_frame.length;
  }

  public final byte[] toArray()
  {
    return byte_frame;
  }

  public final void setByteFrame( byte[] _byte_frame )
  {
    byte_frame = _byte_frame;
    size = byte_frame.length;
  }

  public final int getSize()
  {
    return size;
  }

  public final void free()
  {
    byte_frame = null;
  }

  /*
   *  Accessi inline (?) in quanto dichiarati final, valore restituito short in modo da evitare valori<0
   */

  public final short get( int index )
  {
    short s = byte_frame[index];
    return  s>=0 ? s : (short) (s + 256);
  }

  public final void put( int index, byte value )
  {
    byte_frame[index] = value;
  }

  public String toString()
  {
    int last = Math.min(size, 1024);
    String res = "size: " + size + " < ";

    for ( int i=0; i<last; ++i )
      res += get(i) + " ";

    res += ">\n";
    return res;
  }
}

