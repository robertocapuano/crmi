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
package rio.registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.reference.*;
import run.serialization.Container;
import run.serialization.Container.ContainerInputStream;
import run.serialization.SerializationException;

import run.stub_skeleton.Skeleton;
import run.RemoteException;
import run.Services;
import run.serialization.SizeOf;

/**
 * In e' contattato dal registry remoto su una interrogazione di un servizio globale.
 *
 */

public class In extends Skeleton implements Services
{
               private final Logger log = LogManager.getLogger(this.getClass() );
 
  public In( Services _services )
  {
    super( _services );
  }

  // firma di register:
  // (Lrio/registry/RegistryServicesReference;)V ---> _1239236785
  public Container invoke_register__1239236785( Container container ) throws RemoteException, SerializationException
  {
    Container.ContainerInputStream cis = container.getContainerInputStream( true );
    cis.close();

/*
    try
    {
*/
    // parametri di ingresso
      RegistryServicesReference registry_ref = (RegistryServicesReference) cis.readObject(  );
      Global.register( registry_ref );
      Container res = new Container( SizeOf.VOID );
//    Container.ContainerOutputStream cos = res.getContainerOutputStream();
//    cos.writeVoid(  );
      return res;
/*
    }
    catch (RegistryException registry_exception )
    {
      RemoteException remote_exception = new RemoteException( registry_exception.getMessage() );
      throw remote_exception;
    }
    catch (SerializationException serialization_exception )
    {
      RemoteException remote_exception = new RemoteException( serialization_exception.getMessage() );
      throw remote_exception;
    }
*/
  }

  // firma:
  // (Ljava/lang/String;)Lrio/registry/RegistryServicesReference;
  // _415118191
  public Container invoke_lookup__415118191( Container container ) throws RemoteException, SerializationException
  {
    Container.ContainerInputStream cis = container.getContainerInputStream( true );

/*
    try
    {
*/
      // parametri di ingresso
      String services_name = cis.readString( );
      cis.close();

      RegistryServicesReference registry_ref = Global.lookup( services_name );

      Container res = new Container( SizeOf.object( registry_ref ) );
      Container.ContainerOutputStream cos = res.getContainerOutputStream( true );
      cos.writeObject( registry_ref );
      cos.close();
      return res;
/*
    }
*/
/*    catch (RegistryException registry_exception )
    {
      RemoteException remote_exception = new RemoteException( registry_exception.getMessage() );
      throw remote_exception;
    }
*/
/*
    catch (SerializationException serialization_exception )
    {
      RemoteException remote_exception = new RemoteException( serialization_exception.getMessage() );
      throw remote_exception;
    }
*/

  }

}
