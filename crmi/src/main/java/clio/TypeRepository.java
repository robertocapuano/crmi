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
package clio;

import de.fub.bytecode.*;
import de.fub.bytecode.classfile.*;
import de.fub.bytecode.generic.*;

abstract class TypeRepository
{
  // ClusterClassLoader
  final static JavaClass Transportable_jclass = Repository.lookupClass("run.Transportable");
  final static JavaClass Stub_jclass = Repository.lookupClass( "run.stub_skeleton.Stub" );
  final static JavaClass Skeleton_jclass = Repository.lookupClass( "run.stub_skeleton.Skeleton" );

  // RWSGenerator
  final static String FastTransportable_fqn = "run.FastTransportable";
  final static JavaClass FastTransportable_jclass = Repository.lookupClass( FastTransportable_fqn);
  final static ObjectType FastTransportable_type = new ObjectType( FastTransportable_fqn );
  final static String cis_fqn = "run.serialization.Container$ContainerInputStream";
  final static ObjectType cis_type = new ObjectType( cis_fqn );
  final static String cos_fqn = "run.serialization.Container$ContainerOutputStream";
  final static ObjectType cos_type = new ObjectType(cos_fqn);

  // Stub/Skeleton Generator
  final static String Services_fqn = "run.Services";
  final static ObjectType Services_type = new ObjectType( Services_fqn );
  final static JavaClass Services_jclass = Repository.lookupClass( Services_fqn );
  final static String Container_fqn = "run.serialization.Container";
  final static ObjectType Container_type = new ObjectType( Container_fqn );
  final static String RemoteServiceReference_fqn = "run.reference.RemoteServiceReference";
  final static ObjectType RemoteServiceReference_type = new ObjectType( RemoteServiceReference_fqn );
  final static String Stub_fqn = "run.stub_skeleton.Stub";
  final static ObjectType Stub_type = new ObjectType( Stub_fqn );
  final static String Skeleton_fqn = "run.stub_skeleton.Skeleton";
  final static ObjectType Skeleton_type = new ObjectType( Skeleton_fqn );
  final static String RegistryServicesReference_fqn = "rio.registry.RegistryServicesReference";
  final static ObjectType RegistryServicesReference_type = new ObjectType( RegistryServicesReference_fqn );
  final static String Callgram_fqn = "run.session.Callgram";
  final static ObjectType Callgram_type = new ObjectType( Callgram_fqn );
  final static String CallgramListener_fqn = "run.session.CallgramListener";
  final static ObjectType CallgramListener_type = new ObjectType( CallgramListener_fqn );
  final static String Manager_fqn = "run.exec.Manager";
  final static ObjectType Manager_type = new ObjectType( Manager_fqn );
  final static String SizeOf_fqn = "run.serialization.SizeOf";
  final static ObjectType SizeOf_type = new ObjectType( SizeOf_fqn );

  final static Type[] callgram_init_arg_type = new Type[] { Type.BYTE, RemoteServiceReference_type, CallgramListener_type, Container_type };

  final static ObjectType ExecException_type = new ObjectType("run.exec.ExecException");
  final static ObjectType SerializationException_type = new ObjectType("run.serialization.SerializationException");
  final static ObjectType TransportException_type = new ObjectType("run.transport.TransportException");

}