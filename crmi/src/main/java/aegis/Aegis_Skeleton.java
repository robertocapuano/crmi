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

import aegis.Rs;

import run.serialization.Container;
import run.stub_skeleton.Skeleton;

import run.RemoteException;
import run.Services;
import run.serialization.SizeOf;
import run.serialization.SerializationException;
import run.FastTransportable;

/**
 * classe generata
 * Rappresenta l'endpoint della chiamata
 * La chiamata parte dal basso per risalire
*/
public class Aegis_Skeleton extends Skeleton
{

  public Aegis_Skeleton( Services _services )
  {
    super( _services );
  }

/**
 * Il nome del metodo e' importante:
 * invoke_methodname_codicehash
 *   public int f1( X x, int[] ia, int i ) throws RemoteException
 *   diventa invoke_f1__519339342
 *   con -519339342 codice hash associato a (Laegis.X;[II)I
 */


  public Container invoke_f1__519339342( Container container ) throws RemoteException, SerializationException
  {
    Container.ContainerInputStream cis = container.getContainerInputStream(true);

    // il suid sara' calcolata al tempo della compilazione
    // per adesso usiamo uno shortcut
//    long suid = java.io.ObjectStreamClass.lookup(X.class).getSerialVersionUID();


    X x = (X) cis.readObject( );
    int[] ia = (int[]) cis.readArray();
    int i = cis.readInt();
//    int[] ia = new int[20];
//    int i = 10;

    /**
     * up-call
     * int f1( X x, int[] ia, int i ) throws RemoteException;
     */
    Rs server_ref = (Rs) services;
    int invoke_res = server_ref.f1( x, ia, i ) ;

    Container res = new Container( SizeOf.INT );
    Container.ContainerOutputStream cos = res.getContainerOutputStream( false );
    cos.writeInt( invoke_res );
    cos.close();
    return res;
  }

  // codice hash associato a int[] f2( X x, X[] xa, int i )

  public Container invoke_f2_373712653( Container container ) throws RemoteException, SerializationException
  {
    Container.ContainerInputStream cis = container.getContainerInputStream( true );

    // il suid sara' calcolata al tempo della compilazione
    //
//    long suid = java.io.ObjectStreamClass.lookup(X.class).getSerialVersionUID();


/**
 *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
 * quindi prima i bytes, chars, float, ..
 * poi gli array...di primitivi
 * le matrici ?
 * gli oggetti
 * gli array di oggetti
 * le matrici di oggetti?
 */

    X x = (X) cis.readObject();
    X[] xa = (X[]) cis.readArray(  );
    int i = cis.readInt();

    /**
     * up-call
     *   int[] f2( X x, X[] xa, int i ) throws RemoteException;
     */
    Rs server_ref = (Rs) services;
    int[] invoke_res = server_ref.f2( x, xa, i );

    Container res = new Container( SizeOf.array(invoke_res) );
    Container.ContainerOutputStream cos = res.getContainerOutputStream(true);
    cos.writeArray( invoke_res );
    cos.close();
//    throw new run.serialization.SerializationException( "pippo 2");

    return res;
  }

  public Container invoke_f3__1108078295( Container container ) throws RemoteException, SerializationException
  {
    Container.ContainerInputStream cis = container.getContainerInputStream( true );

    // il suid sara' calcolata al tempo della compilazione
    //
//    long suid = java.io.ObjectStreamClass.lookup(X.class).getSerialVersionUID();


/**
 *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
 * quindi prima i bytes, chars, float, ..
 * poi gli array...di primitivi
 * le matrici ?
 * gli oggetti
 * gli array di oggetti
 * le matrici di oggetti?
 */

    int i = cis.readInt();

    /**
     * up-call
     *   X f3( int i ) throws RemoteException;
     */
    Rs server_ref = (Rs) services;
    X invoke_res = server_ref.f3( i );

    Container res = new Container( SizeOf.object((FastTransportable) invoke_res) );
    Container.ContainerOutputStream cos = res.getContainerOutputStream(true);
    cos.writeObject( (FastTransportable) invoke_res );
    cos.close();
//    throw new run.serialization.SerializationException( "pippo 2");

    return res;
  }

  public Container invoke_f4__1108078233( Container container ) throws RemoteException, SerializationException
  {
    Container.ContainerInputStream cis = container.getContainerInputStream( true );

    // il suid sara' calcolata al tempo della compilazione
    //
//    long suid = java.io.ObjectStreamClass.lookup(X.class).getSerialVersionUID();


/**
 *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
 * quindi prima i bytes, chars, float, ..
 * poi gli array...di primitivi
 * le matrici ?
 * gli oggetti
 * gli array di oggetti
 * le matrici di oggetti?
 */

    int i = cis.readInt();

    /**
     * up-call
     *   X f4( int i ) throws RemoteException;
     */
    Rs server_ref = (Rs) services;
    Z invoke_res = server_ref.f4( i );

    Container res = new Container( SizeOf.object((FastTransportable) invoke_res) );
    Container.ContainerOutputStream cos = res.getContainerOutputStream(true);
    cos.writeObject( (FastTransportable) invoke_res );
    cos.close();
//    throw new run.serialization.SerializationException( "pippo 2");

    return res;
  }

  public Container invoke_f5_39797( Container container ) throws RemoteException, SerializationException
  {
    Container.ContainerInputStream cis = container.getContainerInputStream( true );


/**
 *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
 * quindi prima i bytes, chars, float, ..
 * poi gli array...di primitivi
 * le matrici ?
 * gli oggetti
 * gli array di oggetti
 * le matrici di oggetti?
 */

//    int i = cis.readInt();

    /**
     * up-call
     *   void f5( ) throws RemoteException;
     */
    Rs server_ref = (Rs) services;
    server_ref.f5( );

    Container res = new Container( 0 );
    Container.ContainerOutputStream cos = res.getContainerOutputStream(false);
//    cos.writeObject( (FastTransportable) invoke_res );
    cos.close();
//    throw new run.serialization.SerializationException( "pippo 2");

    return res;
  }

}