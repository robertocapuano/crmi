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

public class Y extends X implements Transportable {

// campi della classe Y

  int c;

  public int get()
  {
    return c;
  }

// costruttore di default
  public Y(int _a, int _b, int[] _ia, int _c)
  {
    super (_a, _b, _ia );
    c = _c;
  }

  public String toString()
  {
    StringBuffer res = new StringBuffer();

    res.append( "<"+ super.toString() );

//    res.append( "< a: " + a + " b: " + b + " c: " + c + " ia: <");

//    for ( int i=0; i<ia.length; ++i )
//      res.append( ia[i] + ", " );

    res.append( " c: " + c + ">");
    return res.toString();
  }

  /*
   * Metodi necessari per la serializzazione
   */
  public Y()
  {
    super();
  }

  // writeObject/readObject generate da CLIO
  // chiamate durante l'esecuzione

  public void writeObject0( Container.ContainerOutputStream cos ) throws SerializationException
  {
//    super.writeObject( cos );
    cos.writeInt( c );
    return;
  }

  public void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
//    super.readObject( cis );
    c = cis.readInt();
  }

//  int[] ia = new int[10];
//  String s = "pippo";
//  X x = new X();

  public int sizeOf()
  {
//    return SizeOf.INT + SizeOf.string(s) + SizeOf.array( ia ) + SizeOf.object(x );
    return SizeOf.INT;
  }

//  final public static long SUID = java.io.ObjectStreamClass.lookup( Y.class ).getSerialVersionUID();

}