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

/**
 * Frame rappresenta i dati raw inviati/da inviare a remoto
 *
 */

public class Frame
{
  // dati contenuti nel frame
  protected byte[] byte_frame;
  protected int size;

  public Frame( int _size )
  {
    size = _size;
    byte_frame = new byte[size];
  }

  public Frame( byte[] _byte_frame )
  {
    byte_frame = _byte_frame;
    size = _byte_frame.length;
  }

  public final byte[] toArray()
  {
    return byte_frame;
  }

  public final void setByteFrame( byte[] _byte_frame )
  {
    byte_frame = _byte_frame;
    size = byte_frame.length;
  }

  public final int getSize()
  {
    return size;
  }

  public final void free()
  {
    byte_frame = null;
  }

  /*
   *  Accessi inline (?) in quanto dichiarati final, valore restituito short in modo da evitare valori<0
   */

  public final short get( int index )
  {
    short s = byte_frame[index];
    return  s>=0 ? s : (short) (s + 256);
  }

  public final void put( int index, byte value )
  {
    byte_frame[index] = value;
  }

  public String toString()
  {
    int last = Math.min(size, 1024);
    String res = "size: " + size + " < ";

    for ( int i=0; i<last; ++i )
      res += get(i) + " ";

    res += ">\n";
    return res;
  }
}

