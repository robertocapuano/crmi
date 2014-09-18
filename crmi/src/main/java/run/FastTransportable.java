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