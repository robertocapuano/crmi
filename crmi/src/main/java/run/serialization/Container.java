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

import java.io.UTFDataFormatException;
import java.lang.reflect.Array;

//import java.io.ObjectStreamClass;

import com.borland.primetime.util.FastStringBuffer;

//import de.fub.bytecode.generic.Type;
//import de.fub.bytecode.util.ByteSequence;

import run.FastTransportable;
import run.serialization.SizeOf;

import run.Utility;

import clio.SUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wrapper per la classe Frame.
 * Rappresenta la serializzazione di:
 * - un oggetto (singolo argomento di una chiamata)
 * - l'intera lista degli argomenti di una chiamata remota (Marshalling)
 * - dati da un Packet
 * Per il processo di serializzazione/deserializzazione usa:
 * - Per gli oggetti
 * 1. la handle_table: handle sullo stream -> reference oggetto in memoria, puo' essere un ArrayList. Per la Deserializzazione
 * 2. la reference_table: reference oggetto in memoria-> handle sullo stream puo' essere una hashtable. Per la serializzazione
 * - Per le classi
 * 1. la hashtable da SUID -> classi
 * 2. il set Classi (->SUID)
 *
 * Formato dello stream:
 * primitivo | (OBJECT_TYPE HANDLE [ CLASS_TYPE SUID [CLASSNAME] [INSTANCE] ] )
 * La composizione dello stream (succesione dei tipi dei dati e' gia' conosciuto dal sistema)
 * E per i tipi di dati primitivi( stringa, array di primitivi, array di oggetti) questo basta.
 * Per gli oggetti il tipo effettivo rispetto al tipo formale.
 * Inoltre, nel caso di oggetti, quello che non conosce e' se per un riferimento e' presente una nuova istanza
 * o un riferimento ad una istanza gia' presente.
 * Per gli array non conosce la dimensione ed il tipo effettivo di ogni elemento.
 *
 * Valori per OBJECT_TYPE:
 *  1)  Per i tipi primitivi: int, short, byte, ...
 *      non e' presente OBJECT_TYPE.
 *  2) Per i tipi non primitivi Oggetti, Stringhe, Arrays, Matrici
 *     abbiamo che puo' valere:
 *    INSTANCE: Sullo stream e' presente l'istanza dell'oggetto
 *    REFERENCE: Sullo stream e' presente l'handle all'oggetto gia' presente in memoria
 *    Seguito dall'handle
 * Valori per CLASS_TYPE:
 *  1) NEWCLASS: Nuovo tipo da memorizzare, segue classname e suid
 *  2) REFCLASS: Riferimento ad un tipo gia' incontrato. Segue solo lo suid
 *
 * Esempi di dati sullo stream.
 * primitivi: dato
 * - Nuova Stringa (StringObject): INSTANCE handle (utf8)stringa
 *   Rif. Stringa (StringObject): REFERENCE handle
 * - Nuovo array: INSTANCE handle NEWCLASS SUID (utf8)classname (int)len [Nuovo Oggetto|Riferimento]
 *                           INSTANCE handle REFCLASS SUID (int)len [Nuovo Oggetto|Riferimento]
 *   Rif. array:  REFERENCE handle
 *
 * Per gli array di primitivi: int, short, string, ...
 * - Nuovo array: INSTANCE handle NEWCLASS (int)len [Nuovo Oggetto|Riferimento]
 *                           INSTANCE handle (int)len [Nuovo Oggetto|Riferimento]
 *   Rif. array:  REFERENCE handle
 *
 * - Nuovo Oggetto: INSTANCE handle NEWCLASS SUID (utf8)classname Istanza
 *                INSTANCE handle REFCLASS SUID Oggetto
 *   Riferimento ad un oggetto: REFERENCE handle
 * Oss: nel caso di REFERENCE non ci puo' mai essere NEWCLASS.
 * ma non cerchiamo di ottimizzare troppo.
 *
 *
 * Oss.: la scelta di usare il SUID per referenziare le classi.
 * e un progressivo gli oggetti si e' dimostrata vincente.
 * In quanto se per gli oggetti avessimo scelto il codice hash avremmo commesso un errore.
 * Infatti non si sarebbe potuto referenziare in maniera efficiente gli array e le matrici.
 *
 */

public class Container
{
         private final static Logger log = LogManager.getLogger(Container.class );
 
  /**
   * Valori per OBJECT_TYPE:
   * INSTANCE: Sullo stream e' presente l'istanza dell'oggetto
   * REFERENCE: Sullo stream e' presente l'handle all'oggetto gia' presente in memoria
   */
  static final byte INSTANCE = 1;
  static final byte REFERENCE = 2;

  /**
   *  Valori per CLASS_TYPE:
   *  NEWCLASS: Nuovo tipo da memorizzare, segue classname e suid
   *  REFCLASS: Riferimento ad un tipo gia' incontrato. Segue solo lo suid
   */
  static final byte NEWCLASS = 1;
  static final byte REFCLASS = 2;

  /**
   * Dimensione degli header dei dati serializzati
   */
  public static final int SIZE_OF_OBJECT_HEADER = SizeOf.BYTE + SizeOf.INT + SizeOf.BYTE + SizeOf.LONG;
  public static final int SIZE_OF_NULL = SizeOf.BYTE + SizeOf.INT;
  public static final int SIZE_OF_ARRAY_HEADER = SIZE_OF_OBJECT_HEADER + SizeOf.INT; // + length
  public static final int SIZE_OF_STRING_HEADER = SizeOf.BYTE + SizeOf.INT; // INSTANCE/REFERENCE + handle

  /**
   * Frame che rappresenta la serializzazione dell'oggetto.
   */
  private Frame frame;

  /**
   * Dimensione effettiva del container sottostante
   */
  protected int size;

  public Container( int ini_size )
  {
    frame = new Frame( ini_size );
    setSize( ini_size );
  }

  public Container( Frame ini_frame )
  {
    frame = ini_frame;
    setSize( ini_frame.getSize() );
  }

  /**
   *   Ottiene il frame sottostante
  public Frame getFrame()
  {
    return frame;
  }
   */

  public void setFrame( Frame _frame )
  {
    frame = _frame;
    setSize( frame.getSize() );
  }

  /**
   * ByteFrame sottostante
   */
  public byte[] getByteFrame( )
  {
    return frame.toArray();
  }

  /**
   * Dimensione del Frame in base all'ultima scrittura.
   * La dimensione iniziale del frame e' sempre sovradimensionata.
   * Questo metodo ci permette di conoscere la dimensione effettiva.
   */
  public final int getSize()
  {
    return size;
  }

  public final void setSize( int _size )
  {
    size = _size;
  }

  public final int getCapacity()
  {
    return frame.getSize();
  }

  public String toString()
  {
    return frame.toString();
  }

  /**
   * Libera la risorsa
   */
  public final void free()
  {
    frame.free();
    frame = null;
  }

  /**
   * Ottiene un nuovo stream dal frame
   */
  public ContainerInputStream getContainerInputStream( boolean have_objects )
  {
    return new ContainerInputStream( have_objects );
  }

  /**
   * ContainerInputStream
   * @description Reppresenta un flusso di input dal frame
   */

  public class ContainerInputStream
  {
    /**
     * Prossima posizione su cui operare
     */
    private int next_pos = 0;

    /**
     * handle sullo stream --> oggetto (riferimento in memoria)
     */
    private HandleTable handle_table;

    /**
     * SUIDTable
     * SUID sullo stream -> Classe
     */
    private SUIDTable suid_table;

    /**
     * @param have_object indica se verranno serializzati oggetti: Istanze di classi, Array, Stringhe
     */
    public ContainerInputStream( boolean have_objects )
    {
      if (have_objects)
      {
	  handle_table = new HandleTable();
	  suid_table = new SUIDTable();
      }

      next_pos = 0;
    }

