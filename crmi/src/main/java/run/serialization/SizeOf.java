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

import java.lang.reflect.*;


import run.FastTransportable;

//import run.serialization.Container;

/**
 * Questa classe e' usata per la serializzazione.
 * Le dimensioni ritornate comprendono il protocollo implementato in @ref run.serialization.Container
 */
public abstract class SizeOf
{
  public final static int VOID = 0;
  public final static int BOOLEAN = 1;
  public final static int BYTE = 1;
  public final static int CHAR  = 2;
  public final static int SHORT = 2;
  public final static int INT = 4;
  public final static int LONG = 8;
  public final static int FLOAT = 4;
  public final static int DOUBLE = 8;


  /**
   * Dimensioni del protocollo di serializzazione
   */
//  private static final int OBJECT_HEADER = run.serialization.Container.SIZE_OF_OBJECT_HEADER;
//  private static final int OBJECT_ARRAY_HEADER = run.serialization.Container.SIZE_OF_OBJECT_ARRAY_HEADER;
//  private static final int PRIMITIVE_ARRAY_HEADER = run.serialization.Container.SIZE_OF_PRIMITIVE_ARRAY_HEADER;
//  private static final int STRING_HEADER = run.serialization.Container.SIZE_OF_STRING_HEADER;

  public static final int object( FastTransportable o )
  {
    if (o!=null)
    {
      int size = Container.SIZE_OF_OBJECT_HEADER;

      size += SizeOf.stringUTF8(o.getClass().getName());
      size += o.sizeOf();
      return size;
    }
    else
      return Container.SIZE_OF_NULL;
  }


  public final static int primitive( char type )
  {
      switch (type)
      {
	case 'B':
	  return BYTE;
	case 'C':
	  return CHAR;
	case 'D':
	  return DOUBLE;
	case 'F':
	  return FLOAT;
	case 'I':
	  return INT;
	case 'S':
	  return SHORT;
	case 'J':
	  return LONG;
	case 'Z':
	  return BOOLEAN;
      } // end switch

      return 0;
  }

  /**
   * Dimensione di una stringa
   */
  public final static int string( String str )
  {
    if (str!=null)
    {
      int size = stringUTF8(str);
      size += Container.SIZE_OF_STRING_HEADER;
      return size;
    }
    else
      return Container.SIZE_OF_NULL;
  }

  /**
   * Ritorna la dimensione della codifica in utf8.
   * La lunghezza comprende i due caratteri che rappresentano la lunghezza della stringa.
   */
  public final static int stringUTF8( String str )
  {
    int strlen = str.length();
    int utflen = 0;
    char[] charr = new char[strlen];
    int c, count = 0;

    str.getChars(0, strlen, charr, 0);

    for (int i = 0; i < strlen; i++) {
	c = charr[i];
	if ((c >= 0x0001) && (c <= 0x007F)) {
	    utflen++;
	} else if (c > 0x07FF) {
	    utflen += 3;
	} else {
	    utflen += 2;
	}
    }

    return utflen +2;
  }

  /**
   * Dimensione di un array/matrice
   */
  public final static int array( Object array )
  {
    if (array!=null)
    {
      int size = Container.SIZE_OF_ARRAY_HEADER;
      Class array_type = array.getClass();
      size += SizeOf.stringUTF8(array_type.getName() );
      assert( array_type.isArray() );
      Class component_type = array_type.getComponentType();

      if (component_type.isArray())
      {
	Object[] matrix = (Object[]) array;
	for ( int i=0; i<matrix.length; ++i )
	  size += SizeOf.array(matrix[i]);
	return size;
      }
      else
      if (component_type.isPrimitive())
      {
	return size+SizeOf.array0( array );
      }
      else
      if (component_type==String.class)
      {
	return size+SizeOf.stringArray( (String[]) array );
      }
      else
      {
	return size+SizeOf.objectArray( (FastTransportable[]) array );
      }
    }
    else
    {
      return Container.SIZE_OF_NULL;
    }

  }

  /**
   * Dimensione di un array di primitivi
   */
  private final static int array0( Object array )
  {
    assert( array!=null );

    Class cl = array.getClass();
    String array_type = cl.getName();

    assert( cl.isArray() && cl.getComponentType().isPrimitive() );

    int len = Array.getLength(array);
    int component_size = SizeOf.primitive( array_type.charAt( array_type.length()-1 ) ); // [I

    /*
    Class component_class = cl.getComponentType();
    String component_type = component_class.getName();
    int component_size = primitive( component_type.charAt(0) );
    */

    // dimensione del vettore
    int size = component_size * len;

    return size;
  }
  /**
   * Dimensione di un array/matrice di Stringhe
   */
  private final static int stringArray( String[] string_array )
  {
    assert( string_array!=null );
    int size = 0;

    for ( int i=0; i<string_array.length; ++i )
      size += SizeOf.string( string_array[i] );
    return size;
  }

   /**
   * Dimensione di un array/matrice di oggetti FastTransportable
   */
  private final static int objectArray( FastTransportable[] ft_array )
  {
    int size = 0;

    assert( ft_array!=null );

    for ( int i=0; i<ft_array.length; ++i )
      size += SizeOf.object( ft_array[i] );

    return size;
  }

}