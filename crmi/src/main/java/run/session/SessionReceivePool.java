package run.session;


import run.Utility;
import run.exec.ThreadPool;

/**
 * Pool di Session
 */

class SessionReceivePool extends ThreadPool
{
  protected static int DEFAULT_POOLSIZE = 4;

  public SessionReceivePool( String pool_name )
  {
    super( pool_name, SessionReceive.class, Utility.getIntProperty( "run.session.sessionreceivepool.poolsize", DEFAULT_POOLSIZE ) );
  }

  public SessionReceivePool( String pool_name, int init_size )
  {
    super( pool_name, SessionReceive.class, init_size );
  }
}