    public void reset( boolean have_objects )
    {
      next_pos = 0;

      if (have_objects)
      {
	if (handle_table!=null)
	  handle_table.reset();
	else
	  handle_table = new HandleTable();

	if (suid_table!=null)
	  suid_table.reset();
	else
	  suid_table = new SUIDTable();
      } // end have_objects
      else
      {
	if (handle_table!=null)
	{
	  handle_table.reset();
	  handle_table = null;
	}
	if (suid_table!=null)
	{
	  suid_table.reset();
	  suid_table = null;
	}
      } // end !have_objects

    } // end reset()

    /**
     * Chiusura della connessione
     */
    public final int close()
    {
      int pos = next_pos;
      reset(true);
      return pos;
    }

    public void free( )
    {
      if (handle_table!=null)
	handle_table.reset();
      handle_table = null;

      if (suid_table!=null)
	suid_table.reset();
      suid_table = null;
    }

    public final int getPos()
    {
      return next_pos;
    }

    /**
     * Valori per OBJECT_TYPE:
     *  1)  Per i tipi primitivi: int, short, byte, ...
     *      non e' presente OBJECT_TYPE.
     *  2) Per i tipi non primitivi Oggetti, Stringhe, Arrays
     *     abbiamo che puo' valere:
     *    INSTANCE: Sullo stream e' presente l'istanza dell'oggetto
     *    REFERENCE: Sullo stream e' presente l'handle all'oggetto gia' presente in memoria
     */
    private final byte readObjectType( )
    {
      return readByte();
    }

    /**
     * Valori per CLASS_TYPE:
     *  1) NEWCLASS: Nuovo tipo da memorizzare, segue classname e suid
     *  2) REFCLASS: Riferimento ad un tipo gia' incontrato. Segue solo lo suid
     */
    private final byte readClassType()
    {
      return readByte();
    }

    /**
     * SUID che identifica univocamente una classe
     */
    final long readClassSUID()
    {
      return readLong();
    }

    /**
     * Handle che identifica univocamente un oggetto all'interno dello stream
     */
    private final int readHandle()
    {
      return readInt();
    }

    /**
     * Il boolean e' memorizzato con perdita di spazio come un byte (0x00/0x01)
     */
    public final boolean readBoolean()
    {
	return frame.get(next_pos++) > 0;
    }

// Lettura dei tipi primitivi.
    public final byte readByte()
    {
      return (byte) frame.get(next_pos++);
    }

    // Lettura dei tipi primitivi.
    public final short readUByte()
    {
      return frame.get(next_pos++);
    }

    public final char readChar()
    {
      char res = (char) frame.get(next_pos);
      res <<= 8;
      res += frame.get(next_pos+1);
      next_pos += SizeOf.CHAR;
      return res;
    }

    final public short readShort()
    {
      short res = (short) frame.get(next_pos);
      res <<=8;
      res += frame.get(next_pos+1);
      next_pos += SizeOf.SHORT;
      return res;
    }

    final public int readInt( )
    {
      int res = (int) frame.get(next_pos);
      res <<= 8;
      res += frame.get(next_pos+1);
      res <<= 8;
      res += frame.get(next_pos+2);
      res <<= 8;
      res += frame.get(next_pos+3);

      next_pos += SizeOf.INT;

      return res;
    }

    final public long readLong()
    {
      long res = (long) frame.get(next_pos);
      res <<= 8;
      res += frame.get(next_pos+1);
      res <<= 8;
      res += frame.get(next_pos+2);
      res <<= 8;
      res += frame.get(next_pos+3);
      res <<= 8;
      res += frame.get(next_pos+4);
      res <<= 8;
      res += frame.get(next_pos+5);
      res <<= 8;
      res += frame.get(next_pos+6);
      res <<= 8;
      res += frame.get(next_pos+7);

      next_pos += SizeOf.LONG;

      return res;
    }

    public final float readFloat()
    {
      float res = (float) Float.intBitsToFloat(((frame.get(next_pos + 0) & 0xFF) << 24) +
				    ((frame.get(next_pos + 1) & 0xFF) << 16) +
				    ((frame.get(next_pos + 2) & 0xFF) << 8) +
				    ((frame.get(next_pos + 3) & 0xFF) << 0));
      next_pos += SizeOf.FLOAT;
      return res;
    }


    final public double readDouble()
    {
      double res = Double.longBitsToDouble(((frame.get(next_pos + 0) & 0xFFL) << 56) +
				       ((frame.get(next_pos + 1) & 0xFFL) << 48) +
				       ((frame.get(next_pos + 2) & 0xFFL) << 40) +
				       ((frame.get(next_pos + 3) & 0xFFL) << 32) +
				       ((frame.get(next_pos + 4) & 0xFFL) << 24) +
				       ((frame.get(next_pos + 5) & 0xFFL) << 16) +
				       ((frame.get(next_pos + 6) & 0xFFL) << 8) +
				       ((frame.get(next_pos + 7) & 0xFFL) << 0));
      next_pos += SizeOf.DOUBLE;
      return res;
    }

    /**
     * Legge una stringa dal frame
     * Nuova Stringa: INSTANCE handle stringa_utf8
     * Rif. Stringa REFERENCE handle
     */
    final public String readString() throws SerializationException
    {
      byte object_type = readObjectType();
      int handle = readHandle();

      switch (object_type)
      {
	case INSTANCE:
	  String str_ref;
	  str_ref = readStringInternal();

	  int local_handle = handle_table.put( str_ref );
	  assert( local_handle == handle );
	  return str_ref;
	case REFERENCE:
	  return (String) handle_table.get( handle );
	default:
	  throw new SerializationException( "readString(): Errore nel formato dello stream di lettura" );
      }
    }

    /**
     * Legge una stringa in formato UTF8
     */
    public final String readStringUTF8() throws SerializationException
    {
      return readStringInternal();
    }

    /**
     * codice legacy di DataInputStream per leggere una stringa in formato UTF8
     */

    private final String readStringInternal() throws SerializationException
    {
      int utflen = readShort();
      FastStringBuffer str = new FastStringBuffer(utflen);

      int c, char2, char3;
      int count = 0;

      while (count < utflen) {
	  c = (int) readByte() & 0xff;
	  switch (c >> 4) {
	      case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
		  /* 0xxxxxxx*/
		  ++count;
		  str.append((char)c);
		  break;
	      case 12: case 13:
		  /* 110x xxxx   10xx xxxx*/
		  count+=2;
		  if (count > utflen)
		      throw new SerializationException("readUTF(): string too long");

		  char2 = (int) readByte();
		  if ((char2 & 0xC0) != 0x80)
		      throw new SerializationException("readUTF(): Data Format");
		  str.append((char)(((c & 0x1F) << 6) | (char2 & 0x3F)));
		  break;
	      case 14:
		  /* 1110 xxxx  10xx xxxx  10xx xxxx */
		  count += 3;
		  if (count > utflen)
		      throw new SerializationException( "readUTF():String too long" );
		  char2 = (int) readByte();
		  char3 = (int) readByte();
		  if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
		      throw new SerializationException("readUTF(): Data Format");
		  str.append((char)(((c     & 0x0F) << 12) |
				    ((char2 & 0x3F) << 6)  |
				    ((char3 & 0x3F) << 0)));
		  break;
	      default:
		  /* 10xx xxxx,  1111 xxxx */
		  throw new SerializationException("readUTF(): Data Format");
	      }
      }
      // The number of chars produced may be less than utflen

      String result = str.toString();
//      next_pos += result.length();
      return result;
    }

