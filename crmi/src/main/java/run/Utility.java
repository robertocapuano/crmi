package run;


import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utility {
    
           private final static Logger log = LogManager.getLogger(Utility.class );
 
      /**
     * Types
     *  B byte signed byte
     *  C char character
     *  D double double precision IEEE float
     *  F float single precision IEEE float
     *  I int integer
     *  J long long integer
     *  L; ... an object of the given class
     *  S short signed short
     *  Z boolean true or false
     *  [ ... array
     */

    private final String expandType( char type )
    {
      switch (type)
      {
	case 'B':
	  return "Byte";
	case 'C':
	  return "Char";
	case 'D':
	  return "Double";
	case 'F':
	  return "Float";
	case 'I':
	  return "Int";
	case 'S':
	  return "Short";
	case 'Z':
	  return "Boolean";
      } // end switch

      return "";
    }

  static Properties run_properties= new Properties();

  static
  {
    try { run_properties.load( new java.io.FileInputStream( System.getProperty("properties", "run.properties") )) ; }
    catch (java.io.IOException ioe) { log.info("errore sul file proprieta':" + ioe.toString() ); }
  }

  /**
   * Legge e converte una proprieta' di sistema.
   * Miglioramento: gestire un file di proprieta'
   */
  public static int getIntProperty( final String property_name, final int default_value )
  {
    String prop_value = run_properties.getProperty( property_name );

    if (prop_value != null )
    {
      try{ return Integer.parseInt(prop_value); } catch (NumberFormatException nfe ) { return default_value; }
    }
    else
      return default_value;
  }

  /**
   * Legge e converte una proprieta' di sistema.
   */
  public static String getStringProperty( final String property_name, final String default_value )
  {
    String prop_value = run_properties.getProperty( property_name );

    if (prop_value != null )
      return prop_value;
    else
      return default_value;
  }

  /**
   * Show the resource status of the system
   */
  public static String getStatus()
  {
    String res = "";
    res += run.exec.Dispatcher.getStatus();
    res += run.session.SessionTable.getStatus();
    res += run.transport.TransportTable.getStatus();

    return res;
  }

  /**
   * Converte un unsigned byte in signed short
  public final static short byteToShort( byte b )
  {
    return (short) (b<0 ? b+256 : b);
  }

  public final static int shortToInt( short s )
  {
    return (int) ( s<0 ? s + 65536 : s );
  }
   */


}