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