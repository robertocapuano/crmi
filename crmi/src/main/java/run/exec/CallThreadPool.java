package run.exec;


import java.util.*;
import java.lang.reflect.*;

import run.exec.ThreadPool;

//import run.transport.Callgram;

/**
 * Pool di threads pre-forkati.
 * La lista di tutti e' thread e' una array-list.
 * La lista dei thread liberi e' un array-list;
 * La lista dei result e' una LinkedList.
 */

public class CallThreadPool extends ThreadPool
{
  public static int DEFAULT_POOLSIZE = 4;

  public CallThreadPool( String pool_name )
  {
    super( pool_name, CallThread.class, run.Utility.getIntProperty("run.exec.callthreadpool.poolsize", DEFAULT_POOLSIZE) );
  }

  public CallThreadPool( String pool_name, int init_size )
  {
    super( pool_name, CallThread.class, init_size );
  }

}