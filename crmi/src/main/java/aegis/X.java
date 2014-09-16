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
package aegis;

import run.*;
import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

public class X implements Transportable {

// campi della classe X
  int a;
  int b;

  int[] ia;

  public int get()
  {
    return a;
  }

// costruttore di default
  public X(int _a, int _b, int[] _ia)
  {
    a = _a;
    b = _b;
    ia = _ia;
  }


  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( "< a: " + a + " b: " + b + " ia: <");

    for ( int i=0; i<ia.length; ++i )
      res.append( ia[i] + "," );

    res.append( "> >");
    return res.toString();
  }

  /*
   * Metodi necessari per la serializzazione
   */
  public X()
  {
    super();
  }

  // writeObject/readObject generate da CLIO
  // chiamate durante l'esecuzione

  public void writeObject0( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeInt( a );
    cos.writeInt( b );
    cos.writeArray( ia );
    return;
  }

  public void readObject0( Container.ContainerInputStream cis ) throws SerializationException
  {
    a = cis.readInt( );
    b = cis.readInt( );
    ia = (int[]) cis.readArray();
  }

  public int sizeOf0()
  {
    return /*a*/ SizeOf.INT+ /*b*/ SizeOf.INT + /*ia*/ SizeOf.array( ia );
  }

//  public final static long SUID0 = clio.SUID.getSUID(X.class);
//  final public static long SUID = java.io.ObjectStreamClass.lookup( X.class ).getSerialVersionUID();

}