    /**
     * Legge e restituisce la class di un oggetto sullo stream
     * Il "suggerimento" del tipo e' fatto o da CLIO, per gli oggetti
     * o dalla routine per gli array.
     * suggested_type: Tipo suggerito.
     * suggested_SUID: SUID suggerito.
     *
     * Formato stream:
     * CLASS_TYPE SUID (class_name)
     */
    final protected Class readClass(  ) throws SerializationException
    {
      Class remote_class;
      String class_name;

      byte class_type = readClassType();
      long class_SUID = readClassSUID();

      try
      {
	switch (class_type)
	{
	  case REFCLASS:
	    return suid_table.get( class_SUID );
	  case NEWCLASS:
	      class_name = readStringInternal();
	      remote_class = Class.forName( class_name );

	      // In questo caso il controllo dei due SUID va fatto
	      suid_table.put( class_SUID, remote_class );
	      return remote_class;
	  default:
	    throw new SerializationException( "Errore nel formato dello stream di lettura" );
	} // end switch
      }
      catch ( ClassNotFoundException cnfe ) { throw new SerializationException( cnfe.getMessage() ); }
      catch (Exception reflect_exception) { throw new SerializationException( reflect_exception.getMessage() ); }
    } // end readClass()

    /**
     * Legge un oggetto FastTransportable dal frame e lo deseriarilizza
     * Nuovo Oggetto: INSTANCE handle NEWCLASS SUID classname Istanza
     *                INSTANCE handle REFCLASS SUID Istanza
     * Riferimento ad un oggetto: REFERENCE handle
     * @return Oggetto letto. Non e' detto che sia FastTransportable, per via della classi legacy: String, Array
     */

    public FastTransportable readObject( ) throws SerializationException
    {
      byte op = readObjectType();
      int handle = readHandle();

      switch (op)
      {
	case REFERENCE:
	  // cerca il ref nell'hashtable
//	  return (Transportable) (handle!=0 ? handle_table.get( handle ) : null);
	  return (FastTransportable) handle_table.get( handle );
	case INSTANCE:
	    Class local_class;
	    local_class = readClass();

	    // istanza inizializzato con il costruttore di default
	    FastTransportable instance;

	    try { instance = (FastTransportable) local_class.newInstance(); }
	    catch (IllegalAccessException iae )
	    {
	      log.info( iae.toString() );
	      throw new SerializationException( iae.getMessage() );
	    }
	    catch (InstantiationException ie )
	    {
	      log.info( ie.toString() );
	      throw new SerializationException( ie.getMessage() );
	    }

	  // si memorizza l'handle prima di deserializzare l'oggetto
	  // in modo da lasciare sincronizzato lo stream
	  int local_handle = handle_table.put( instance );

	  /*
	   * Si usa l'overriding per chiamare il metodo virtuale per leggere l'istanza dallo stream.
	   * Ogni oggetto sa come leggere i suoi campi
	   * La readObject e' aggiunta da CLIO
	  */
	  instance.readObject( this );

/**
 * Marshall -> readObject -> int=readInt, float=readFloat, oggetto=Container.readObject
*/
	  assert(local_handle==handle);
	  return instance;
	default:
	  throw new SerializationException( "Errore nel formato dello stream di lettura" );
      }

    } // end readObject

    /**
     * Lettura di array primitivi
     * Evitiamo di usare la reflection (method.invoke) per motivi di prestazione
     * Nuovo array di tipi primitivi: INSTANCE (int)handle (int)len dati
     * Rif array di tipi primitivi: REFERENCE (int)handle
     */
    private final boolean[] readBooleanArrayInternal( boolean[] ba )
    {
      /* Come memorizzare un boolean?
       * per adesso lo convertiamo a byte con perdita di 7 bits
       */
      for ( int i=0; i<ba.length; ++i )
	ba[i] = readBoolean();

      return ba;
    }

    private final byte[] readByteArrayInternal(byte[] ba )
    {
      for ( int i=0; i<ba.length; ++i )
	ba[i] = readByte();
      return ba;
    }


    private final char[] readCharArrayInternal(char[] ca )
    {
      for ( int i=0; i<ca.length; ++i )
	ca[i] = readChar();

      return ca;
    }


    private final short[] readShortArrayInternal(short[] sa)
    {
      for ( int i=0; i<sa.length; ++i )
	sa[i] = readShort();

      return sa;
    }


    private final int[] readIntArrayInternal( int[] ia )
    {
      for ( int i=0; i<ia.length; ++i )
	ia[i] = readInt();

      return ia;
    }


    private final long[] readLongArrayInternal( long[] la)
    {
      for ( int i=0; i<la.length; ++i )
	la[i] = readLong();

      return la;
    }


    private final float[] readFloatArrayInternal( float[] fa )
    {
      for ( int i=0; i<fa.length; ++i )
	fa[i] = readFloat();

      return fa;
    }


    private final double[] readDoubleArrayInternal( double[] da )
    {
      for ( int i=0; i<da.length; ++i )
	da[i] = readDouble();

      return da;
    }

    private final String[] readStringArrayInternal( String[] sa ) throws SerializationException
    {
      for ( int i=0; i<sa.length; ++i )
	sa[i] = readString();

      return sa;
    }

    private final FastTransportable[] readObjectArrayInternal( FastTransportable[] fa ) throws SerializationException
    {
      for ( int i=0; i<fa.length; ++i )
	fa[i] = readObject();

      return fa;
    }

    /**
     * Legge un array di oggetti FastTransportable
     * il casting se ne occupa il chiamante
     * Nuovo array di oggetti: INSTANCE handle NEWCLASS SUID classname (int)len [Nuovo Oggetto|Riferimento]
     *                         INSTANCE handle REFCLASS SUID (int)len [Nuovo Oggetto|Riferimento]
     * Rif. array di oggetti: REFERENCE handle
     *
     * con:
     * Nuovo Oggetto: INSTANCE handle NEWCLASS SUID classname Istanza
     *                INSTANCE handle REFCLASS SUID Istanza
     * Riferimento ad un oggetto: REFERENCE handle
     */
    public final Object readArray( ) throws SerializationException
    {
      byte object_type = readObjectType();
      int handle = readHandle();

      switch (object_type)
      {
	case REFERENCE:
	  return handle_table.get(handle);
	case INSTANCE:
	  Class array_type = readClass( );
	  Class component_type = array_type.getComponentType();

	  int len = readInt();
	  Object oa = Array.newInstance( component_type, len );
	  int local_handle = handle_table.put( oa );
	  assert( local_handle == handle );

	  if (component_type.isArray())
	  {
	    Object[] matrix = (Object[]) oa;
	    for ( int i=0; i<len; ++i )
	      matrix[i] = readArray( );
	    return matrix;
	  }
	  else
	  {
	    return readArrayInternal(oa);
	  }
	default:
	  throw new SerializationException( "Errore nel formato dello stream di lettura" );
      }

    }

    private final Object readArrayInternal( Object row ) throws SerializationException
    {
      Class component_type = row.getClass().getComponentType();

      if (component_type.isPrimitive())
      {
	if (component_type==boolean.class)
	{
	  return readBooleanArrayInternal((boolean[]) row);
	}
	else if (component_type==byte.class)
	{
	  return readByteArrayInternal((byte[]) row);
	}
	else if (component_type==short.class)
	{
	  return readShortArrayInternal((short[]) row);
	}
	else if (component_type==char.class)
	{
	  return readCharArrayInternal((char[]) row);
	}
	else if (component_type==int.class)
	{
	  return readIntArrayInternal((int[]) row);
	}
	else if (component_type==long.class)
	{
	  return readLongArrayInternal((long[]) row);
	}
	else if (component_type==float.class)
	{
	  return readFloatArrayInternal((float[]) row);
	}
	else if (component_type==double.class)
	{
	  return readDoubleArrayInternal((double[]) row);
	}
	else
	  return null;
      }
      else if (component_type==String.class)
      {
	return readStringArrayInternal((String[]) row);
      }
      else
	return readObjectArrayInternal((FastTransportable[]) row);
    } // end writeMatrixPrimitivesInternal()

  } // end class ContainerInputStream

  /**
   * Ottiene un nuovo stream dal frame
   */

  public ContainerOutputStream getContainerOutputStream( boolean have_objects )
  {
    return new ContainerOutputStream( have_objects );
  }


