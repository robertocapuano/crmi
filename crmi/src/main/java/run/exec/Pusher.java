/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> Capuano <Roberto Capuano <roberto@2think.it>@2think.it>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
