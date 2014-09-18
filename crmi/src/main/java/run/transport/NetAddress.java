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