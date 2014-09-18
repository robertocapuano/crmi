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
package run.stub_skeleton;

import run.Services;
import run.reference.RemoteServiceReference;

import rio.registry.RegistryServicesReference;


/**
 * classe generata
 * da un registry intelligente (centralizzato) <---
 *
 * Relazioni tra le classi:
 * Services (memorizzato dall'applicazione) == Aegis_Stub == Stub --> RegistryServicesReference
 *
 */


public abstract class Stub implements Services
{
  /**
   * Suffisso delle classi Stub
   */
  public static final String CLASSNAME_SUFFIX = "_Stub";

  /**
   * Indirizzo del server remoto.
   */
  protected RegistryServicesReference registry_services_reference;

  public Stub( RegistryServicesReference ini_reference )
  {
    registry_services_reference = ini_reference;
  }

/*  public static String getClassname( String services_classname )
  {
    return services_classname + CLASSSNAME_SUFFIX;
  }
*/
  public String getClassname(  )
  {
    return getClass().getName();
  }

  public String toString()
  {
    StringBuffer res = new StringBuffer();
    res.append( '<' );
    res.append( getClass().getName() );
    res.append( ">\n" );
    res.append( registry_services_reference.toString() );
    return res.toString();
  }
}

