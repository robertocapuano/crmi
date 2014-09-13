package run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.serialization.SerializationException;
import run.serialization.Container;
import run.serialization.SizeOf;

public class RemoteException extends Exception implements FastTransportable
{
        private final Logger log = LogManager.getLogger(this.getClass() );
 
  public RemoteException()
  {
  }

  public RemoteException( String str )
  {
    super();
    message = str;
  }

  public String getMessage()
  {
    return message;
  }

  public void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeString( getMessage() );
  }

  public void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
    message = cis.readString();
  }

  public int sizeOf()
  {
    return SizeOf.string( getMessage() );
  }

  public final static long SUID = clio.SUID.computeSUID( "run.RemoteException" );
//  public static long SUID = java.io.ObjectStreamClass.lookup( RemoteException.class ).getSerialVersionUID();

  // public final long SUID;
/*
  public static final long getSUID()
  {
    // il suid sara' calcolata al tempo della compilazione
    long suid = `.ObjectStreamClass.lookup( RemoteException.class ).getSerialVersionUID();
    return suid;
  }
*/
//  public static final long SUID = ;

  String message;

  public String toString()
  {
    return message;
  }
/*
  public static final void main( String[] args )
  {
    RemoteException remote_ex = new RemoteException( "pippo");
    // Memorizza nel container l'exception
    Container container_res = new Container( SizeOf.object( remote_ex ) );
    // true indica che serializzeremo anche oggetti quindi creare la reference_table
    Container.ContainerOutputStream cos = container_res.getContainerOutputStream( true );
    try { cos.writeObject( remote_ex ); }
    // *** Come compartarsi per questo errore?
    catch (SerializationException se) { Debug.print( se.toString() ); }
    cos.free();
  }
*/
}