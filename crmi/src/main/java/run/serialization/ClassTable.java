/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> Capuano <Roberto Capuano <roberto@2think.it>@2think.it>
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