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

import java.util.*;
import java.lang.reflect.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Pool di threads pre-forkati.
 * La lista di tutti e' thread e' una array-list.
 * La lista dei thread liberi e' un array-list;
 * La lista dei result e' una LinkedList.
 */

public abstract class ThreadPool
{
         private final Logger log = LogManager.getLogger(this.getClass() );
 
  /**
   * Gruppo dei threads di questo pool
   */
  protected ThreadGroup thread_group;

  /**
   * Numero di thread iniziali per default
   */
  protected static int DEFAULT_POOL_SIZE = 4;

  /**
   * lista di tutti i threads che appartengono al pool
   */
  protected List thread_list;

  /**
   * lista dei threads liberi
   */
  protected List ready_list;

  /**
   * Tipo della classe nel pool
   */
  protected Class pool_type;

  // dati private per l'istanziazione dei threads
  private Class[] param;
  private Constructor constructor;
  private Object[] arg;

  public ThreadPool( String pool_name )
  {
    this( pool_name, Thread.class, run.Utility.getIntProperty( "run.exec.threadpool.poolsize", DEFAULT_POOL_SIZE ) );
  }

  public ThreadPool( String pool_name, Class _pool_type )
  {
    this( pool_name, _pool_type, run.Utility.getIntProperty( "run.exec.threadpool.poolsize",DEFAULT_POOL_SIZE ) );
  }

  public ThreadPool( String pool_name, Class _pool_type, int init_size )
  {
    try
    {
      pool_type = _pool_type;
      thread_group = new ThreadGroup( pool_name );
      thread_list = new ArrayList( init_size );
      ready_list = new ArrayList( init_size );

      // this.getClass() individua il tipo effettivo del threadpool
      param = new Class[] { ThreadGroup.class, this.getClass() };
      constructor = pool_type.getConstructor( param );
      arg = new Object[] { thread_group, this };

      for ( int i=0; i<init_size; i++)
      {
	Thread t = (Thread) constructor.newInstance( arg );
	thread_list.add( t );
	ready_list.add( t );
      }
    }
    catch ( Exception reflect_exception)
    {
      log.info( reflect_exception.toString() );
    }
  }

  /**
   * Restituisce un thread libero
   */
  public synchronized Thread get()
  {
    Thread t = null;

    if ( ready_list.size()==0)
    {
      try
      {
	t = (Thread) constructor.newInstance(arg);
        thread_list.add( t );
      }
      catch ( Exception newinstance_exception ) { log.info( newinstance_exception.getMessage() ); }
    }
    else
    {
      t = (Thread) ready_list.remove(ready_list.size()-1 );
    }

    return t;
  }

  /**
   * Restituzione di un thread
   */
  public synchronized void put( Thread t )
  {
    ready_list.add(t);
  }

  public String toString()
  {
    String res = "<" + getClass().getName() + ">\n";
    res += "size: " + thread_list.size() + "\n";
    res += "free: " + ready_list.size() + "\n";
    return res;
  }

/*
 * Nel caso in cui non vi siano thread disponibili ne raddoppia il numero
 * E' realmente utile?
  private void growThreadList()
  {
      CallThread t;

      int num = ready_list.size();
      for ( int i=0; i<num; ++i )
      {
	t = new CallThread( thread_group, this );
        thread_list.add( t );
        ready_list.add( t );
      }
  }
 */

}