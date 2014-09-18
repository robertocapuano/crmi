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

import java.util.*;

import run.session.CallgramListener;
import run.session.Callgram;

/*
** Debug
*/
import run.session.*;

/**
 *  Gestisce le chiamate in arrivo.
 *  Svegliato dal Transport all'arrivo di un pacchetto con un interrupt.
 *  Per essere sicuro effettua un polling dei transport ogni 10 ms.
 */

public class Dispatcher
{
/**
 * Pool dei threads pre-forkati
 */
  private static ThreadPool thread_pool;

  static
  {
    // numero di thread di default
    thread_pool = new CallThreadPool( "Dispatche/CallThreadPool");
  }

  /**
   * Listener per i callgram ricevuti
   */
  static CallgramListener dispatcher = new CallgramListener()
  {
    /**
     * Preleva un thread dal pool e delega la chiamata
     */
    public void execCall( Callgram c )
    {
      CallThread ct = (CallThread) thread_pool.get();
      ct.execCall( c );
    }
  };

  /**
   * Restituisce il listener del dispatcher
   */
  public static CallgramListener getDispatcher()
  {
    return dispatcher;
  }

  public static String getStatus()
  {
    String res = "<" + Dispatcher.class.getName() + ">\n";
    res += thread_pool.toString();
    return res;
  }

/* Debug
  public static void main( String[] args )
  {
    run.transport.Transport transport = run.transport.TransportTable.getTransport(0);
  }
*/
} // end class