  /**
   * Classe che rappresenta uno stream di scrittura dal Container
   */
  public class ContainerOutputStream
  {
    /**
     * Prossima posizione su cui operare
     */
    private int next_pos = 0;

    /**
     * Insieme delle classi gia' serializzate
     */
    private ClassTable class_table;

    /**
     * Tavola degli oggetti gia' serializzati.
     */
    private ReferenceTable reference_table;

    public ContainerOutputStream( boolean have_objects )
    {
      next_pos = 0;
      reference_table = new ReferenceTable();
      class_table = new ClassTable();
    }

    public void reset( boolean have_objects )
    {
      next_pos = 0;
      if (have_objects)
      {
	if (reference_table!=null)
	  reference_table.reset();
	else
	  reference_table = new ReferenceTable();

	if (class_table!=null)
	  class_table.reset();
	else
	  class_table = new ClassTable();
      } // end have_objects
      else
      {
	if (reference_table!=null)
	{
	  reference_table.reset();
	  reference_table = null;
	}
	if (class_table!=null)
	{
	  class_table.reset();
	  class_table = null;
	}
      } // end !have_objects
    } // end reset()

    /**
     * Rimuove le tabelle per la serializzazione
     */
    public void free( )
    {
      if (reference_table!=null)
	reference_table.reset();
      reference_table = null;

      if (class_table!=null)
	class_table.reset();
      class_table = null;
    } // end free()

    /**
     * Chiude il COStream:
     * 1. Effettua il free() dello stream
     * 2. Memorizza la posizione dell'ultimo accesso (next_pos) in byte_frame_size
     */
    public int close()
    {
      setSize( getPos() );
      reset(true);
      return getSize();
    }

    /**
     * Posizione della testina R/W
     */
    public final int getPos()
    {
      return next_pos;
    }
    /**
     * Valori per OBJECT_TYPE:
     *  1)  Per i tipi primitivi: int, short, byte, ...
     *      non e' presente OBJECT_TYPE.
     *  2) Per i tipi non primitivi Oggetti, Stringhe, Arrays
     *     abbiamo che puo' valere:
     *    INSTANCE: Sullo stream e' presente l'istanza dell'oggetto
     *    REFERENCE: Sullo stream e' presente l'handle all'oggetto gia' presente in memoria
     */
    private final void writeObjectType( byte object_type )
    {
      assert( object_type==INSTANCE || object_type==REFERENCE );
      writeByte( object_type );
    }

    /**
     * Valori per CLASS_TYPE:
     *  1) NEWCLASS: Nuovo tipo da memorizzare, segue classname e suid
     *  2) REFCLASS: Riferimento ad un tipo gia' incontrato. Segue solo lo suid
     */
    final void writeClassType( byte class_type )
    {
      assert( class_type==NEWCLASS || class_type==REFCLASS );
      writeByte( class_type );
    }

    /**
     * SUID che identifica univocamente una classe
     */
    private final void writeClassSUID( long SUID )
    {
      writeLong( SUID );
    }

    /**
     * Handle che identifica univocamente un oggetto all'interno dello stream
     */
    private final void writeHandle( int handle )
    {
      writeInt( handle );
    }

    /**
     * Il boolean e' memorizzato con perdita di spazio come un byte (0x00/0x01)
     */
    public final void writeBoolean( boolean b )
    {
      frame.put( next_pos++, (byte) (b ? 1 : 0) );
    }

// Lettura dei tipi primitivi.
    public final void writeByte( byte b )
    {
      frame.put( next_pos++, b );
    }

    public final void writeChar( char c )
    {
      byte b;
      b = (byte) (c>>>8 & 255);
      frame.put( next_pos, b );
      b = (byte) (c & 255);
      frame.put( next_pos+1, b );
      next_pos += SizeOf.CHAR;
    }

    final public void writeShort( short s )
    {
      byte b;
      b = (byte) (s>>>8 & 255);
      frame.put( next_pos, b );
      b = (byte) (s & 255);
      frame.put( next_pos+1, b );
      next_pos += SizeOf.SHORT;
    }

    final public void writeInt( int i )
    {
      byte b;
      b = (byte) (i>>>24 & 255);
      frame.put( next_pos, b );
      b = (byte) (i>>>16 & 255);
      frame.put( next_pos+1, b );
      b = (byte) (i>>>8 & 255);
      frame.put( next_pos+2, b );
      b = (byte) (i & 255);
      frame.put( next_pos+3, b );

      next_pos += SizeOf.INT;
    }

    final public void writeLong( long l )
    {
      byte b;

      b = (byte) (l&255);
      frame.put( next_pos+7, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+6, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+5, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+4, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+3, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+2, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+1, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos, b );

      next_pos += SizeOf.LONG;
    }

    public final void writeFloat( float f )
    {
      int i = Float.floatToIntBits(f);
      byte b;

      b = (byte) (i>>>24 & 255);
      frame.put( next_pos, b );
      b = (byte) (i>>>16 & 255);
      frame.put( next_pos+1, b );
      b = (byte) (i>>>8 & 255);
      frame.put( next_pos+2, b );
      b = (byte) (i & 255);
      frame.put( next_pos+3, b );

      next_pos += SizeOf.FLOAT;
    }


    final public void writeDouble( double d)
    {
      long l = Double.doubleToLongBits(d);
      byte b;

      b = (byte) (l&255);
      frame.put( next_pos+7, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+6, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+5, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+4, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+3, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+2, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos+1, b );
      l >>>= 8;
      b = (byte) (l&255);
      frame.put( next_pos, b );

      next_pos += SizeOf.DOUBLE;
    }

    /**
     * Scrive un oggetto stringa sul frame
     * Nuova Stringa: INSTANCE handle stringa_utf8
     * Rif. Stringa REFERENCE handle
     */
    final public void writeString( String str ) throws SerializationException
    {
      int handle = reference_table.get( str );

      if (handle==0)
      {
	// nuova istanza
	handle = reference_table.put( str );
	writeObjectType(INSTANCE);
	writeHandle(handle);

	writeStringInternal( str );
      }
      else
      {
	// riferimento
	writeObjectType(REFERENCE);
	writeHandle(handle);
      }
    } // end writeString()

    /**
     * Scrive una stringa sullo stream in formato UTF8
     */
    final public void writeStringUTF8( String str ) throws SerializationException
    {
      writeStringInternal( str );
    }

    /**
     * codice legacy di DataOutputStream per scrivere una stringa in formato UTF8
     */
    private final int writeStringInternal( String str ) throws SerializationException // throws IOException
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

	if (utflen > 65535)
	    throw new SerializationException("writeUTF(): String too long");
//	byte[] bytearr = new byte[utflen+2];

	writeByte( (byte) ((utflen >>> 8) & 0xFF) );
	writeByte( (byte) ((utflen >>> 0) & 0xFF) );

