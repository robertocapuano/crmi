package aegis;

import run.*;
import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

public class Y extends X implements Transportable {

// campi della classe Y

  int c;

  public int get()
  {
    return c;
  }

// costruttore di default
  public Y(int _a, int _b, int[] _ia, int _c)
  {
    super (_a, _b, _ia );
    c = _c;
  }

  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( "<"+ super.toString() );

//    res.append( "< a: " + a + " b: " + b + " c: " + c + " ia: <");

//    for ( int i=0; i<ia.length; ++i )
//      res.append( ia[i] + ", " );

    res.append( " c: " + c + ">");
    return res.toString();
  }

  /*
   * Metodi necessari per la serializzazione
   */
  public Y()
  {
    super();
  }

  // writeObject/readObject generate da CLIO
  // chiamate durante l'esecuzione

  public void writeObject0( Container.ContainerOutputStream cos ) throws SerializationException
  {
//    super.writeObject( cos );
    cos.writeInt( c );
    return;
  }

  public void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
//    super.readObject( cis );
    c = cis.readInt();
  }

//  int[] ia = new int[10];
//  String s = "pippo";
//  X x = new X();

  public int sizeOf()
  {
//    return SizeOf.INT + SizeOf.string(s) + SizeOf.array( ia ) + SizeOf.object(x );
    return SizeOf.INT;
  }

//  final public static long SUID = java.io.ObjectStreamClass.lookup( Y.class ).getSerialVersionUID();

}