package run.transport;

import run.FastTransportable;
import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

public abstract class NetAddress implements FastTransportable
{
 /**
   * Indirizzo IP: 6 bytes
   * 4 bytes di rete, 2 per la porta
   */

  public static final byte IP_ADDRESS = 0x6;

  /**
   * Indirizzo VIA: 5 bytes
   * 2 per il nodo, 2 per l'endpoint, 1 di lunghezza.
   */
  public static final byte VIA_ADDRESS = 0x5;

  /**
   * Formato e lunghezza dell'indirizzo
   */
  public abstract byte getType();

  /**
   * Array di byte che formano l'indirizzo.
   */
  public abstract byte[] getByteArray();

  public boolean equals( NetAddress addr )
  {
    return this==addr || ( addr!=null && getType()==addr.getType() && java.util.Arrays.equals(getByteArray(),addr.getByteArray() ));
  }

  /**
   * L'indirizzo remote e' raggiungibile da questa rete
   */
  public abstract boolean isReachable( NetAddress remote_address );

  /**
   * Serializzazione:
   */

  public abstract void writeObject( Container.ContainerOutputStream cos ) throws SerializationException;
  public abstract void readObject( Container.ContainerInputStream cis ) throws SerializationException;

  /**
   * Dimensione dell'indirizzo.
   */
  public abstract int sizeOf();

  final public static long SUID = 0;

}