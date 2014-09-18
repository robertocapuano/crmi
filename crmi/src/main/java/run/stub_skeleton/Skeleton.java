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

import run.*;
import run.serialization.Container;
import run.reference.RemoteServiceReference;

/**
 *  Relazioni tra le classi:
 *  services_name -> LocalServicesReference -- > Skeleton --> Services
 */

public abstract class Skeleton
{
  /**
   * Suffisso delle classi Skeleton
   */
  public static final String CLASSNAME_SUFFIX = "_Skeleton";

  /**
   * Riferimento al servizio a cui lo skeleton e' associato
   */
  protected Services services;
//  protected LocalServicesReference services_reference;

  public Skeleton( Services _services )
  {
//    remote_reference = remote_ref;
    services = _services;
  }

/*  public static String getClassname( String services_classname )
  {
    return services_classname + CLASSNAME_SUFFIX;
  }
*/

  public String getClassname( )
  {
    return getClass().getName();
  }

  public final Services getServices()
  {
    return services;
  }

  public final void setServices( Services services_ref )
  {
    services = services_ref;
  }

}
