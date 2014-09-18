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

abstract class Generator
{
  protected static int addLocalVariable( InstructionFactory factory, MethodGen mg, InstructionList il, String name, Type type )
  {
    if ( type.getSignature().equals("V") || type==Type.VOID )
      return -1;

    LocalVariableGen lg = mg.addLocalVariable( name, type, null, null ); //try_start, try_end );
    int index = lg.getIndex();

    if (type instanceof BasicType)
    {
      if (type==Type.LONG)
      {
	il.append( new PUSH( null, 0l ) );
      }
      else
      if (type==Type.DOUBLE)
      {
	il.append( new PUSH( null, 0d ) );
      }
      else
      if (type==Type.FLOAT)
      {
	il.append( new PUSH( null, 0f ) );
      }
      else
      {
	il.append( new PUSH( null, 0 ) );
      }
    }
    else
//    if (type instanceof ObjectType || type instanceof ArrayType)
      il.append( InstructionConstants.ACONST_NULL );

    Instruction store_op = factory.createStore( type, index );
    InstructionHandle store_handle = il.append( store_op );
    lg.setStart( store_handle );

    return index;
  }

}