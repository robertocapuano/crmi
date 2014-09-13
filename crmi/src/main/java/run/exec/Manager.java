package run.exec;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import run.session.*;

import run.RemoteException;
import run.serialization.SerializationException;
import run.exec.ExecException;
import run.transport.TransportException;

// debug
import run.transport.*;
import run.serialization.*;
import run.reference.*;

/**
 * Gestisce l'invio dei messaggi remoti.
 * Puo' gestire piu' richieste.
 * Avra' una coda di richieste FIFO e le smista sulla rete.
 */

public class Manager
{
           private final static Logger log = LogManager.getLogger(Manager.class );
 
  /*
   * Pool dei manager di invio/ricevimento chiamata/risposta
  static PusherPool pusher_pool;

  static
  {
    pusher_pool = new PusherPool();
  }
   */

  /**
   * Esegue la chiamata.
   */
  public static Callgram execRemoteCall( Callgram callgram ) throws RemoteException, SerializationException, ExecException, TransportException
  {
    Pusher pusher=null;


    try
    {
      //    Pusher pusher = pusher_pool.get();
      pusher = new Pusher();

      Callgram callgram_reply = pusher.execRemoteCall( callgram );
      return callgram_reply;
    }
    finally
    {
      if (pusher!=null)
	pusher.free();
    }

  } // end execRemoteCall()

  public static void main( String[] args ) throws Exception
  {
    log.info( SessionTable.class.getName() );

    boolean res = selftest();
    log.info( "Selftest " + Manager.class.getName() + ":<"+ res +">");
  }

  private static boolean selftest() throws Exception
  {
    boolean res = true;
    res &= selftest_callgram();
//    res &= selftest_send();
    return res;
  }


  private static boolean selftest_callgram() throws Exception
  {
    NetAddress remote_host = new IPAddress( java.net.InetAddress.getByName("127.0.0.1"), 2051 );

    byte[] ba = new byte[50000];
    for ( int i=0; i<ba.length; ++i )
      ba[i] = (byte) (i%128);

    int framesize = SizeOf.array(ba) + SizeOf.DOUBLE;
    Container container = new Container( framesize );
    Container.ContainerOutputStream cos = container.getContainerOutputStream(false);

    cos.writeArray(ba);
    cos.writeDouble(3);
    cos.close();
    Service remote_service = new Service( "f", "(V)V" );
    run.reference.RemoteServiceReference remote_services_reference = new run.reference.RemoteServiceReference( remote_host, "F", remote_service );

    Callgram callgram = new Callgram( Callgram.CALL, remote_services_reference, null, container );

    Callgram callgram_reply = execRemoteCall( callgram );

    log.info( callgram_reply.toString() );

    return callgram.equals(callgram_reply);
  }

}