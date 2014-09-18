/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> 
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