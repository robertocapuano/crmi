package run.serialization;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import java.io.ObjectStreamClass;

/**
 * Mappa Class -> SUID
 * in modo da verificare se un tipo classe e' gia' stata serializzata.
 * E' implementata con una HashSet
 * Usata durante la serializzazione per le classi
 */

public class ClassTable
{
         private final Logger log = LogManager.getLogger(this.getClass() );
 
  private Set class_table;

  public ClassTable()
  {
    class_table = new HashSet();
  }

  void reset()
  {
    class_table.clear();
  }

  /**
   * Inserisce la classe nel db
   *
   * @return SUID della classe
   */
  void put( Class class_type )
  {
    class_table.add( class_type );

/*    long suid = 0;

    if (!class_type.isArray())
    {
      try { suid = class_type.getField("SUID").getLong(null); }
      catch ( Exception reflection_exception ) { Debug.print( reflection_exception.toString() ); }
    }
    else
    {
      suid = ObjectStreamClass.lookup(class_type).getSerialVersionUID();
    }

    return suid;
    */
  }

  /*
   * @return Il valore 0 indica che e' la prima volta che si presenta l'oggetto alla class_table
  long get( Class class_type )
  {
    Long SUID = (Long) class_table.get( class_type );

    if (suid!=null)
      return suid.longValue();
    else
      return 0;
  }
   */

  boolean has( Class class_type )
  {
    return class_table.contains( class_type );
  }
}