	for (int i = 0; i < strlen; i++) {
	    c = charr[i];
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		writeByte( (byte) c );
	    } else if (c > 0x07FF) {
		writeByte( (byte) (0xE0 | ((c >> 12) & 0x0F)) );
		writeByte( (byte) (0x80 | ((c >>  6) & 0x3F)) );
		writeByte( (byte) (0x80 | ((c >>  0) & 0x3F)) );
	    } else {
		writeByte( (byte) (0xC0 | ((c >>  6) & 0x1F)) );
		writeByte( (byte) (0x80 | ((c >>  0) & 0x3F)) );
	    }
	}

//	next_pos += utflen + 2;
	return utflen + 2;
    }

    /**
     * Scrive il nome della classe di un oggetto sullo stream
     * Formato stream:
     * CLASS_TYPE SUID (class_name)
     *
     * @param type tipo da serializzare
     */
    private final void writeClass( Class type ) throws SerializationException
    {
      long SUID = 0;

      SUID = clio.SUID.getSUID( type );
/*
      if (!type.isArray())
      {
	try { SUID = type.getField( "SUID" ).getLong( null ); }
	catch (Exception reflect_exception) { throw new SerializationException( reflect_exception.getMessage() ); }
      }
      else
      {
	SUID = clio.SUID.computeSUID( type );
//	SUID = ObjectStreamClass.lookup(type).getSerialVersionUID();
      }
*/
      if ( ! class_table.has( type ) )
      {
	// NEWCLASS
	class_table.put( type );
	writeClassType( NEWCLASS );
	writeClassSUID( SUID );
	writeStringInternal( type.getName() );
      }
      else
      {
	// REFCLASS
	writeClassType( REFCLASS );
	writeClassSUID( SUID );
      }
    }

    /**
     * Scrive un oggetto FastTransportable sul frame serializzandolo
     * Nuovo Oggetto: INSTANCE handle NEWCLASS SUID classname Istanza
     *                INSTANCE handle REFCLASS SUID Istanza
     * Riferimento ad un oggetto: REFERENCE handle
     */

    public void writeObject( FastTransportable t ) throws SerializationException
    {
      int handle = 0;
      if (t!=null && (0 == (handle = reference_table.get( t )) ) )
      {
	// INSTANCE
	handle = reference_table.put( t );
	writeObjectType(INSTANCE);
	writeHandle(handle);
	writeClass( t.getClass() );
	// La writeObjectInternal ce la fornisce la classe stessa
	// non c'e' bisogno dell'introspezione
	t.writeObject( this );
      }
      else
      {
	// in caso di Reference ad un oggetto o Reference nullo
	// REFERENCE
	writeObjectType(REFERENCE);
	writeHandle(handle);
      }

    } // end writeObject()

    /**
     * Scrittura di array primitivi
     * Evitiamo di usare la reflection (method.invoke) per motivi di prestazione
     * Nuovo array di tipi primitivi: INSTANCE (int)handle (int)len dati
     * Rif array di tipi primitivi: REFERENCE (int)handle
     */

    private final void writeBooleanArrayInternal( boolean[] ba )
    {
      for ( int i=0; i<ba.length; ++i )
	writeBoolean(ba[i]);
    }

    private final void writeByteArrayInternal( byte[] ba )
    {
      for ( int i=0; i<ba.length; ++i )
	writeByte(ba[i]);
    }

    private final void writeCharArrayInternal( char[] ca )
    {
      for ( int i=0; i<ca.length; ++i )
	writeChar( ca[i] );
    }

    private final void writeShortArrayInternal( short[] sa )
    {
      for ( int i=0; i<sa.length; ++i )
	writeShort( sa[i] );
    }

    private final void writeIntArrayInternal( int[] ia )
    {
      for ( int i=0; i<ia.length; ++i )
	writeInt( ia[i] );
    }

    private final void writeLongArrayInternal( long[] la )
    {
      for ( int i=0; i<la.length; ++i )
	writeLong( la[i] );
    }


    private final void writeFloatArrayInternal( float[] fa )
    {
      for ( int i=0; i<fa.length; ++i )
	writeFloat( fa[i] );
    }


    private final void writeDoubleArrayInternal( double[] da )
    {
      for ( int i=0; i<da.length; ++i )
	writeDouble( da[i] );
    }


    private final void writeStringArrayInternal( String[] sa ) throws SerializationException
    {
      for ( int i=0; i<sa.length; ++i )
	writeString( sa[i] );
    }

    private final void writeObjectArrayInternal( FastTransportable[] fa ) throws SerializationException
    {
      for ( int i=0; i<fa.length; ++i )
	writeObject( fa[i] );
    }

    /**
     * Scrive un array di oggetti FastTransportable
     * Nuovo array di oggetti: INSTANCE handle NEWCLASS SUID classname (int)len [Nuovo Oggetto|Riferimento]
     *                         INSTANCE handle REFCLASS SUID (int)len [Nuovo Oggetto|Riferimento]
     * Rif. array di oggetti: REFERENCE handle
     *
     * con:
     * Nuovo Oggetto: INSTANCE handle NEWCLASS SUID classname Istanza
     *                INSTANCE handle REFCLASS SUID Istanza
     * Riferimento ad un oggetto: REFERENCE handle
     *
     * - Nuova matrice di tipi primitivi: INSTANCE (int)handle (int)dimensione Matrice0 Matrice1
     *   Rif matrice di tipi primitivi: REFERENCE (int)handle
     * - Nuova matrice di oggetti: INSTANCE handle NEWCLASS SUID (utf8)classname (int)dim (Matrice di dimensione-1) (Matrice di dimensione-1) ...
     *                             INSTANCE handle REFCLASS SUI (int)dim (Matrice di dimensione-1) (Matrice di dimensione-1) ...
     * - Rif. matrice di oggetti: REFERENCE handle
     * - Nuova Stringa (StringObject): INSTANCE handle (utf8)stringa
     *   Rif. Stringa (StringObject): REFERENCE handle
     *
     */

    public final void writeArray( Object array ) throws SerializationException
    {
      int handle=0;
      if (array!=null && ((handle = reference_table.get( array ))==0) )
      {
	// INSTANCE
	Class array_type = array.getClass();
	handle = reference_table.put( array );
	writeObjectType(INSTANCE);
	writeHandle(handle);
	writeClass( array_type );
	int len = Array.getLength(array);
	writeInt( len );

	Class component_type = array_type.getComponentType();
	if (component_type.isArray() )
	{
	  Object[] matrix = (Object[]) array;
	  for ( int i=0; i<matrix.length; i++ )
	    writeArray( matrix[i] );
	}
	else
	{
	  writeArrayInternal(array);
	}
      }
      else
      {
	// REFERENCE
	writeObjectType(REFERENCE);
	writeHandle(handle);
      }
    }

    private final void writeArrayInternal( Object row ) throws SerializationException
    {
      Class component_type = row.getClass().getComponentType();

      if (component_type.isPrimitive())
      {
	if (component_type==boolean.class)
	{
	  boolean[] za = (boolean[]) row;
	  writeBooleanArrayInternal(za);
	}
	else if (component_type==byte.class)
	{
	  byte[] ba = (byte[]) row;
	  writeByteArrayInternal( ba );
	}
	else if (component_type==short.class)
	{
	  short[] sa = (short[]) row;
	  writeShortArrayInternal( sa );
	}
	else if (component_type==char.class)
	{
	  char[] ca = (char[]) row;
	  writeCharArrayInternal(ca);
	}
	else if (component_type==int.class)
	{
	  int[] ia = (int[]) row;
	  writeIntArrayInternal( ia );
	}
	else if (component_type==long.class)
	{
	  long[] la = (long[]) row;
	  writeLongArrayInternal( la );
	}
	else if (component_type==float.class)
	{
	  float[] fa = (float[]) row;
	  writeFloatArrayInternal( fa );
	}
	else if (component_type==double.class)
	{
	  double[] da = (double[]) row;
	  writeDoubleArrayInternal( da );
	}
      }
      else if (component_type==String.class)
      {
	String[] ua = (String[]) row;
	writeStringArrayInternal(ua);
      }
      else
      {
	FastTransportable[] ft = (FastTransportable[]) row;
	writeObjectArrayInternal( ft );
      }
      return;
    } // end writeArrayInternal()
  } // end class ContainerOutputStream


  public static void main( String[] args ) throws SerializationException, ClassNotFoundException
  {
    Container c = new Container( 12384 );
    boolean res = c.selftest();
    log.info( "Selftest " + c.getClass().getName() + ":<"+ res +">");
  }


  private boolean selftest() throws SerializationException, ClassNotFoundException
  {
    boolean res = selftest_prim();
    res &= selftest_array();
    res &= selftest_string();
    res &= selftest_object();
    res &= selftest_object_array();
    res &= selftest_object_list();
    res &= selftest_matrix();
    res &= selftest_object_matrix();
    return res;
  }

  private boolean selftest_prim() throws SerializationException
  {
    log.info( "selftest_prim" );
    ContainerOutputStream cos = getContainerOutputStream(true);

    boolean pre_boolean = true;
    cos.writeBoolean( pre_boolean );
    byte pre_byte = 73;
    cos.writeByte( pre_byte );
    char pre_char = 'X';
    cos.writeChar(pre_char);
    short pre_short = 1973;
    cos.writeShort( pre_short );
    int pre_int = 3111973;
    cos.writeInt( pre_int );
    long pre_long = 31119731;
    cos.writeLong( pre_long );
    float pre_float = 1973.31f;
    cos.writeFloat( pre_float );
    double pre_double = 19731973.1973;
    cos.writeDouble( pre_double );

    int sizeof_data = SizeOf.BOOLEAN + SizeOf.BYTE + SizeOf.CHAR + SizeOf.SHORT + SizeOf.INT + SizeOf.LONG + SizeOf.FLOAT + SizeOf.DOUBLE;

    assert( sizeof_data == cos.getPos() );

    cos.close();
    int pre_data_size = getSize();
    assert( sizeof_data == pre_data_size );
    log.info( "sizeof_data: " + sizeof_data  + "/pre_data_size: " + getSize() );

    ContainerInputStream cis = getContainerInputStream(true);

    boolean post_boolean = cis.readBoolean();
    assert(pre_boolean == post_boolean );
    byte post_byte = cis.readByte();
    assert(pre_byte == post_byte );
    char post_char = cis.readChar();
    assert( pre_char == post_char );
    short post_short = cis.readShort();
    assert( pre_short == post_short );
    int post_int = cis.readInt();
    assert(pre_int == post_int );
    long post_long = cis.readLong();
    assert( pre_long == post_long );
    float post_float = cis.readFloat();
    assert(pre_float == post_float );
    double post_double = cis.readDouble();
    assert(pre_double == post_double );

    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );

    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size );
    log.info( "" );

    return true;
  }

  private boolean selftest_array() throws SerializationException
  {
    log.info( "selftest_array" );
    int sizeof_data = 0;

    ContainerOutputStream cos = getContainerOutputStream(true);

    boolean[] pre_boolean_a = new boolean[] { true, false, true, false, true, true,true,true,true, false };
    cos.writeArray( pre_boolean_a );
    sizeof_data += SizeOf.array(pre_boolean_a);
    byte[] pre_byte_a = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 127, (byte) 128, (byte) 255, (byte) 164, -1, -2, -128 };
    cos.writeArray( pre_byte_a );
    sizeof_data += SizeOf.array(pre_byte_a);
    short[] pre_short_a = new short[] { 0, -1, 1973, -1973, -32768, 32767, (short) 40192 };
    cos.writeArray( pre_short_a );
    sizeof_data += SizeOf.array(pre_short_a);
    int[] pre_int_a = new int[] { 0, 255, 32768, 65536, 20000000, -1, -2, 128000 };
    cos.writeArray( pre_int_a );
    sizeof_data += SizeOf.array(pre_int_a);
    long[] pre_long_a = new long[] { 0, 255, 32768, -1, -2, 12345678, -255 };
    cos.writeArray( pre_long_a );
    sizeof_data += SizeOf.array(pre_long_a);
    float[] pre_float_a = new float[] { 1973.31f, -.1f, .2f, 10e-2f };
    cos.writeArray( pre_float_a );
    sizeof_data += SizeOf.array(pre_float_a);
    double[] pre_double_a = new double[] { 19731973.1973, -22.1234, -.214567 };
    cos.writeArray( pre_double_a );
    sizeof_data += SizeOf.array(pre_double_a);

    cos.close();

    int pre_data_size = getSize();
    assert( sizeof_data == pre_data_size );
    log.info( "sizeof_data: " + sizeof_data + "/pre_data_size: " + pre_data_size );

    ContainerInputStream cis = getContainerInputStream(true);

    boolean[] post_boolean_a = (boolean[]) cis.readArray( );
    assert( java.util.Arrays.equals(pre_boolean_a, post_boolean_a ) );
    byte[] post_byte_a = (byte[]) cis.readArray( );
    assert( java.util.Arrays.equals(pre_byte_a, post_byte_a ) );
    short[] post_short_a = (short[]) cis.readArray( );
    assert( java.util.Arrays.equals(pre_short_a, post_short_a ) );
    int[] post_int_a = (int[]) cis.readArray( );
    assert( java.util.Arrays.equals(pre_int_a, post_int_a ) );
    long[] post_long_a = (long[]) cis.readArray( );
    assert( java.util.Arrays.equals(pre_long_a, post_long_a ) );
    float[] post_float_a = (float[]) cis.readArray( );
    assert( java.util.Arrays.equals(pre_float_a, post_float_a ) );
    double[] post_double_a = (double[]) cis.readArray( );
    assert( java.util.Arrays.equals(pre_double_a, post_double_a ) );

    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );
    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size);
    log.info( "" );

    return true;
  }

  private boolean selftest_string() throws SerializationException
  {
    log.info( "selftest_string" );
    ContainerOutputStream cos = getContainerOutputStream(true);

    String pre_string_1 = "hi world";
    cos.writeStringUTF8(pre_string_1);
    String pre_string_2 = "Hello world";
    cos.writeString(pre_string_2);

    int sizeof_data = SizeOf.stringUTF8( pre_string_1 );
    sizeof_data += SizeOf.string( pre_string_2 );

    assert( sizeof_data == cos.getPos() );

/*
    String[] pre_string_a = new String[] { "hello", "world" };
    cos.writeStringArray( pre_string_a );
    sizeof_data += SizeOf.stringArray( pre_string_a );
*/

    cos.close();
    int pre_data_size = getSize();
    assert( sizeof_data == pre_data_size );
    log.info( "sizeof_data: " + sizeof_data + "/pre_data_size: " + pre_data_size );

    ContainerInputStream cis = getContainerInputStream(true);

    String post_string_1 = cis.readStringUTF8();
    assert( pre_string_1.equals(post_string_1) );
    String post_string_2 = cis.readString();
    assert( pre_string_2.equals( post_string_2) );

    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );
    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size);
    log.info( "" );

