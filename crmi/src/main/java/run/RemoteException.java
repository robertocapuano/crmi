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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.serialization.SerializationException;
import run.serialization.Container;
import run.serialization.SizeOf;

public class RemoteException extends Exception implements FastTransportable
{
        private final Logger log = LogManager.getLogger(this.getClass() );
 
  public RemoteException()
  {
  }

  public RemoteException( String str )
  {
    super();
    message = str;
  }

  public String getMessage()
  {
    return message;
  }

  public void writeObject( Container.ContainerOutputStream cos ) throws SerializationException
  {
    cos.writeString( getMessage() );
  }

  public void readObject( Container.ContainerInputStream cis ) throws SerializationException
  {
    message = cis.readString();
  }

  public int sizeOf()
  {
    return SizeOf.string( getMessage() );
  }

  public final static long SUID = clio.SUID.computeSUID( "run.RemoteException" );
//  public static long SUID = java.io.ObjectStreamClass.lookup( RemoteException.class ).getSerialVersionUID();

  // public final long SUID;
/*
  public static final long getSUID()
  {
    // il suid sara' calcolata al tempo della compilazione
    long suid = `.ObjectStreamClass.lookup( RemoteException.class ).getSerialVersionUID();
    return suid;
  }
*/
//  public static final long SUID = ;

  String message;

  public String toString()
  {
    return message;
  }
/*
  public static final void main( String[] args )
  {
    RemoteException remote_ex = new RemoteException( "pippo");
    // Memorizza nel container l'exception
    Container container_res = new Container( SizeOf.object( remote_ex ) );
    // true indica che serializzeremo anche oggetti quindi creare la reference_table
    Container.ContainerOutputStream cos = container_res.getContainerOutputStream( true );
    try { cos.writeObject( remote_ex ); }
    // *** Come compartarsi per questo errore?
    catch (SerializationException se) { Debug.print( se.toString() ); }
    cos.free();
  }
*/
}