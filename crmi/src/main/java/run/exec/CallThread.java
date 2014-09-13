package run.exec;

import java.lang.reflect.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.*;
import run.reference.*;
import run.session.*;
import run.stub_skeleton.*;
import run.transport.Opcodes;
import run.serialization.Container;
import run.serialization.SerializationException;
import run.serialization.SizeOf;
import rio.registry.RegistryException;

/**
 * Questa classe e' attiva per liberare il thread di sistema
 *
 * Implementare una GC qui per liberare SessionReceice SessionSend, Thread
 *
 */

public class CallThread extends Thread implements CallgramListener
{
        private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * TIMEOUT prima di una verifica
   */
  protected static long TIME_OUT = 1000; // 1 secondo
  /**
   * Counter dei threads creati
   */
  private static int counter = 0;
  /**
   * Pool a cui appartiene il thread
   */
  CallThreadPool thread_pool;

  private final static Class Container_class = Container.class;

  /**
   * Dati della chiamata
   */
  private Callgram callgram_event;

  public CallThread( ThreadGroup tg, CallThreadPool ctp )
  {
    super( tg, "CallThread: " + counter++ );

    callgram_event = null;
    thread_pool = ctp;
    start();
  }

  /**
   * metodo chiamato dalla SessionReceive/SessionSend
   */
  public void execCall( Callgram callgram )
  {
    // operazione
    switch (callgram.getOperation() )
    {
      // chiamata da SessionReceive
      case Callgram.CALL:
	synchronized (this)
	{
	  // Setta i dati
	  callgram_event = callgram;
	  // sveglia il thread che svolgera' l'esecuzione in un thread separato
	  this.notifyAll();
	  // restituisce il thread al Dispatcher
	  break;
	}
      // Chiamata da SessionSend
      case Callgram.RETURN:
	/* ritorno della chiamata
	** libera le risorse
	** libera le sessioni: SessionSend, SessionReceive
	** Il container, il frame
	** il thread
	*/
	callgram.free();
//	SessionTable.free( callgram.getSession() );
	// restituisce il thread al pool
	thread_pool.put( this );
	break;
      // Chiamata da SessionSend
      case Callgram.REMOTE_EXCEPTION:
	// Una REMOTE_EXCEPTION o un ERROR non e' generabile sull'invio
	// e sono gestiti solo per completezza
	// CallThread non gestisce errori REMOTE_EXCEPTION generati quando
	// viene effettuata la chiamata, ne' sul valore di ritono
	// ma solo sulla chiamata locale

	// la spedizione del valore di ritono non e' andata a buon fine

	// Stampa l'errore:
        Container.ContainerInputStream cis = callgram.getContainer().getContainerInputStream(true);

	RemoteException re = null;

	try
	{
	  re = (RemoteException) cis.readObject(/*RemoteException.class*/);
	}
	catch (SerializationException se )
	{
	  log.info( se.toString() );
	}

	cis.close();
	System.err.println( "RUN System Error: " + re.getMessage() );
	// libera le risorse
	// libera le sessioni: SessionSend, SessionReceive
	callgram.free();
	// restituisce il thread al pool
	thread_pool.put( this );

	// **** per verificare se la spedizione del valore di ritorno e' andata bene
	// **** inserire qui altro codice

	break;
      case Callgram.ERROR:
	// Anche questo errore non e' generabile, ed e' gestito solo per completezza
	// Stampa l'errore:
	System.err.println( "Remote error: " + callgram.toString() );
	callgram.free();
	thread_pool.put( this );
      default:
        log.info( "CallThread error" );
	break;
    } // end switch

    return;
  }

/*
  // Debug
  public void run_debug()
  {
    Callgram callgram_reply;

    while (true)
    {
      // finche' non si riceve un callgram
      synchronized (this)
      {
	while (callgram_event==null)
	  try { wait(); }
	  catch (InterruptedException ie) { log.info( ie.toString() ); }

	// svegliato da execCall
	assert( callgram_event != null );

	Callgram c = callgram_event;
	log.info( callgram_event.toString() );
	if (c.getOperation()==Callgram.CALL)
	{
	  byte[] ba = new byte[10000];
	  for ( int i=0; i<ba.length; ++i )
	    ba[i] = (byte) (i%64);

	  int framesize = run.serialization.SizeOf.array(ba) + run.serialization.SizeOf.DOUBLE;
	  Container container = new Container( framesize );
	  Container.ContainerOutputStream cos = container.getContainerOutputStream(false);

	  try
	  {
	    cos.writeByteArray(ba);
	    cos.writeDouble(2);
	    cos.close();
	  }
	  catch (Exception e) {}

	  Callgram callgram = new Callgram( Callgram.RETURN, null, this, container );


	  SessionSend session_send = SessionTable.reverseReceiveToSend( (SessionReceive) c.getSession() );
	  // invia il risultato
	  session_send.send( callgram );
	}

	return;
      }
    }
  }
*/


