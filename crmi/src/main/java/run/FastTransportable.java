package run;

import run.serialization.SerializationException;

import run.serialization.Container;
import run.serialization.SerializationException;

/**
 * Una classe che vuole essere resa FastTransportable, implementa il tipo Transportable.
 * On-the-fly Clio modifica la classe rendendola fast-transportable.
 * Tutto le classi devono definire questi metodi oltre che un campo long SUID
 * 1) Un costruttore di default
 * 2) writeObject()
 * 3) readObject()
 * 4) sizeOf()
 * 5) il campo SUID
 */

public interface FastTransportable extends Transportable
{
// Stream unique identifier
//  public final static long SUID = ..;

// Bisogna far generare in maniera automatica questi due metodi
// Serializza l'oggetto
  public void writeObject( Container.ContainerOutputStream cos ) throws SerializationException;
// inizializza l'istanza dell'oggetto
  public void readObject( Container.ContainerInputStream cis ) throws SerializationException;

// ...insieme a questo, dipende dal tipo effettivo dell'oggetto.
  public int sizeOf(); // dimensione dell'oggetto.

}