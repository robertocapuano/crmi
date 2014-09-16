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
package run.reference;

import run.FastTransportable;
import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

/**
 * Descrittore di un singolo servizio
 */

public final class Service implements FastTransportable
{
  String name;
  String signature;

  public Service( String _name, String _signature )
  {
    name = _name;
    signature = _signature;
  }

  public final String getName()
  {
    return name;
  }

  public final String getSignature()
  {
    return signature;
  }


  /**
   * Serializzazione: costruttore di default
   */
  public Service()
  {
  }

  /**
   * Serializzazione: writeObject()
   */
  public final void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeString( name );
    cos.writeString( signature );
  }

  /**
   * Serializzazione: readObject()
   */
  public final void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
    name = cis.readString();
    signature = cis.readString();
  }

  public final int sizeOf()
  {
    return SizeOf.string(name) + SizeOf.string(signature);
  }

  public final static long SUID = clio.SUID.computeSUID( "run.reference.Service" );

  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( '<' );
    res.append( getClass().getName() );
    res.append( ">\n" );
    res.append( "name: " );
    res.append( name );
    res.append( ' ' );
    res.append( "signature: " );
    res.append( signature );
    res.append( '\n' );

    return res.toString();
  }
}
