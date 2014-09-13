package run.session;


import run.exec.ThreadPool;
import run.Utility;

/**
 * Pool di Session
 */

class SessionSendPool extends ThreadPool
{
  private static int DEFAULT_POOLSIZE = 4;

  public SessionSendPool( String pool_name )
  {
    super( pool_name, SessionSend.class, Utility.getIntProperty("run.session.sessionsendpool.poolsize", DEFAULT_POOLSIZE ) );
  }

  public SessionSendPool( String pool_name, int init_size )
  {
    super( pool_name, SessionSend.class, init_size );
  }
}