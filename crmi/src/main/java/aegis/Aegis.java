package aegis;

//import com.borland.primetime.util.Debug;
import org.apache.logging.log4j.*;

import rio.registry.Registry;
import rio.registry.RegistryException;

import run.RemoteException;
import run.Services;
import run.Utility;

import rio.registry.*;

import run.transport.*;

public class Aegis implements Rs
{
    
      private static final Logger log = LogManager.getLogger(Aegis.class );
  
  public Aegis()
  {
  }

  public static void main(String[] args) throws RegistryException, RemoteException
  {
    NetAddress registry_address = null;
    Aegis server = new Aegis();
    String prop_registry_address = Utility.getStringProperty( "rio.registry.address", "10.0.2.1" );
    int prop_registry_port = Utility.getIntProperty( "rio.registry.port", 2002 );

    try
    {
      registry_address = new IPAddress( java.net.InetAddress.getByName(prop_registry_address), prop_registry_port );
    }
    catch (Exception e ) { log.info( e.toString() ); }

    Registry.register( registry_address, "aegis", server );

    // serve per far modificare la classe X
    X dummy = new X(10,20, new int[0] );
/*

    for ( ; ; )
    {
      try { Thread.sleep( 2000 ); } catch (Exception e ) { log.info( e.toString() ); }
      System.out.println( Utility.getStatus() );

      System.out.println( run.exec.Dispatcher.getStatus() );
      System.out.println( SessionTable.getStatus() );
      System.out.println( run.transport.TransportTable.getTransport(0).toString() );
    }

//    Client.main( args );
*/
  }


  public int f1( X x, int[] ia, int i ) throws RemoteException
  {
    log.info( "f1 invoked, i:" + i );

    System.out.println( Utility.getStatus() );

    return i+1;
  }

  public int[] f2( X x, X[] xa, int i ) throws RemoteException
  {
    log.info( "f2 invoked, i: " + i  );
    if (i==0)
      throw new RemoteException("Tutto ok: questa eccezione e' stata generata da remoto nel metodo f2");
    else
      return new int[] { 47, 48 };
  }

  public X f3( int i ) throws RemoteException
  {
    log.info( "f3 invoked, i: " + i  );
    X x = new X(32, 64, new int[] { 9, 3 } );
    return x;
//    Y y = new Y(32,64, new int[] { 9,3}, 48 );
//    return y;
  }

  public Z f4( int n ) throws RemoteException
  {
    log.info("f4 invoked, n: " + n );
    Z z3 = new Z( 103, null );
    Z z2 = new Z( 102, z3 );
    Z z1 = new Z( 101, z2 );

    for ( Z i=z1; i!=null; i = i.next )
      log.info( i.toString() );

    return z1;
  }

  public void f5( ) throws RemoteException
  {
    log.info( "f5 invoke" );

    return;
  }

  public float f6( X x, long l, String s ) throws RemoteException
  {
    log.info( "f6: " + x.toString() + " l: " + l + " s: " + s );
    return x.a+l+s.length();
  }

  public String f7( X x, int l, String s ) throws RemoteException
  {
    log.info( "f7() x: " + x.toString() + " l: " + l + " s: " + s );
    return l+s;
  }

  public X f8( long l, String s, X x ) throws RemoteException
  {
    log.info( "f8(): l:" + l + "s: " + s + " x: " + x.toString() );
    return new X( (int) l, s.length(), x.ia );
  }


}