//    log.info( "" + post_string_1 );

    return true;
  }

  private boolean selftest_object() throws SerializationException
  {
    log.info( "selftest_object" );
    ContainerOutputStream cos = getContainerOutputStream(true);

    selftest_object pre_object = new selftest_object( 1, 2, new int[] { 3, 4 } );
    cos.writeObject( pre_object );

    int sizeof_data = SizeOf.object( pre_object );

    /*
    ** In genere la dimensione precalcolata e' una sovrastima di quella effettiva
    ** in quanto gli handle permettono di evitare la riscrittura di definizioni di classi
    ** In questo caso sono uguali in quanto serializziamo oggetti singoli.
    ** Nel caso in cui l'oggetto contenga riferimenti multipli allo stesso oggetto
    ** sostituire con <=
    */
    assert( sizeof_data == cos.getPos() );

    cos.close();

    int pre_data_size = getSize();
    assert( sizeof_data == pre_data_size );
    log.info( "sizeof_data: " + sizeof_data + "/pre_data_size: " + pre_data_size );

    ContainerInputStream cis = getContainerInputStream(true);

    selftest_object post_object = (selftest_object) cis.readObject();
    assert( pre_object.equals(post_object) );

    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );
    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size);
    log.info( "" );

    return true;
  }

  private boolean selftest_object_array() throws SerializationException
  {
    log.info( "selftest_object_array" );
    ContainerOutputStream cos = getContainerOutputStream(true);

    int[] ia = new int[] { 3, 4 };
    selftest_object object0 = new selftest_object( 1, 2, ia );
    selftest_object object1 = new selftest_object( 5, 6, new int[] { 7, 8, 9 } );
    selftest_object object2 = new selftest_object( 7, 8, ia );
    selftest_object object3 = new selftest_object( 7, 8, null );
    selftest_object[] pre_object_array = new selftest_object[] { object0, object1, object2, object1, object3 };

    cos.writeArray( pre_object_array );

    int sizeof_data = SizeOf.array( pre_object_array );

    cos.close();

    /*
    ** In questo caso sizeof_data e' una sovrastima, in quanto il protocollo di serializzazione
    ** riutilizza la definizione della classe base.
    */
    int pre_data_size = getSize();
    assert( sizeof_data >= pre_data_size );
    log.info( "sizeof_data: " + sizeof_data + "/pre_data_size: " + pre_data_size );

    ContainerInputStream cis = getContainerInputStream(true);

    selftest_object[] post_object_array = (selftest_object[]) cis.readArray();
    java.util.Arrays.equals( pre_object_array, post_object_array );
//    assert( pre_object_array[0].equals(post_object) );

    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );
    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size);
    log.info( "" );
    assert( post_object_array[0].ia == post_object_array[2].ia );
    return true;
  }

  private boolean selftest_object_list() throws SerializationException
  {
    log.info( "selftest_object_list" );
    ContainerOutputStream cos = getContainerOutputStream(true);

    int[] ia = new int[] { 3, 4 };
    selftest_object_elem object2 = new selftest_object_elem( null, 7, 8, ia );
    selftest_object_elem object1 = new selftest_object_elem( object2, 5, 6, new int[] { 7, 8, 9 } );
    selftest_object_elem object0 = new selftest_object_elem( object1, 1, 2, ia );
    selftest_object_elem pre_object = object0;
//    selftest_object_elem[] pre_object_array = new selftest_object_elem[] { object0, object1, object2, object1 };

//    cos.writeObjectArray( pre_object_array );
    cos.writeObject( pre_object );

//    int sizeof_data = SizeOf.objectArray( pre_object_array );
    int sizeof_data = SizeOf.object( pre_object );

    cos.close();

    /*
    ** In questo caso sizeof_data e' una sovrastima, in quanto il protocollo di serializzazione
    ** riutilizza la definizione della classe base.
    */
    int pre_data_size = getSize();
    assert( sizeof_data >= pre_data_size );
    log.info( "sizeof_data: " + sizeof_data + "/pre_data_size: " + pre_data_size );

    ContainerInputStream cis = getContainerInputStream(true);

//    selftest_object[] post_object_array = (selftest_object[]) cis.readObjectArray(selftest_object.class);
    selftest_object_elem post_object = (selftest_object_elem) cis.readObject();
//    java.util.Arrays.equals( pre_object_array, post_object_array );
//    assert( pre_object_array[0].equals(post_object) );
    assert( pre_object.equals( post_object ) );
    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );
    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size);
    log.info( "" );
