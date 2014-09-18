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
package aegis;

//import com.borland.primetime.util.Debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rio.registry.Registry;
import rio.registry.RegistryException;

import run.RemoteException;
import run.transport.IPAddress;

import run.session.SessionTable;
import run.Utility;

public class Client {
      private static final Logger log = LogManager.getLogger(Client.class );

  public Client()
  {
  }

  public static void main(String[] args) throws RegistryException, RemoteException
  {
    String prop_registry_address = run.Utility.getStringProperty( "rio.registry.address", "10.0.2.1" );
    int prop_registry_port = run.Utility.getIntProperty( "rio.registry.port", 2001 );


    IPAddress registry_address = null;

    try
    {
      registry_address = new IPAddress( java.net.InetAddress.getByName(prop_registry_address), prop_registry_port );
    }
    catch (Exception e ) { log.info( e.toString() ); }

    X x = new X( 1, 2, new int[] { 3,4,5 } );

    Rs remote_services = (Rs) Registry.lookup( registry_address, "aegis" );

    log.info( ((run.stub_skeleton.Stub) remote_services).toString() );

    System.out.println( SessionTable.getStatus() );

    /*
    ** f1()
    */
    int res = remote_services.f1( x, new int[] { 6,7,8}, 10000 );
    log.info( "f1(): "+res );

    res = remote_services.f1( x, new int[] { 6,7,8}, 1000 );
    log.info( "f1(): "+res );

    /*
    ** f2()
    */
    int[] ia = new int[] { 6,7,8 };

    X[] xa = new X[] { new X( 3,4, ia),new X( 5,6, ia) };

    log.info( "f2(x,xa,9) invoked");
    try
    {
      int ir[] = remote_services.f2(x,xa,9);

      log.info( "f2() res: <" );
      for ( int i=0; i<ir.length; i++)
	log.info( ir[i]+" "  );
      log.info( ">");
    }
    catch (RemoteException re)
    {
      log.info(re.toString());
    }

    /*
    ** f2()
    */
    log.info( "f2(x,xa,0) invoked");
    try
    {
      int ir[] = remote_services.f2(x,xa,0);

      log.info( "f2() res: <" );
      for ( int i=0; i<ir.length; i++)
	log.info( ir[i]+" "  );
      log.info( ">");
    }
    catch (RemoteException re)
    {
//      log.info(re.toString());
//      log.info( re.getMessage() );
      re.printStackTrace();
    }


    /*
    ** f2()
    */
    Y y = new Y( 10, 20, ia, 8 );

    log.info( "f2(y,xa,9) invoked");
    try
    {
      int ir[] = remote_services.f2(y,xa,9);

      log.info( "f2() res: <" );
      for ( int i=0; i<ir.length; i++)
	log.info( ir[i]+" "  );
      log.info( ">");
    }
    catch (RemoteException re)
    {
      log.info(re.toString());
    }


    /*
    ** f3()
    */
    X x_res = remote_services.f3(30);

    log.info( "f3(): result classname: " + x_res.getClass().getName() );
    log.info( "<x_res.get(): " + x_res.get()+">" );
    log.info( x_res.toString() );


    /*
    ** f4()
    */
    Z z_res = remote_services.f4(3);

    log.info( "f4() res: " + z_res.getClass().getName() );
    log.info( z_res.toString() );
    for ( Z i=z_res; i!=null; i = i.next )
      log.info( i.toString() );


    /*
    ** f5()
    */
    remote_services.f5();
    log.info( "f5() res: void" );


    float f6 = remote_services.f6( x, 4247, "hello" );
    log.info( "f6: " + f6 );

    String s7 = remote_services.f7( x, 29, "jbuilder" );
    log.info("s7: " + s7 );

    X x8 = remote_services.f8( 9996, "tyew", new X(29,10, new int[0] ) );
    log.info( "x8: " +x8.toString() );

    log.info( Utility.getStatus() );
  }
}
