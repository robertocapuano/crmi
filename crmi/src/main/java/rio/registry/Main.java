package rio.registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main
 */

public class Main {
      private final static Logger log = LogManager.getLogger(Main.class );
 
  public Main()
  {
  }

  public static void main(String[] args)  throws ClassNotFoundException
  {
    // istanziazione del Registry
    Class.forName("rio.registry.Local");
    Class.forName("run.transport.TransportTable");
    log.info("Registry avviato");
//    log.info( run.Utility.getStatus() );
    log.info( run.transport.TransportTable.getStatus() );
  }
}