//    assert( post_object_array[0].ia == post_object_array[2].ia );
    return true;
  }

  private boolean selftest_matrix() throws SerializationException, ClassNotFoundException
  {
    log.info( "selftest_matrix" );
    int sizeof_data = 0;

    ContainerOutputStream cos = getContainerOutputStream(true);

    boolean[][] pre_boolean_aa = new boolean[][] { { true, false, true, false, true, true,true,true,true, false }, { true,true, true, false, true, true, false, true,true, false } };
    cos.writeArray( pre_boolean_aa );
    sizeof_data += SizeOf.array(pre_boolean_aa);
    byte[][][] pre_byte_aaa = new byte[][][] { { {43,20} ,{1,2,3,4} }, { {1,2,3},{5,6,7,8,9,10} }, { {1},{1,2} } };
    cos.writeArray( pre_byte_aaa );
    sizeof_data += SizeOf.array(pre_byte_aaa);
    short[] pre_short_a = new short[] { 0, -1, 1973, -1973, -32768, 32767, (short) 40192 };
    cos.writeArray( pre_short_a );
    sizeof_data += SizeOf.array(pre_short_a);
    int[][][] pre_int_aaa = new int[][][] { { {21,2},{5,6,7,8} }, { {1}, {1,2}, {1,2,3} }, { {1,2,3,4} } };
    cos.writeArray( pre_int_aaa );
    sizeof_data += SizeOf.array(pre_int_aaa);
    long[][] pre_long_aa = new long[][] { {1}, {2} };
    cos.writeArray( pre_long_aa );
    sizeof_data += SizeOf.array(pre_long_aa);
    float[][][][] pre_float_aaaa = new float[][][][] { {{ { 1973.31f, -.1f, .2f, 10e-2f },{ 2}},{{3 },{4 }} }, {{null,{6 }},{ { 1973.31f, -.1f, .2f, 10e-2f },{7 } }} };
    cos.writeArray( pre_float_aaaa );
    sizeof_data += SizeOf.array( pre_float_aaaa );
    double[][] pre_double_aa = new double[][] { {}, { 19731973.1973, -22.1234, -.214567 } };
    cos.writeArray( pre_double_aa );
    sizeof_data += SizeOf.array(pre_double_aa);

    cos.close();

    int pre_data_size = getSize();
    assert( sizeof_data >= pre_data_size );
    log.info( "sizeof_data: " + sizeof_data + "/pre_data_size: " + pre_data_size );

    ContainerInputStream cis = getContainerInputStream(true);

    boolean[][] post_boolean_aa = (boolean[][]) cis.readArray(  );
    assert( pre_boolean_aa.length==post_boolean_aa.length);
    for ( int i=0; i<pre_boolean_aa.length; i++ )
      assert( java.util.Arrays.equals(pre_boolean_aa[i], post_boolean_aa[i] ) );
    byte[][][] post_byte_aaa = (byte[][][]) cis.readArray(  );
    assert( pre_byte_aaa.length==post_byte_aaa.length);
    for ( int i=0; i<pre_byte_aaa.length; ++i )
    {
      assert( pre_byte_aaa[i].length==post_byte_aaa[i].length);
      for ( int j=0; j<pre_byte_aaa[i].length; ++j )
	assert( java.util.Arrays.equals(pre_byte_aaa[i][j], post_byte_aaa[i][j] ) );
    }
    short[] post_short_a = (short[]) cis.readArray(  );
    assert( java.util.Arrays.equals(pre_short_a, post_short_a ) );
    int[][][] post_int_aaa = (int[][][]) cis.readArray(  );
    assert( pre_int_aaa.length==post_int_aaa.length);
    for ( int i=0; i<pre_int_aaa.length; ++i )
    {
      assert( pre_int_aaa[i].length==post_int_aaa[i].length);
      for ( int j=0; j<pre_int_aaa[i].length; ++j )
	assert( java.util.Arrays.equals(pre_int_aaa[i][j], post_int_aaa[i][j] ) );
    }
    long[][] post_long_aa = (long[][]) cis.readArray();
    assert( pre_long_aa.length==post_long_aa.length);
    for ( int i=0; i<pre_long_aa.length; i++ )
      assert( java.util.Arrays.equals(pre_long_aa[i], post_long_aa[i] ) );
    float[][][][] post_float_aaaa = (float[][][][]) cis.readArray(  );
    assert( pre_float_aaaa.length==post_float_aaaa.length);
    for ( int i=0; i<pre_float_aaaa.length; ++i )
    {
      assert( pre_float_aaaa[i].length==post_float_aaaa[i].length);
      for ( int j=0; j<pre_float_aaaa[i].length; ++j )
      {
        assert( pre_float_aaaa[i][j].length==post_float_aaaa[i][j].length);
	for ( int k=0; k<pre_float_aaaa[i][j].length; ++k)
	  assert( java.util.Arrays.equals(pre_float_aaaa[i][j][k], post_float_aaaa[i][j][k] ) );
      }
    }
    double[][] post_double_aa = (double[][]) cis.readArray( );
    assert( pre_double_aa.length==post_double_aa.length);
    for ( int i=0; i<pre_double_aa.length; i++ )
      assert( java.util.Arrays.equals(pre_double_aa[i], post_double_aa[i] ) );

    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );
    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size);
    log.info( "" );

    return true;
  }

  private boolean selftest_object_matrix() throws SerializationException, ClassNotFoundException
  {
    log.info( "selftest_object_matrix" );
    ContainerOutputStream cos = getContainerOutputStream(true);

    int[] ia = new int[] { 3, 4 };
    selftest_object object0 = new selftest_object( 1, 2, ia );
    selftest_object object1 = new selftest_object( 5, 6, new int[] { 7, 8, 9 } );
    selftest_object object2 = new selftest_object( 7, 8, ia );
    selftest_object object3 = new selftest_object( 7, 8, null );
    selftest_object[] pre_object_array_1 = new selftest_object[] { object0, object1, object2, object1, object3 };

    selftest_object object4 = new selftest_object( 21, 32, ia );
    selftest_object object5 = new selftest_object( 55, 46, new int[] { 7, 8, 9 } );
    selftest_object object6 = new selftest_object( 67, 78, ia );
    selftest_object object7 = new selftest_object( 27, 88, null );
    selftest_object[] pre_object_array_2 = new selftest_object[] { object4, object5, object6, object7, object3 };

    selftest_object[][] pre_object_matrix = new selftest_object[][] { pre_object_array_1, pre_object_array_2 };

    cos.writeArray( pre_object_matrix );

    int sizeof_data = SizeOf.array( pre_object_matrix );

    cos.close();

    /*
    ** In questo caso sizeof_data e' una sovrastima, in quanto il protocollo di serializzazione
    ** riutilizza la definizione della classe base.
    */
    int pre_data_size = getSize();
    assert( sizeof_data >= pre_data_size );
    log.info( "sizeof_data: " + sizeof_data + "/pre_data_size: " + pre_data_size );

    ContainerInputStream cis = getContainerInputStream(true);

    selftest_object[][] post_object_matrix = (selftest_object[][]) cis.readArray( );
    assert( pre_object_matrix.length == pre_object_matrix.length );
    for ( int i=0; i<post_object_matrix.length; ++i)
    {
      assert( post_object_matrix[i].length==pre_object_matrix[i].length );
      for ( int j=0; j<post_object_matrix[i].length; ++j )
      {
//	assert( java.util.Arrays.equals( pre_object_matrix[i], post_object_matrix[i] ) );
	assert( pre_object_matrix[i][j].equals(post_object_matrix[i][j]) );
      }
    }
//    assert( pre_object_array[0].equals(post_object) );

    int post_data_size = cis.getPos();

    assert( pre_data_size == post_data_size );
    log.info( "pre_data_size: " + pre_data_size + "/post_data_size: " + post_data_size);
    log.info( "" );
    assert( post_object_matrix[0][0].ia == post_object_matrix[0][2].ia );
    return true;
  }

} // end Container

