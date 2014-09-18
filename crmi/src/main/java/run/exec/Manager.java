/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> 
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