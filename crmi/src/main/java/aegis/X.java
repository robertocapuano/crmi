package aegis;

import run.*;
import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

public class X implements Transportable {

// campi della classe X
  int a;
  int b;

  int[] ia;

  public int get()
  {
    return a;
  }

// costruttore di default
  public X(int _a, int _b, int[] _ia)
  {
    a = _a;
    b = _b;
    ia = _ia;
  }


  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( "< a: " + a + " b: " + b + " ia: <");

    for ( int i=0; i<ia.length; ++i )
      res.append( ia[i] + "," );

    res.append( "> >");
    return res.toString();
  }

  /*
   * Metodi necessari per la serializzazione
   */
  public X()
  {
    super();
  }

  // writeObject/readObject generate da CLIO
  // chiamate durante l'esecuzione

  public void writeObject0( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeInt( a );
    cos.writeInt( b );
    cos.writeArray( ia );
    return;
  }

  public void readObject0( Container.ContainerInputStream cis ) throws SerializationException
  {
    a = cis.readInt( );
    b = cis.readInt( );
    ia = (int[]) cis.readArray();
  }

  public int sizeOf0()
  {
    return /*a*/ SizeOf.INT+ /*b*/ SizeOf.INT + /*ia*/ SizeOf.array( ia );
  }

//  public final static long SUID0 = clio.SUID.getSUID(X.class);
//  final public static long SUID = java.io.ObjectStreamClass.lookup( X.class ).getSerialVersionUID();

}