package run.exec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import run.RemoteException;
import run.session.*;
import run.serialization.Container;
import run.serialization.SerializationException;
import run.transport.TransportException;

/**
 * Tre momenti nell'invio.
 * 1) Invio del callgram, avviato da Transport.send ed ha il suo thread.
 * 2) Lettura del risultato e morizzazione via execCall
 * 3) Restituzione del risultato via execRemoteCall
 */

public class Pusher implements CallgramListener
{
    
         private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * TIMEOUT prima di una verifica
   */
  protected static long TIME_OUT = 1000; // 1 secondo

  /**
   * Callgram con la risposta da remoto
   */
  Callgram callgram_reply;

  /**
   * Dati della chiamata
   */
  Pusher( )
  {
    super();
  }

  void free( )
  {
    // libera le risorse allocate
    callgram_reply = null;
  }

  /**
   * Chiamata remota, Usa il thread up-call
   */
  Callgram execRemoteCall( Callgram callgram_send ) throws RemoteException, TransportException, ExecException, SerializationException
  {
//    log.info( "execRemoteCall(): " + callgram_send.toString() );
    callgram_reply = null;

    SessionSend send_session = SessionTable.newSessionSend( callgram_send.getRemoteAddress() );
    callgram_send.setListener( this );
    send_session.send( callgram_send );

    // Restituisce la risposta quando l'avra' ricevuta da remoto.
    // Il thread del client si blocca qui
    synchronized (this)
    {
      while ( callgram_reply == null )
	try { wait( TIME_OUT ); } catch (InterruptedException ie ) { log.info( ie.getMessage() ); }
    }

    try
    {
      // analisi della risposta
      switch ( callgram_reply.getOperation() )
      {
	case Callgram.RETURN:
	    return callgram_reply;
	case Callgram.REMOTE_EXCEPTION:
	    Container.ContainerInputStream cis = callgram_reply.getContainer().getContainerInputStream( true );
	    RemoteException remote_exception = (RemoteException) cis.readObject( /*RemoteException.class*/ );
	    throw remote_exception;
	case Callgram.ERROR:
	  throw new ExecException( "Errore nell'invocazione remota" );
	default:
	  log.info( "Callgram.operation sconosciuto" );
	  return callgram_reply;
      } // end switch
    }
    finally
    {
      callgram_send.setSession(null);
      callgram_reply.freeSession();
    }
  } // end execRemoteCall()

  /**
   * Listener degli eventi del callgram, thread avviato da downcall
   */
  public void execCall( Callgram callgram_event )
  {
    switch ( callgram_event.getOperation() )
    {
      // completato l'invio
      case Callgram.CALL:
	// NOP
        break;
      case Callgram.REMOTE_EXCEPTION:
      case Callgram.RETURN:
      case Callgram.ERROR:
	// risposta
	synchronized (this)
	{
	  callgram_reply = callgram_event;
	  notifyAll();
	}
	break;
      default:
	log.info( "Callgram.operation sconosciuto" );
	break;
    } // end switch
  } // end execCall

} // end Pusher
