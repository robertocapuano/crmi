package clio;


import java.lang.reflect.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Avvia il ClusterClassLoader
 */

public class Main {
   private final Logger log = LogManager.getLogger(this.getClass() );
  
  public Main()
  {

  }

  public static void main(String[] args) throws Exception
  {
    if (args.length==0)
    {
      System.err.println( "Formato: java clio.Main nomeclasse" );
      return;
    }
    else
    {
      ClusterClassLoader ccl = ClusterClassLoader.getClusterClassLoader();

      Class application_class = Class.forName( args[0], true, ccl );
      Method main = application_class.getMethod( "main", new Class[] { Class.forName("[Ljava.lang.String;") } );

      Object[] param = new Object[] { new String[0]  };
      main.invoke( null, param );

//      Debug.println( ccl.toString() );
    }

  }

}