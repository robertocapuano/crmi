package run.serialization;

import java.util.ArrayList;
import java.util.List;

/**
  * Gestisce gli handle degli oggetti referenziati
  * Mappa da handle (riferimento ad oggetto sullo stream) ->  riferimenti di oggetti in memoria
  * Basta una semplice ArrayList
  * Usata durante la deserializzazione per gli oggetti
  */

public class HandleTable
{
  private List handle_table;

  public HandleTable()
  {
    handle_table = new ArrayList();
    handle_table.add( null ); // la posizione 0 non e' usata
  }

  void reset()
  {
    handle_table.clear();
    handle_table.add( null ); // la posizione 0 non e' usata
  }

  int put( Object reference )
  {
    handle_table.add( reference );
    return handle_table.size()-1;
  }

  Object get( int handle )
  {
    return handle_table.get( handle );
  }

}