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

//import com.borland.primetime.util.Debug;

import run.Services;
import run.RemoteException;

import run.stub_skeleton.Stub;

import run.exec.Manager;
import run.exec.ExecException;

import run.reference.RemoteServiceReference;

import run.session.Callgram;

import run.serialization.SizeOf;
import run.serialization.Container;
import run.serialization.SerializationException;

import run.transport.TransportException;

import rio.registry.RegistryServicesReference;

import run.FastTransportable;

public class Aegis_Stub extends Stub implements Rs // Nota: Rs extends services
{
  public Aegis_Stub( RegistryServicesReference rsr )
  {
    super( rsr );
  }

  // primo servizio
  public int f1( X x, int[] ia, int i ) throws RemoteException
  {
    try
    {
      int f1_framesize =0;
      f1_framesize = SizeOf.object( (FastTransportable) x );
      f1_framesize += SizeOf.array(ia);
      f1_framesize += SizeOf.INT;

      Container container = new Container( f1_framesize );

      // true indica che serializzeremo anche oggetti quindi creare la reference_table
      Container.ContainerOutputStream cos = container.getContainerOutputStream( true );

      cos.writeObject( (FastTransportable) x );
      cos.writeArray( ia );
      cos.writeInt( i );
      cos.close();

      // migliorare qui per l'individuazione del servizio remoto.
      // firma aggiunta a "mano"
      RemoteServiceReference remote_services_reference =
	  registry_services_reference /*campo ereditato*/
	    .toRemoteServiceReference( "f1", "(Laegis/X;[II)I" );

      Callgram callgram = new Callgram( Callgram.CALL, remote_services_reference, null, container );

      Callgram rcv_callgram = Manager.execRemoteCall( callgram );
      callgram.free();
      callgram = null;

      Container res = rcv_callgram.getContainer();

      // argomento false: il risultato e' un primitivo
      Container.ContainerInputStream cis = res.getContainerInputStream( false );

      int _f1 = cis.readInt();

      rcv_callgram.free();
      rcv_callgram = null;
      return _f1;
    }
    catch (ExecException exec_exception ) { throw new RemoteException( exec_exception.getMessage() ); }
    catch (SerializationException serialization_exception) { throw new RemoteException(serialization_exception.getMessage() ); }
    catch (TransportException transport_exception) { throw new RemoteException( transport_exception.getMessage() ); }
  }

/**
 *  secondo servizio
 */

  public int[] f2( X x, X[] xa, int i ) throws RemoteException
  {
    try
    {
      int f2_framesize = SizeOf.object( (FastTransportable) x ) + SizeOf.array( xa ) + SizeOf.INT;

      Container container = new Container( f2_framesize );

      // true indica che serializzeremo anche oggetti quindi creare la reference_table
      Container.ContainerOutputStream cos = container.getContainerOutputStream( true );

  /**
   *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
   * quindi prima i bytes, chars, float, ..
   * poi gli array...di primitivi
   * le matrici (?)
   * gli oggetti
   * gli array di oggetti
   * le matrici di oggetti (?)
   */
      cos.writeObject( (FastTransportable) x );
      cos.writeArray( xa );
      cos.writeInt( i );
      cos.close();

      // migliorare qui per l'individuazione del servizio remoto.
      // firma aggiunta a "mano"
      RemoteServiceReference remote_service_reference =
	  registry_services_reference /*campo ereditato*/
	    .toRemoteServiceReference( "f2", "(Laegis/X;[Laegis/X;I)[I" );
      Callgram callgram = new Callgram( Callgram.CALL, remote_service_reference, null, container );

      Callgram rcv_callgram = Manager.execRemoteCall( callgram );
      callgram.free();
      callgram = null;

      Container res = rcv_callgram.getContainer();

      // argomento false: il risultato e' un primitivo
      Container.ContainerInputStream cis = res.getContainerInputStream( true );

      int[] _f2 = (int[]) cis.readArray();
      rcv_callgram.free();
      rcv_callgram = null;

      return _f2;
    }
    catch (ExecException exec_exception ) { throw new RemoteException( exec_exception.getMessage() ); }
    catch (SerializationException serialization_exception) { throw new RemoteException(serialization_exception.getMessage() ); }
    catch (TransportException transport_exception) { throw new RemoteException( transport_exception.getMessage() ); }
  }

/**
 *  terzo servizio
 */

  public X f3( int i ) throws RemoteException
  {
    try
    {
      int f3_framesize = SizeOf.INT;

      Container container = new Container( f3_framesize );

      // true indica che serializzeremo anche oggetti quindi creare la reference_table
      Container.ContainerOutputStream cos = container.getContainerOutputStream( true );

  /**
   *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
   * quindi prima i bytes, chars, float, ..
   * poi gli array...di primitivi
   * le matrici (?)
   * gli oggetti
   * gli array di oggetti
   * le matrici di oggetti (?)
   */
      cos.writeInt( i );
      cos.close();

      // migliorare qui per l'individuazione del servizio remoto.
      // firma aggiunta a "mano"
      RemoteServiceReference remote_service_reference =
	registry_services_reference /*campo ereditato*/
	  .toRemoteServiceReference( "f3", "(I)Laegis/X;" );
      Callgram callgram = new Callgram( Callgram.CALL, remote_service_reference, null, container );

      Callgram rcv_callgram = Manager.execRemoteCall( callgram );
      callgram.free();
      callgram = null;

      Container res = rcv_callgram.getContainer();

      // argomento false: il risultato e' un primitivo
      Container.ContainerInputStream cis = res.getContainerInputStream( true );

      X x = (X) cis.readObject();
      rcv_callgram.free();
      rcv_callgram = null;

      return x;
    }
    catch (ExecException exec_exception ) { throw new RemoteException( exec_exception.getMessage() ); }
    catch (SerializationException serialization_exception) { throw new RemoteException(serialization_exception.getMessage() ); }
    catch (TransportException transport_exception) { throw new RemoteException( transport_exception.getMessage() ); }
  }

