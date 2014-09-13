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
