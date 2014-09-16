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

public class Z implements Transportable
{
  int val;
  Z next;
//  String s;

  public Z()
  {
  }

  public Z( int _val, Z _next )
  {
    val = _val;
    next = _next;
  }

  public String toString()
  {
    String res = "";

    res += "< val: " + val + " next: " + next + ">";
    return res;
  }

  public void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
//    cos.writeString( s );
    cos.writeInt( val );
    cos.writeObject( (FastTransportable) next );
    return;
  }

  public void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
//    s= cis.readString();
    val = cis.readInt( );
    next = (Z) cis.readObject();
  }

  public int sizeOf()
  {
    return SizeOf.INT + SizeOf.object((FastTransportable) next);
  }

//  final public static long SUID = java.io.ObjectStreamClass.lookup( Z.class ).getSerialVersionUID();

}