  /**
   *  quarto servizio
   */

  public Z f4( int i ) throws RemoteException
  {
    try
    {
      int f4_framesize = SizeOf.INT;

      Container container = new Container( f4_framesize );

      // true indica che serializzeremo anche oggetti quindi creare la reference_table
      Container.ContainerOutputStream cos = container.getContainerOutputStream( true );

  /**
   *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
   * quindi prima i bytes, chars, float, ..
   * poi gli array...di primitivi
   * le matrici (?)
   * gli oggetti
   * gli array di oggetti
   * le matrici di oggetti (?)
   */
      cos.writeInt( i );
      cos.close();

      // migliorare qui per l'individuazione del servizio remoto.
      // firma aggiunta a "mano"
      RemoteServiceReference remote_service_reference =
	registry_services_reference /*campo ereditato*/
	  .toRemoteServiceReference( "f4", "(I)Laegis/Z;" );
      Callgram callgram = new Callgram( Callgram.CALL, remote_service_reference, null, container );

      Callgram rcv_callgram = Manager.execRemoteCall( callgram );
      callgram.free();
      callgram = null;

      Container res = rcv_callgram.getContainer();

      // argomento false: il risultato e' un primitivo
      Container.ContainerInputStream cis = res.getContainerInputStream( true );

      Z z = (Z) cis.readObject();
      rcv_callgram.free();
      rcv_callgram = null;

      return z;
    }
    catch (ExecException exec_exception ) { throw new RemoteException( exec_exception.getMessage() ); }
    catch (SerializationException serialization_exception) { throw new RemoteException(serialization_exception.getMessage() ); }
    catch (TransportException transport_exception) { throw new RemoteException( transport_exception.getMessage() ); }
  }

  public void f5( ) throws RemoteException
  {
    try
    {
      int f5_framesize = 0;

      Container container = new Container( f5_framesize );

      // true indica che serializzeremo anche oggetti quindi creare la reference_table
      Container.ContainerOutputStream cos = container.getContainerOutputStream( false );

  /**
   *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
   * quindi prima i bytes, chars, float, ..
   * poi gli array...di primitivi
   * le matrici (?)
   * gli oggetti
   * gli array di oggetti
   * le matrici di oggetti (?)
   */

      // migliorare qui per l'individuazione del servizio remoto.
      // firma aggiunta a "mano"
      RemoteServiceReference remote_service_reference =
	registry_services_reference /*campo ereditato*/
	  .toRemoteServiceReference( "f5", "()V" );
      Callgram callgram = new Callgram( Callgram.CALL, remote_service_reference, null, container );

      Callgram rcv_callgram = Manager.execRemoteCall( callgram );
      callgram.free();
      callgram = null;

      Container res = rcv_callgram.getContainer();

      // argomento false: il risultato e' un primitivo
      Container.ContainerInputStream cis = res.getContainerInputStream( false );

//      Z z = (Z) cis.readObject();
      rcv_callgram.free();
      rcv_callgram = null;

      return;
    }
    catch (ExecException exec_exception ) { throw new RemoteException( exec_exception.getMessage() ); }
    catch (SerializationException serialization_exception) { throw new RemoteException(serialization_exception.getMessage() ); }
    catch (TransportException transport_exception) { throw new RemoteException( transport_exception.getMessage() ); }
  }

  /**
   *  quarto servizio
   */

  public void f6( int i ) throws RemoteException
  {
    try
    {
      int f6_framesize = SizeOf.INT;

      Container container = new Container( f6_framesize );

      // true indica che serializzeremo anche oggetti quindi creare la reference_table
      Container.ContainerOutputStream cos = container.getContainerOutputStream( true );

  /**
   *  Sullo stream i campi sono ordinati per tipo e per ordine alfabetico.
   * quindi prima i bytes, chars, float, ..
   * poi gli array...di primitivi
   * le matrici (?)
   * gli oggetti
   * gli array di oggetti
   * le matrici di oggetti (?)
   */
      cos.writeInt( i );
      cos.close();

      // migliorare qui per l'individuazione del servizio remoto.
      // firma aggiunta a "mano"
      RemoteServiceReference remote_service_reference =
	registry_services_reference /*campo ereditato*/
	  .toRemoteServiceReference( "f6", "(I)V" );
      Callgram callgram = new Callgram( Callgram.CALL, remote_service_reference, null, container );

      Callgram rcv_callgram = Manager.execRemoteCall( callgram );
      callgram.free();
      callgram = null;

      Container res = rcv_callgram.getContainer();

      // argomento false: il risultato e' un primitivo
      Container.ContainerInputStream cis = res.getContainerInputStream( true );

      rcv_callgram.free();
      rcv_callgram = null;

      return;
    }
    catch (ExecException exec_exception ) { throw new RemoteException( exec_exception.getMessage() ); }
    catch (SerializationException serialization_exception) { throw new RemoteException(serialization_exception.getMessage() ); }
    catch (TransportException transport_exception) { throw new RemoteException( transport_exception.getMessage() ); }
  }


}