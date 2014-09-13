package run.serialization;

//import java.io.ObjectStreamClass;
import java.util.*;
//import java.io.ObjectStreamClass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gestisce le descrizioni dei tipi delle classi
 * Implementata con una hashtable che mappa SUID con classi
 * E' una idea semplice: gestire la versionizzazione della classe con il SUID: stream unique identifier.
 * Ottimizzazione implementata: i dati delle classi sono validi per tuta la connessione, non solo per la singola chiamata.
 * Quindi metodi e dati sono statici.
 * Usata durante la deserializzazione per le classi
 */

public class SUIDTable
{
         private final static Logger log = LogManager.getLogger(SUIDTable.class );
 
  private static Map suid_table = new HashMap();

  /**
   * Svuota il db delle classi.
   */
  static void reset( )
  {
    suid_table.clear();
  }

  static boolean has( long suid )
  {
    return suid_table.containsKey( new Long(suid) );
  }

  static synchronized void put( long foreign_suid, Class local_class )
  {
    // Verifica che il suid del mittente e' uguale al suid del ricevente
    try
    {
      long internal_suid = clio.SUID.getSUID( local_class );
/*      if (!local_class.isArray())
	internal_suid = local_class.getField("SUID").getLong(null);
      else
	internal_suid = clio.SUID.computeSUID( local_class );
//	internal_suid = ObjectStreamClass.lookup(local_class).getSerialVersionUID();
*/

      assert foreign_suid == internal_suid : "Class: "+local_class.getName()+"SUID: "+ internal_suid +"External SUID: "+foreign_suid ;
    }
    catch (Exception reflection_exception) { log.info( reflection_exception.toString() ); }

    suid_table.put( new Long(foreign_suid), local_class );
  }

  static synchronized Class get( long suid )
  {
    return (Class) suid_table.get( new Long( suid ) );
  }

}