  public void run()
  {
    Callgram callgram_reply;

    while (true)
    {
      // finche' non si riceve un callgram
      synchronized (this)
      {
	while (callgram_event==null)
//	  try { wait(TIME_OUT); }
	  try { wait(); }
	  catch (InterruptedException ie) { log.info( ie.toString() ); }

	// svegliato da execCall
	assert( callgram_event != null );

	/*
	** Esegue la chiamata in questo thread
	*/
	callgram_reply = execLocalCall( callgram_event );
      }
      // in caso il callgram e' null restituisce un errore
      assert( callgram_reply!=null && (callgram_reply.getOperation()==Callgram.RETURN || callgram_reply.getOperation()==Callgram.REMOTE_EXCEPTION || callgram_reply.getOperation()==Callgram.ERROR) );
      // Inverte la direzione della session, la vecchia SessionReceive e' rimossa automaticamente
      SessionSend session_send = SessionTable.reverseReceiveToSend( (SessionReceive) callgram_event.getSession() );
      // consumazione dei dati effettuata.
      // rimuove il riferimento alla sessione
      callgram_event.setSession(null);
      callgram_event.free();
      callgram_event = null;
      // invia il risultato
      session_send.send( callgram_reply );
    } // end forever
  } // end run

  /**
   * Esegue la up-call al servizio.
   */
  private Callgram execLocalCall( Callgram callgram )
  {
    byte operation;
    int return_value;
    Container container_res = null;

    try
    {
      RemoteServiceReference r_ref = callgram.getRemoteServiceReference();
      assert( r_ref != null );

      // Interrogazione del Registry
      // Comprende anche i servizi del Registry: register, lookup
      LocalServicesReference l_ref = rio.registry.Local.lookup( r_ref );

      Skeleton service_skeleton = l_ref.getSkeleton();

      assert( service_skeleton != null);

      log.info( r_ref.toString() );
      String service_name = "invoke_" + r_ref.getServiceName() + "_" + r_ref.getServiceNumericSignature();

      log.info( "service_skeleton: " + service_skeleton );
      log.info( "service_name: " + service_name );

      // skeleton_class e' un sotto-tipo della classe Skeleton
      Class skeleton_class = service_skeleton.getClass();

      log.info( "skeleton_class: " + skeleton_class.getName() );

      Method service_invoke = skeleton_class.getMethod( service_name, new Class[] { Container_class } );


      // **** Tutto il sistema serve per eseguire questa singola riga di codice! ****

      // effettua la chiamata
      container_res = (Container) service_invoke.invoke( service_skeleton, new Object[] { callgram.getContainer() } );
      operation = Callgram.RETURN;
      return_value = 0;
    }
    catch ( RegistryException registry_ex )
    {
      // errore: il servizio non e' stato trovato?
      operation = Callgram.REMOTE_EXCEPTION;
      log.info( registry_ex.toString() );
      return_value = 1;

      RemoteException remote_exception = new RemoteException( registry_ex.getMessage() );
      container_res = new Container( SizeOf.object( remote_exception ) );
      Container.ContainerOutputStream cos = container_res.getContainerOutputStream(true);
      try
      {
	cos.writeObject( remote_exception );
      }
      catch (SerializationException se )
      {
	operation = Callgram.ERROR;
	return_value = -1;
	container_res = new Container( SizeOf.VOID );
      }
    }
    catch (NoSuchMethodException nsme)
    {
      // errore non previsto: il servizio non e' stato trovato?
      log.info( nsme.toString() );
//      operation = Callgram.ERROR;
      operation = Callgram.REMOTE_EXCEPTION;
      return_value = 2;
      RemoteException remote_exception = new RemoteException( nsme.getMessage() );
      container_res = new Container( SizeOf.object( remote_exception ) );
      Container.ContainerOutputStream cos = container_res.getContainerOutputStream(true);
      try
      {
	cos.writeObject( remote_exception );
      }
      catch (SerializationException se )
      {
	operation = Callgram.ERROR;
	return_value = -1;
	container_res = new Container( SizeOf.VOID );
      }
    }
    catch (IllegalAccessException ilae)
    {
      // errore non previsto: il servizio non e' stato trovato?
      log.info( ilae.toString() );
//      operation = Callgram.ERROR;
      operation = Callgram.REMOTE_EXCEPTION;
      return_value = 3;
      RemoteException remote_exception = new RemoteException( ilae.getMessage() );
      container_res = new Container( SizeOf.object( remote_exception ) );
      Container.ContainerOutputStream cos = container_res.getContainerOutputStream(true);
      try
      {
	cos.writeObject( remote_exception );
      }
      catch (SerializationException se )
      {
	operation = Callgram.ERROR;
	return_value = -1;
	container_res = new Container( SizeOf.VOID );
      }
    }
    catch (InvocationTargetException ite)
    {
      // il metodo invocato ha generato una exception
      Exception service_exception = (Exception) ite.getTargetException();

      if (service_exception instanceof RemoteException )
      {
	RemoteException remote_ex = (RemoteException) service_exception;

	// questo errore si puo' sempre verificare.
	operation = Callgram.REMOTE_EXCEPTION;
	return_value = 4;
	// Memorizza nel container l'exception
	container_res = new Container( SizeOf.object(remote_ex) );
	// true indica che serializzeremo anche oggetti quindi creare la reference_table
	Container.ContainerOutputStream cos = container_res.getContainerOutputStream( true );
	try
	{
	  cos.writeObject( remote_ex );
	}
	// *** Come compartarsi per questo errore?
	catch (SerializationException se)
	{
	  SerializationException serialization_ex = (SerializationException) se;
	  // errore non previsto: il servizio non e' stato trovato?
	  log.info( serialization_ex.toString() );
	  operation = Callgram.ERROR;
	  return_value = -1;
	  container_res = new Container( SizeOf.VOID );
	}

	cos.free();
      }
      else
      if (service_exception instanceof SerializationException)
      {
	SerializationException serialization_ex = (SerializationException) service_exception;
	// errore non previsto: il servizio non e' stato trovato?
	log.info( serialization_ex.toString() );
	return_value = -1;
	operation = Callgram.ERROR;
	container_res = new Container( SizeOf.VOID );
      }
      else
      {
	log.info( "CallThread: Unknown exception: " + service_exception.toString());
	operation = Callgram.ERROR;
	return_value = -2;
	container_res = new Container( SizeOf.VOID );
      }
    }


    // prepara un callgram di risposta invertendo sender e receiver
    // callgram.getRemoteAddress() individua il chiamante
    Callgram callgram_res = new Callgram( operation, return_value, null, this, container_res );
    // imposta il listener del risultato
    callgram_res.setListener( this );

    return callgram_res;
  }
}
