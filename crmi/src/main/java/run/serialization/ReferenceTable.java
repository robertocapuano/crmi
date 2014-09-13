package run.serialization;

import java.util.*;

/**
 * ReferenceTable
 * reference in memoria -> handle (numeri di serie delle istanze degli oggetti sullo stream).
 * E' una HashMap
 * Usata durante la serializzazione per gli oggetti
 */

public class ReferenceTable
{
  private Map reference_table;

  /**
   * Prossima riferimento libero: e' un progressivo
   * 0 riservato per indicare l'assenza dell'oggetto
   */
  private int next_handle;

  public ReferenceTable()
  {
    reference_table = new HashMap();
    next_handle = 1;
  }

  void reset()
  {
    reference_table.clear();
    next_handle = 1;
  }

  int put( Object reference )
  {
    reference_table.put( reference, new Integer( next_handle ) );
    return next_handle++;
  }

  /*
   * Verifica se un oggetto e' gia' stato serializzato
  boolean has( Object reference )
  {
    return reference_table.containsKey( reference );
  }
   */

  /**
   * Il valore 0 indica che e' la prima volta che si presenta l'oggetto alla reference_table
   */
  int get( Object reference )
  {
    Integer i_handle = (Integer) reference_table.get(reference);

    if ( i_handle != null )
      return i_handle.intValue();
    else
      return 0;
  }

}