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
package run.transport;

import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

/**
 *  Dalla specifica VIA:
 *   typedef struct {
 *    VIP_UINT16 HostAddressLen;
 *    VIP_UINT16 DiscriminatorLen;
 *    VIP_UINT8 HostAddress[1];
 *  } VIP_NET_ADDRESS;
 */

public class VIAAddress extends NetAddress
{
  /**
   * HostAddress: indirizzo del nodo
   * uint16 --> int
   */
  private int host_address;

  /**
   * endpoint discriminator
   * uint16 --> int
   */
  private int discriminator;

  /**
   * byte length
   * uint8 --> int
   */
  private int byte_length;

  /**
   * Crea un indirizzo VIA
   * @param nodenum Numero del nodo di cui i primi 16 bit validi
   *        discriminator Endpoint discriminator
   */
  public VIAAddress(int nodenum, int discriminator ) {
    super();
    host_address = nodenum;
    discriminator = discriminator;
    byte_length = 0;
  }

  public final int getHostAddress()
  {
    return host_address;
  }

  public final int getDiscriminator()
  {
    return discriminator;
  }

  public final byte[] getByteArray()
  {
    byte[] ba = new byte[ sizeOf() ];

// credo che questo sia il formato di VIA
    ba[0] = (byte) ( (host_address>>>16 )& 0xff);
    ba[1] = (byte) ( host_address & 0xff );
    ba[2] = (byte) ( (discriminator>>>16) & 0xff );
    ba[3] = (byte) ( discriminator & 0xff );
    ba[4] = 0;

    return ba;
  }

  public final byte getType()
  {
    return VIA_ADDRESS;
  }


  /**
   * L'indirizzo remote e' raggiungibile da questa rete
   */
  public boolean isReachable( NetAddress remote_address )
  {
    // supponiamo vi sia una sola rete Myrinet
    return remote_address.getClass() == getClass();
  }

  /**
   * Serializzazione: writeObject()
   */
  public final void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeInt( discriminator );
    cos.writeInt( host_address );
    cos.writeByte( (byte) byte_length );
  }

  /**
   * Serializzazione: readObject()
   */
  public final void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
    discriminator = cis.readInt();
    host_address = cis.readInt();
    byte_length = cis.readByte();
  }

  public final int sizeOf()
  {
    return SizeOf.INT + SizeOf.INT + SizeOf.BYTE;
  }
}