/*
    final public Object readArray( Object x )
    {
      byte object_type = readObjectType();
      int handle = readHandle();

      switch (object_type)
      {
	case REFERENCE:
	  return handle_table.get(handle);
	case INSTANCE:
	  if (x instanceof boolean)
	    return readBoolean();
	  switch type:
	  case 'Z':
	    return readBoolean();
	  case '
	  boolean[] ba = readBooleanArrayInternal();
	  handle_table.put( handle, ba );
	  return ba;
      }
    }
*/

    /**
     * Legge un oggetto componente di un array dal frame e lo deseriarilizza
     * Nuovo Oggetto: INSTANCE handle NEWCLASS SUID classname Istanza
     *                INSTANCE handle REFCLASS SUID Oggetto
     * Riferimento ad un oggetto: REFERENCE handle

    FastTransportable readComponentObject( Class component_type ) throws SerializationException
    {
      byte op = readObjectType();
      int handle = readHandle();

      switch (op)
      {
	case REFERENCE:
	  // cerca il ref nell'hashtable
	  FastTransportable ref = (FastTransportable) handle_table.get( handle );
	  return ref;
	case INSTANCE:
	    Class cl;

	    try
	    { cl = readClass( component_type ); }
	    catch ( ClassNotFoundException cnfe )
	      { throw new SerializationException( cnfe.toString() ); }

	    // istanza inizializzato con il costruttore di default
	    FastTransportable instance;
	    try { instance = (FastTransportable) cl.newInstance(); }
	    catch ( IllegalAccessException iae )
	    {
	      log.info( iae.toString() );
	      throw new SerializationException( iae.toString() );
	    }
	    catch ( InstantiationException ie )
	    {
	      log.info( ie.toString() );
	      throw new SerializationException( ie.toString() );
	    }

//	   * Si usa l'overriding per chiamare il metodo virtuale per leggere l'istanza dallo stream.
//	   * Ogni oggetto sa come leggere i suoi campi
//	   * La readObject aggiunta da CLIO
	  instance.readObject( this );

//* Marshall -> readObject -> readInt, readFloat, oggetto ? Frame.readObject
//*
//*
//*
	  handle_table.put( handle, instance );
	  return instance;
	default:
	  throw new SerializationException( "Errore nel formato dello stream di lettura" );
      }

    } // end readObject


      /**
       * effettua l'append di uno stream da un altro
       *
       */
/*
      void append( FastTransportable t )
      {
	t.writeObject( this );
      }
*/

class selftest_object implements FastTransportable, java.io.Serializable
{
// campi della classe X
  int a;
  int b;

  int[] ia;


// costruttore di default
  public selftest_object( int _a, int _b, int[] _ia)
  {
    a = _a;
    b = _b;
    ia = _ia;
  }


  /**
   * Implementazione per il debug
   */
  public boolean equals( selftest_object x )
  {
    return (this==x) || ( a==x.a && b==x.b && java.util.Arrays.equals(ia, x.ia) );
  }

  // Da qui in poi e' tutto necessario per la serializzazione/deserializzazione
  public selftest_object()
  {
  }

  // writeObject/readObject generate da CLIO
  // chiamate durante l'esecuzione

  public void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeInt( a );
    cos.writeInt( b );
    cos.writeArray( ia );
    return;
  }

  public void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
    a = cis.readInt( );
    b = cis.readInt( );
    ia = (int[]) cis.readArray();
  }

  public int sizeOf()
  {
    return /*a*/ SizeOf.INT+ /*b*/ SizeOf.INT + /*ia*/ SizeOf.array( ia );
  }

  public final static long SUID = clio.SUID.getSUID( selftest_object.class );

  //  public static long SUID = java.io.ObjectStreamClass.lookup( selftest_object.class ).getSerialVersionUID();

}

class selftest_object_elem implements FastTransportable
{
// campi della classe X
  selftest_object_elem next;
  int a;
  int b;

  int[] ia;


// costruttore di default
  public selftest_object_elem( selftest_object_elem _next, int _a, int _b, int[] _ia)
  {
    next = _next;
    a = _a;
    b = _b;
    ia = _ia;
  }


  /**
   * Implementazione per il debug
   */
  public boolean equals( selftest_object_elem x )
  {
    return (this==x) || ( (next==x.next || next.equals(x.next)) && a==x.a && b==x.b && java.util.Arrays.equals(ia, x.ia) );
  }

  // Da qui in poi e' tutto necessario per la serializzazione/deserializzazione
  public selftest_object_elem()
  {
  }

  // writeObject/readObject generate da CLIO
  // chiamate durante l'esecuzione

  public final void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeObject( next );
    cos.writeInt( a );
    cos.writeInt( b );
    cos.writeArray( ia );
    return;
  }

  public final void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
    next = (selftest_object_elem) cis.readObject( );
    a = cis.readInt( );
    b = cis.readInt( );
    ia = (int[]) cis.readArray();
  }

  public final int sizeOf()
  {
    return /*next*/ SizeOf.object(next) + /*a*/ SizeOf.INT+ /*b*/ SizeOf.INT + /*ia*/ SizeOf.array( ia );
  }

  public final static long SUID = clio.SUID.getSUID( selftest_object_elem.class );
//  public static long SUID = java.io.ObjectStreamClass.lookup( selftest_object_elem.class ).getSerialVersionUID();

}
