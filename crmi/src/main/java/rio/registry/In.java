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
