package clio;

import java.util.Vector;

import com.borland.primetime.util.Debug;

import de.fub.bytecode.*;
import de.fub.bytecode.classfile.*;
import de.fub.bytecode.generic.*;

import run.stub_skeleton.*;

import run.serialization.SizeOf;

/**
 * Genera le classi Stub/Skeleton per il servizio
 * La Major/Minor version della classe generata e' uguale a quella della classe del servizio
 */

final class SkeletonGenerator extends Generator
{
  private final static Type[] init_arg_type = new Type[] { TypeRepository.Services_type };

  static JavaClass generateSkeletonClass( JavaClass service_jcl )
  {
    String superclass_name = "run.stub_skeleton.Skeleton";
    String service_name = service_jcl.getClassName();
    String class_name = service_name + Skeleton.CLASSNAME_SUFFIX;
    String file_name = class_name.substring( class_name.lastIndexOf(".")+1 );

    ClassGen cg = new ClassGen( class_name, superclass_name, class_name+".class", Constants.ACC_PUBLIC | Constants.ACC_SUPER, null );
    ConstantPoolGen cpg = cg.getConstantPool();
    InstructionFactory factory = new InstructionFactory(cg);

    addInit( cg, cpg, factory );

    String[] interfaces_name = service_jcl.getInterfaceNames();

    for ( int i=0; i<interfaces_name.length; ++i )
    {
      JavaClass interface_implemented = Repository.lookupClass( interfaces_name[i] );

      if ( interface_implemented.instanceOf(TypeRepository.Services_jclass) )
      {
	Method[] service_method = interface_implemented.getMethods();
	ObjectType interface_type = new ObjectType( interface_implemented.getClassName() );
	for ( int j=0; j<service_method.length; ++j )
	  addServiceSkeleton( cg, cpg, factory, interface_type, service_method[j] );
      }
    }

    return cg.getJavaClass();
  }

  /**
   * Aggiunge il constructor.
   * <code>
   * public init(Services _services )
   * {
   *  super( _services );
   * }
   * </code>
   */

  private static void addInit( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory )
  {
    final String class_name = cg.getClassName();
    final String superclass_name = cg.getSuperclassName();
    final InstructionList il_init = new InstructionList( );

    il_init.append( factory.createLoad( Type.OBJECT, 0 ) );
    il_init.append( factory.createLoad( Type.OBJECT, 1 ) );

    il_init.append( factory.createInvoke( superclass_name, "<init>", Type.VOID, init_arg_type, Constants.INVOKESPECIAL ) );
    il_init.append( factory.createReturn( Type.VOID ) );

    final MethodGen mg_init = new MethodGen( Constants.ACC_PUBLIC, Type.VOID, init_arg_type, new String[] { "_services" }, "<init>", class_name, il_init, cpg );

    // finalizing
    mg_init.setMaxStack();
    mg_init.setMaxLocals();
    cg.addMethod( mg_init.getMethod() );

    // garbage-collection
    il_init.dispose();
  }

