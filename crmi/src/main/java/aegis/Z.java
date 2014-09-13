package aegis;

import run.*;
import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

public class Z implements Transportable
{
  int val;
  Z next;
//  String s;

  public Z()
  {
  }

  public Z( int _val, Z _next )
  {
    val = _val;
    next = _next;
  }

  public String toString()
  {
    String res = "";

    res += "< val: " + val + " next: " + next + ">";
    return res;
  }

  public void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
//    cos.writeString( s );
    cos.writeInt( val );
    cos.writeObject( (FastTransportable) next );
    return;
  }

  public void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
//    s= cis.readString();
    val = cis.readInt( );
    next = (Z) cis.readObject();
  }

  public int sizeOf()
  {
    return SizeOf.INT + SizeOf.object((FastTransportable) next);
  }

//  final public static long SUID = java.io.ObjectStreamClass.lookup( Z.class ).getSerialVersionUID();

}