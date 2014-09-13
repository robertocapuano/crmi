package run;

import run.serialization.SerializationException;

import run.serialization.Container;
import run.serialization.SerializationException;

/**
 * Identifica una classe come "trasportabile" (serializzabile) sullo stream.
 * La classe che implementa Transportable verra' modificata on-the-fly facendola implementare @see run.FastTransportable.
 */

public interface Transportable extends java.io.Serializable
{
}