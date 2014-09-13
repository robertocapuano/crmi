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