  /**
   * Aggiunge l'invocazione locale
   */
  private static void addServiceSkeleton( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory, ObjectType rs_type, Method service )
  {
    InstructionHandle[] ihandle = new InstructionHandle[16];

    final String service_name = service.getName();
    final String service_signature = service.getSignature();
    final String coded_service_name = new StringBuffer( ).
					        append( "invoke_" ).
					        append( service_name ).
						append('_').
						append( service_signature.hashCode() ).
						toString().replace('-','_');
    final Type[] varg_type = Type.getArgumentTypes( service_signature );
    int varg_length = varg_type.length;

    boolean have_objects = false;
    for ( int i=0; i<varg_length; ++i )
      if (  ! (varg_type[i] instanceof BasicType) )
      {
	have_objects = true;
	break;
      }

    final InstructionList il_invoke = new InstructionList( );

    Debug.println( "addServiceSkeleton(): " + service_name );

    // generazione del metodo
    final MethodGen mg_invoke = new MethodGen( Constants.ACC_PUBLIC, TypeRepository.Container_type, new Type[] { TypeRepository.Container_type }, new String[] { "container" }, coded_service_name, cg.getClassName(), il_invoke, cpg );

    // inizio degli slot delle variabili locali: dopo gli argomenti della chiamata
    int start_slot = 1+TypeRepository.Container_type.getSize();
    if (mg_invoke.getMaxLocals()<start_slot)
      mg_invoke.setMaxLocals( start_slot );

    // this
    int this_index = 0;
    // variabile container: argomento della chiamata
    int container_index = 1;

    // variabile: cis
    int cis_index = addLocalVariable( factory, mg_invoke, il_invoke, "cis", TypeRepository.cis_type );

    // variabile: cos
    int cos_index = addLocalVariable( factory, mg_invoke, il_invoke, "cos", TypeRepository.cos_type );

    // variabile: res (Container)
    int res_index = addLocalVariable( factory, mg_invoke, il_invoke, "res", TypeRepository.Container_type );

    // rs: reference all'interfaccia di servizi
    String rs_fqn = rs_type.getClassName();
    int rs_index = addLocalVariable( factory, mg_invoke, il_invoke, "rs", rs_type );

    // variabile: framesize
    int framesize_index = addLocalVariable( factory, mg_invoke, il_invoke, "framesize", Type.INT );

    // variabile: return
    Type return_type = Type.getReturnType(service_signature);
    int return_index = addLocalVariable(factory, mg_invoke, il_invoke, "return_res", return_type );

    // load del container
    ihandle[0] = il_invoke.append( factory.createLoad( TypeRepository.Container_type, container_index ) );

    // Container.getContainerOutputStream( false/true );
    il_invoke.append( new PUSH( cpg, have_objects ) );
//    il_invoke.append( new PUSH( cpg, true ) );
    InvokeInstruction invokeget_op = factory.createInvoke( TypeRepository.Container_fqn, "getContainerInputStream", TypeRepository.cis_type, new Type[] { Type.BOOLEAN }, Constants.INVOKEVIRTUAL );
    il_invoke.append( invokeget_op );

    // cis = ...
    il_invoke.append( factory.createStore( TypeRepository.cis_type, cis_index) );


    // argomenti della chiamata
    int[] varg_index = new int[varg_length];
    for ( int i=0; i<varg_length; ++i )
    {
      String read_method;
      Type read_type;

      if ( varg_type[i] instanceof BasicType )
      {
	StringBuffer arg_typename = new StringBuffer( varg_type[i].toString() );
	arg_typename.setCharAt( 0, Character.toUpperCase(arg_typename.charAt(0)) );

	read_method = "read"+arg_typename;
	read_type = varg_type[i];
      } // end BasicType
      else
      if ( varg_type[i] instanceof ArrayType )
      {
	read_method = "readArray";
	read_type = Type.OBJECT;
      }
      else
//      if (service_arg_type[i] instanceof ObjectType  )
      {
	if ( varg_type[i].equals( Type.STRING ) )
	{
	  read_method = "readString";
	  read_type = Type.STRING;
	}
	else
	{
	  read_method = "readObject";
	  read_type = TypeRepository.FastTransportable_type;
	}
      } // end Type.Object

      il_invoke.append( factory.createLoad( TypeRepository.cis_type, cis_index ) );
      InvokeInstruction invoke_read = factory.createInvoke( TypeRepository.cis_fqn, read_method, read_type, Type.NO_ARGS, Constants.INVOKEVIRTUAL );
      il_invoke.append( invoke_read );

      varg_index[i] = addLocalVariable(factory,mg_invoke, il_invoke, "varg"+i, varg_type[i] );
      if ( ! (varg_type[i] instanceof BasicType ) )
      {
	CHECKCAST checkcast_op = factory.createCheckCast( (ReferenceType) varg_type[i] );
	il_invoke.append( checkcast_op );
      }

      Instruction store_op = factory.createStore(varg_type[i], varg_index[i] );
      il_invoke.append( store_op );
    } // end for

    // cis.close()
    ihandle[1] = il_invoke.append( factory.createLoad( TypeRepository.cis_type, cis_index) );
    il_invoke.append( factory.createInvoke( TypeRepository.cis_fqn, "close", Type.INT, Type.NO_ARGS, Constants.INVOKEVIRTUAL ) );
    // rimuoviamo il risultato
    il_invoke.append( InstructionConstants.POP );

    // getfield: services
    il_invoke.append( factory.createLoad( Type.OBJECT, this_index ) );
    ihandle[2] = il_invoke.append( factory.createGetField(TypeRepository.Skeleton_fqn, "services", TypeRepository.Services_type ) );
    // rs = ..
    il_invoke.append( factory.createCheckCast(rs_type) );
    il_invoke.append( factory.createStore(rs_type, rs_index) );

    // rs.f()
    ihandle[3] = il_invoke.append( factory.createLoad( rs_type, rs_index) );

    for ( int i=0; i<varg_length; ++i )
    {
      il_invoke.append( factory.createLoad( varg_type[i], varg_index[i] ) );
    }

    il_invoke.append( factory.createInvoke(rs_fqn,service_name, return_type, varg_type, Constants.INVOKEINTERFACE ) );

    // res = ...

    if ( return_index>=0 )
    {
      il_invoke.append( factory.createStore(return_type, return_index) );
    }

    boolean return_type_is_object;

    InstructionList il_write = new InstructionList();

    String sizeof_method, write_method;
    Type ws_type;

    if (return_type==Type.VOID )
    {
      return_type_is_object = false;
      il_invoke.append( new PUSH( cpg, 0 ) );
    }
    else
    {
      il_write.append( factory.createLoad( TypeRepository.cos_type, cos_index ) );
      il_write.append( factory.createLoad( return_type, return_index ) );

      if (return_type instanceof BasicType)
      {
	return_type_is_object = false;
	int sizeof;

	sizeof = SizeOf.primitive( return_type.getSignature().charAt(0) );

	StringBuffer arg_typename = new StringBuffer( return_type.toString() );
	arg_typename.setCharAt( 0, Character.toUpperCase(arg_typename.charAt(0)) );

	write_method = "write"+arg_typename;
	ws_type = return_type;

	il_invoke.append( new PUSH( cpg, sizeof ) );
      }
      else
      {
	return_type_is_object = true;

	if (return_type instanceof ArrayType )
	{
	  sizeof_method = "array";
	  write_method = "writeArray";
	  ws_type = Type.OBJECT;
	}
	else
	{
	  if (return_type.equals( Type.STRING ) )
	  {
	    sizeof_method = "string";
	    write_method = "writeString";
	    ws_type = Type.STRING;
	  }
	  else
	  {
	    sizeof_method = "object";
	    write_method = "writeObject";
	    ws_type = TypeRepository.FastTransportable_type;
	    CHECKCAST checkcast_op = factory.createCheckCast( TypeRepository.FastTransportable_type );
	    il_write.append( checkcast_op );
	  }
	}

        il_invoke.append( factory.createLoad( return_type, return_index ) );
	InvokeInstruction invoke_sizeof = factory.createInvoke( TypeRepository.SizeOf_fqn, sizeof_method, Type.INT, new Type[] { ws_type }, Constants.INVOKESTATIC );
	il_invoke.append( invoke_sizeof );

      } // end ObjectType/ArrayType

      InvokeInstruction invoke_write = factory.createInvoke( TypeRepository.cos_fqn, write_method, Type.VOID, new Type[] { ws_type }, Constants.INVOKEVIRTUAL );
      il_write.append( invoke_write );
    } // end !=void

    ihandle[4] = il_invoke.append( factory.createStore( Type.INT, framesize_index) );

    // new Container( );
    Instruction new_op = factory.createNew( TypeRepository.Container_type );
    ihandle[5] = il_invoke.append( new_op );
    il_invoke.append( InstructionConstants.DUP );
    il_invoke.append( InstructionConstants.DUP );
    // tre riferimenti a container

    // container = .. ; consuma il primo riferimento
    ihandle[6] = il_invoke.append( factory.createStore( TypeRepository.Container_type, res_index ) );

    // Container.init( framesize ); consuma il secondo riferimento
    ihandle[7] = il_invoke.append( factory.createLoad( Type.INT, framesize_index ) );
    InvokeInstruction init_op = factory.createInvoke( TypeRepository.Container_fqn, "<init>", Type.VOID, new Type[] { Type.INT }, Constants.INVOKESPECIAL );
    il_invoke.append( init_op );

    // Container.getContainerOutputStream( false/true ); consuma il terzo riferimento
    ihandle[8] = il_invoke.append( new PUSH( cpg, return_type_is_object ) );
    invokeget_op = factory.createInvoke( TypeRepository.Container_fqn, "getContainerOutputStream", TypeRepository.cos_type, new Type[] { Type.BOOLEAN }, Constants.INVOKEVIRTUAL );
    il_invoke.append( invokeget_op );

    // cos = ...
    ihandle[9] = il_invoke.append( factory.createStore( TypeRepository.cos_type, cos_index) );

    // cos.writeXXX()
    il_invoke.append( il_write );

    // cos.close()
    ihandle[10] = il_invoke.append( factory.createLoad( TypeRepository.cos_type, cos_index) );
    il_invoke.append( factory.createInvoke( TypeRepository.cos_fqn, "close", Type.INT, Type.NO_ARGS, Constants.INVOKEVIRTUAL ) );
    // rimuoviamo il risultato
    il_invoke.append( InstructionConstants.POP );

    // return res;
    ihandle[11] = il_invoke.append( factory.createLoad( TypeRepository.Container_type, res_index ) );
    ihandle[12] = il_invoke.append( factory.createReturn( TypeRepository.Container_type ) );


    // aggiunge i throws
    mg_invoke.addException( "run.RemoteException" );
    mg_invoke.addException( "run.serialization.SerializationException" );

    // debug
    for ( int i=0; i<13; ++i )
      mg_invoke.addLineNumber( ihandle[i], i );

    // finalizing
    mg_invoke.setMaxStack();
    mg_invoke.setMaxLocals();
    cg.addMethod( mg_invoke.getMethod() );

    // garbage-collection
    il_invoke.dispose();
    return;
  }
}