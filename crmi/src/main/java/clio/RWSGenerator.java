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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import run.serialization.SizeOf;

//import sun.tools.java.Type;

/**
 * RWGenerator rappresenta il builder del sistema.
 * 1. Aggiunge tra le interfacce implementate FastTransportable
 * 2. Aggiunge eventualmente il costruttore di default: con access modifier public
 * 3. Aggiunge eventualmente i metodi
 *      public void writeObject( run.serialization.Container.ContainerOutputStream cos ) throws SerializationException;
 *      public void readObject( run.serialization.Container.ContainerInputStream cis ) throws SerializationException;
 *      public int sizeOf();
 * 4. aggiunge il campo:
 *      public final static long SUID = java.io.ObjectStreamClass.lookup( X.class ).getSerialVersionUID();
 *
 * I campi sono aggiunti con due cicli for () che operano sullo stesso array, quindi la successione delle ReadXXX(), WriteXXX() dovrebbe essere la stessa.
 * Questo ci serve per la serializzazione.
 *
 * Nel caso in cui l'utente abbia definito i suoi metodi readObject()/writeObject() la classe aggiunge una invocaazione a super.readObject(), super.writeObject()
 *
 */

final class RWSGenerator extends Generator
{
      private final Logger log = LogManager.getLogger(this.getClass() );
  final static String init_signature = Utility.methodTypeToSignature("void", new String[0] );
  final static String read_signature = Utility.methodTypeToSignature("void", new String[] { "run.serialization.Container$ContainerInputStream" } );
  final static String write_signature = Utility.methodTypeToSignature("void", new String[] { "run.serialization.Container$ContainerOutputStream" } );
  final static String sizeof_signature = Utility.methodTypeToSignature("int", new String[0] );

  static JavaClass modify( JavaClass j_cl )
  {
    ClassGen cg;
    ConstantPoolGen cpg;
    InstructionFactory factory;

    cg = new ClassGen( j_cl );
    cpg = cg.getConstantPool();
    factory = new InstructionFactory( cg );

    log.info( "RWSGenerator(): " + j_cl.getClassName() );

    if ( !checkAndSetInit( cg ) )
    {
      boolean ok = addInit( cg,cpg, factory );
      if (!ok) return null;
    }

    if ( cg.containsMethod( "readObject", read_signature  ) == null )
      addReadObject( cg, cpg, factory );
    else
      addSuperInvoke( cg, cpg, factory, "readObject", read_signature, TypeRepository.cis_type );

    if ( cg.containsMethod( "writeObject", write_signature ) == null )
      addWriteObject( cg, cpg, factory );
    else
      addSuperInvoke( cg, cpg, factory, "writeObject", write_signature, TypeRepository.cos_type );

    if ( cg.containsMethod( "sizeOf", sizeof_signature ) == null )
      addSizeOf( cg, cpg, factory );
    else
    {
      addSuperSizeOf( cg, cpg, factory  );
    }

    cg.addInterface("run.FastTransportable");

    if ( cg.containsField("SUID") == null )
      addSUID( cg, cpg, factory );

    JavaClass jcl = cg.getJavaClass();
    return jcl;
  }

  private static boolean checkAndSetInit( ClassGen cg )
  {
    String init_signature = Utility.methodTypeToSignature("void", new String[0] );
    Method[] method_list = cg.getMethods();
    int i;

    for ( i=0; i<method_list.length; ++i )
    {
      if ( method_list[i].getName().equals("<init>") && method_list[i].getSignature().equals(init_signature) )
      {
	int access_flags = method_list[i].getAccessFlags();

	if ((access_flags&Constants.ACC_PUBLIC) == 0 )
	{
	  access_flags &= ~ (Constants.ACC_PRIVATE | Constants.ACC_PROTECTED );
	  access_flags |= Constants.ACC_PUBLIC;
	  method_list[i].setAccessFlags( access_flags );
	}
	break;
      }
    }

    return i<method_list.length;
  }

  /**
   * Aggiunge il costruttore di default
   */
  private static boolean addInit( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory )
  {
    String class_name = cg.getJavaClass().getClassName();
    String superclass_name = cg.getJavaClass().getSuperclassName();
    JavaClass superclass_jclass = Repository.lookupClass( superclass_name );

    log.info( "RWSGenerator(): " + cg.getClassName() + ".<init>()" );
    // la superclasse contiene il default constructor ?
    Method[] method_list = superclass_jclass.getMethods();

    for ( int i=0; i<method_list.length; ++i )
    {
      if ( method_list[i].getName().equals("<init>") && method_list[i].getSignature().equals(init_signature) )
      {
	int access_flags = method_list[i].getAccessFlags();

	if ((access_flags&Constants.ACC_PUBLIC)==0 && (access_flags&Constants.ACC_PROTECTED)==0)
	// arghhh non vi possiamo accedere
	  return false;
	else
	  break;
      }
    }

    cg.addEmptyConstructor( Constants.ACC_PUBLIC );
    return true;
  }

  private static void addReadObject( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory )
  {
    String class_name = cg.getJavaClass().getClassName();
    String superclass_name = cg.getJavaClass().getSuperclassName();
    JavaClass superclass_jclass = Repository.lookupClass( superclass_name );

    MethodGen mg_read;
    InstructionList il_read;

    il_read = new InstructionList();

    InvokeInstruction invoke_op;
    PUTFIELD putfield_op;
    CHECKCAST  checkcast_op;
    ReturnInstruction return_op;

    log.info( "RWSGenerator(): " + cg.getClassName() + ".readObject( Container.ContainerInputStream cis )" );

    // aggiunge super.readObject() se la superclasse e' FastTransportable
    if (superclass_jclass.instanceOf( TypeRepository.FastTransportable_jclass) )
    {
      // load del reference this per la successiva operazione: putfield
      il_read.append( factory.createLoad( Type.OBJECT, 0 ) );

      // carica il primo argomento nello stack: cis
      il_read.append( factory.createLoad( Type.OBJECT, 1 ) );

      invoke_op = factory.createInvoke( superclass_jclass.getClassName(), "readObject", Type.VOID, new Type[] { TypeRepository.cis_type }, Constants.INVOKESPECIAL );
      il_read.append( invoke_op );
    }

    // gestione dei fields
    Field[] f = cg.getFields();

    for ( int i=0; i<f.length; ++i )
    {
      // saltiamo i campi non serializzabili
      if ( f[i].isTransient() || f[i].isStatic() )
	continue;

      // dati del campo da serializzare
      String field_signature = f[i].getSignature();
      Type field_type = Type.getType( field_signature );
      String field_name = f[i].getName();

      // metodo di input e tipo di ritorno
      String input_method;
      Type input_type;

      // opcodes comuni a tutte le istruzioni

      // load del reference this per la successiva operazione: putfield
      // Lo slot 0 delle variabili locali contiene il riferimento this,
      // lo copiamo nella posizione 0  dello stack operands
      //
      // Stato dello stack
      // 1)         this,  cis                                                             TOP dello stack
      il_read.append( factory.createLoad( Type.OBJECT, 0 ) );

      // carica il primo argomento nello stack: cis
      // Lo slot 1 delle variabili locali contiene il primo argomento della chiamata,
      // lo copiamo nella posizione 1  dello stack operands
      // per la successiva chiamata
      //
      // Stato dello stack
      // 2)         this,  cis                                                             TOP dello stack
      il_read.append( factory.createLoad( Type.OBJECT, 1 ) );

      if ( field_type instanceof BasicType )
      {
	StringBuffer field_typename = new StringBuffer( field_type.toString() );
	field_typename.setCharAt(0,Character.toUpperCase(field_typename.charAt(0)) );

//	log.info(field_typename.toString());

	input_method = "read"+field_typename;
	input_type = field_type;

	//
	// 3)              readXXX                                                        consuma il reference cis e produce un dato
	//          this, dato di tipo XXX
	invoke_op = factory.createInvoke( TypeRepository.cis_fqn, input_method, input_type, Type.NO_ARGS, Constants.INVOKEVIRTUAL );
	il_read.append( invoke_op );
      }
      else
      if (field_type instanceof ArrayType )
      {
	input_method = "readArray";
	input_type = Type.OBJECT;

	//
	// 3)              readArray                                                        consuma il reference cis e produce un dato
	//          this, reference
	invoke_op = factory.createInvoke( TypeRepository.cis_fqn, input_method, input_type, Type.NO_ARGS, Constants.INVOKEVIRTUAL );
	il_read.append( invoke_op );

	/**
	 * 4)         CheckCast
	 */
	checkcast_op = factory.createCheckCast( (ReferenceType) field_type );
	il_read.append( checkcast_op );
      }
      else
      {
	if (field_type==Type.STRING)
	{
	  input_method = "readString";
	  input_type = field_type;
	}
	else
	{
	  input_method = "readObject";
	  input_type = TypeRepository.FastTransportable_type;
	}


	/**
	 * 3)              readObject/readString                                                   consuma il reference cis e produce un dato
	 *          this, reference
	 */
	invoke_op = factory.createInvoke( TypeRepository.cis_fqn, input_method, input_type, Type.NO_ARGS, Constants.INVOKEVIRTUAL );
	il_read.append( invoke_op );

	/**
	 * 4)         CheckCast
	 */
	checkcast_op = factory.createCheckCast( (ReferenceType) field_type );
	il_read.append( checkcast_op );

      }
/*
      else
      {
	// Campo sconosciuto
	input_type = null;
	log.info( "Tipo campo: " + f[i] + "non riconosciuto" );
      }
*/
      /**
       * 5)              put_field                                                      consuma this, dato
       */
      putfield_op = factory.createPutField( class_name, field_name, field_type );
      il_read.append( putfield_op );


    } // end for



  // Osservazione: I

    return_op = factory.createReturn( Type.VOID );
    il_read.append( return_op );

    mg_read = new MethodGen( Constants.ACC_PUBLIC, Type.VOID,
			      new Type[] { TypeRepository.cis_type }, new String[] { "cis" },
			      "readObject",  class_name,
			       il_read, cpg );

    mg_read.addException("run.serialization.SerializationException");

    // finalizing
    mg_read.setMaxStack();
    mg_read.setMaxLocals();
    cg.addMethod( mg_read.getMethod() );

    // garbage-collection
    il_read.dispose();
  }

  private static void addWriteObject( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory )
  {
    String class_name = cg.getJavaClass().getClassName();
    String superclass_name = cg.getJavaClass().getSuperclassName();
    JavaClass superclass_jclass = Repository.lookupClass( superclass_name );

    MethodGen mg_write;
    InstructionList il_write;

    il_write = new InstructionList();

    InvokeInstruction invoke_op;
    GETFIELD getfield_op;
    ReturnInstruction return_op;

    log.info( "RWSGenerator(): " + cg.getClassName() + ".writeObject( Container.ContainerInputStream cis )" );

    /*
    ** aggiunge super.writeObject() se la superclasse e' FastTransportable
    */
    if (superclass_jclass.instanceOf( TypeRepository.FastTransportable_jclass) )
    {
      // load del reference this per la successiva operazione: putfield
      /**
       * Stato dello stack
       * 1)         this,  cis                                                             TOP dello stack
       */
      il_write.append( factory.createLoad( Type.OBJECT, 0 ) );

      // carica il primo argomento nello stack: cis
	/**
	 * Stato dello stack
	 * 2)         this,  cis                                                             TOP dello stack
	 */
      il_write.append( factory.createLoad( Type.OBJECT, 1 ) );

      invoke_op = factory.createInvoke( superclass_jclass.getClassName(), "writeObject", Type.VOID, new Type[] { TypeRepository.cos_type }, Constants.INVOKESPECIAL );
      il_write.append( invoke_op );
    }

    // gestione dei fields
    Field[] f = cg.getFields();

    for ( int i=0; i<f.length; ++i )
//    for ( int i=0; i<1; ++i )
    {
      // saltiamo i campi non serializzabili
      if ( f[i].isTransient() || f[i].isStatic() )
	continue;

      // dati del campo da serializzare
      String field_signature = f[i].getSignature();
      Type field_type = Type.getType( field_signature );
      String field_name = f[i].getName();

      // metodo di output e tipo di ritorno
      String output_method;
      Type output_type;

      // opcodes comuni a tutte le istruzioni


      // carica il primo argomento nello stack: cos
      // Lo slot 1 delle variabili locali contiene il primo argomento della chiamata,
      // lo copiamo nella posizione 0  dello stack operands
      // per la successiva chiamata
	/**
	 * Stato dello stack
	 * 1)         cos                                                             TOP dello stack
	 */

      il_write.append( factory.createLoad( Type.OBJECT, 1 ) );

      // load del reference this per la successiva operazione: getfield
      // Lo slot 0 delle variabili locali contiene il riferimento this,
      // lo copiamo nella posizione 1  dello stack operands

      /**
       * Stato dello stack
       * 2)         cos, this                                                             TOP dello stack
       */
      il_write.append( factory.createLoad( Type.OBJECT, 0 ) );

      // aggiungiamo la getfield

      /**
       * 3)              getfield                                                      consuma this
       *  Stato dello stack
       *           cos                                                             TOP dello stack
       */
      getfield_op = factory.createGetField( class_name, field_name, field_type );
      il_write.append( getfield_op );

      if ( field_type instanceof BasicType )
      {
	StringBuffer field_typename = new StringBuffer( field_type.toString() );
	field_typename.setCharAt(0,Character.toUpperCase(field_typename.charAt(0)) );

//	log.info(field_typename.toString());

	output_method = "write"+field_typename;
	output_type = field_type;
      }
      else
      if (field_type instanceof ArrayType )
      {
	output_method = "writeArray";
	output_type = Type.OBJECT;
      }
      else
      if (field_type instanceof ObjectType  )
      {
	if (field_type == Type.STRING )
	{
	  output_method = "writeString";
	  output_type = Type.STRING;
	}
	else
	{
	  output_method = "writeObject";
	  output_type = TypeRepository.FastTransportable_type;
	}
      }
      else
      {
	// Campo sconosciuto
	output_type = null;
	output_method = null;
	log.info( "Tipo campo: " + f[i] + "non riconosciuto" );
	continue;
      }

      invoke_op = factory.createInvoke( TypeRepository.cos_fqn, output_method, Type.VOID, new Type[] { output_type}, Constants.INVOKEVIRTUAL );
      il_write.append( invoke_op );

    } // end for


  // Osservazione: I

    return_op = factory.createReturn( Type.VOID );
    il_write.append( return_op );

    mg_write = new MethodGen( Constants.ACC_PUBLIC, Type.VOID,
			      new Type[] { TypeRepository.cos_type }, new String[] { "cos" },
			      "writeObject",  class_name,
			       il_write, cpg );

    mg_write.addException("run.serialization.SerializationException");

    // finalizing
    mg_write.setMaxStack();
    mg_write.setMaxLocals();
    cg.addMethod( mg_write.getMethod() );

    // garbage-collection
    il_write.dispose();
  }

  private static void addSUID( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory )
  {
    long suid = SUID.computeSUID( cg.getJavaClass() );
    //java.io.ObjectStreamClass.lookup( cl).getSerialVersionUID();

//    log.info( "SUID: " + suid );
    FieldGen fg_suid = new FieldGen( Constants.ACC_PUBLIC|Constants.ACC_STATIC|Constants.ACC_FINAL, Type.LONG, "SUID", cpg );

    fg_suid.setInitValue(suid);

    cg.addField( fg_suid.getField() );
  }

  private static void addSizeOf( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory )
  {
    String class_name = cg.getJavaClass().getClassName();
    String superclass_name = cg.getJavaClass().getSuperclassName();
    JavaClass superclass_jclass = Repository.lookupClass( superclass_name );
    Type fasttransportable_type = new ObjectType( "run.FastTransportable" );
    String sizeof_signature = "run.serialization.SizeOf";
//    Type sizeofclass_type = new ObjectType( sizeof_signature );

    int even;
    int primitive_size = 0;

    MethodGen mg_sizeof;
    InstructionList il_sizeof;

    il_sizeof = new InstructionList();

    InvokeInstruction invoke_op;
    GETFIELD getfield_op;
    PUSH push;
    IADD iadd_op = new IADD();
    ReturnInstruction return_op;

    log.info( "RWSGenerator(): " + cg.getClassName() + ".SUID" );

    /*
    ** aggiunge super.sizeOf() se la superclasse e' FastTransportable
    */
    if (superclass_jclass.instanceOf( TypeRepository.FastTransportable_jclass) )
    {
      // load del reference this per la successiva operazione: putfield
      /**
       * Stato dello stack
       * 1)         this,                                                             TOP dello stack
       */
      il_sizeof.append( factory.createLoad( Type.OBJECT, 0 ) );

      invoke_op = factory.createInvoke( superclass_jclass.getClassName(), "sizeOf", Type.INT, Type.NO_ARGS, Constants.INVOKESPECIAL );
      il_sizeof.append( invoke_op );

      even = 1;
    }
    else
    {
      even = 0;
    }

    // gestione dei fields
    Field[] f = cg.getFields();

    for ( int i=0; i<f.length; ++i )
    {
      // saltiamo i campi non serializzabili
      if ( f[i].isTransient() || f[i].isStatic() )
	continue;

      // dati del campo da serializzare
      String field_signature = f[i].getSignature();
      Type field_type = Type.getType( field_signature );
      String field_name = f[i].getName();

      // metodo di sizeof e tipo di argomento
      String sizeof_method;
      Type sizeof_type;

      // opcodes comuni a tutte le istruzioni


      if ( field_type instanceof BasicType )
      {
	primitive_size += SizeOf.primitive( field_type.getSignature().charAt(0) );
      }
      else
      {
	// Object

	/**
	 * Stato dello stack
	 * 1)          this                                                             TOP dello stack
	 */
	il_sizeof.append( factory.createLoad( Type.OBJECT, 0 ) );

	// aggiungiamo la getfield

	/**
	 * 2)              getfield                                                      consuma this
	 *  Stato dello stack
	 *           this, field                                                             TOP dello stack
	 */
	getfield_op = factory.createGetField( class_name, field_name, field_type );
	il_sizeof.append( getfield_op );


	if (field_type instanceof ArrayType )
	{
	  sizeof_method = "array";
	  sizeof_type = Type.OBJECT;
	}
	else
	if (field_type instanceof ObjectType  )
	{
	  if (field_type==Type.STRING )
	  {
	    sizeof_method = "string";
	    sizeof_type = Type.STRING;
	  }
	  else
	  {
	    sizeof_method = "object";
	    sizeof_type = fasttransportable_type;
	  }
	}
	else
	{
	  // Campo sconosciuto
	  sizeof_type = null;
	  sizeof_method = null;
	  log.info( "Tipo campo: " + f[i] + "non riconosciuto" );
	  continue;
	}

	invoke_op = factory.createInvoke( sizeof_signature, sizeof_method, Type.INT, new Type[] { sizeof_type}, Constants.INVOKESTATIC );
	il_sizeof.append( invoke_op );

	++even;
	if (even>1)
	{
	  il_sizeof.append( iadd_op );
	}
      } // end object
    } // end for

    il_sizeof.append( new PUSH( cpg, primitive_size ) );
    if (even>0)
    {
      il_sizeof.append( iadd_op );
    }

  // Osservazione: I

    return_op = factory.createReturn( Type.INT );

    il_sizeof.append( return_op );


    mg_sizeof = new MethodGen( Constants.ACC_PUBLIC, Type.INT,
			      Type.NO_ARGS, new String[0],
			      "sizeOf",  class_name,
			       il_sizeof, cpg );

    // finalizing
    mg_sizeof.setMaxStack();
    mg_sizeof.setMaxLocals();
    cg.addMethod( mg_sizeof.getMethod() );

    // garbage-collection
    il_sizeof.dispose();
  }


  /*
  ** aggiunge super.readObject()/super.writeObject() se la superclasse e' FastTransportable
  */
  private static void addSuperInvoke( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory, String method_name, String method_signature, Type arg_type )
  {
    String class_name = cg.getJavaClass().getClassName();
    String superclass_name = cg.getJavaClass().getSuperclassName();
    JavaClass superclass_jclass = Repository.lookupClass( superclass_name );

    if ( superclass_jclass.instanceOf( TypeRepository.FastTransportable_jclass) )
    {
      Method[] method_list = cg.getMethods();
      int i;

      for ( i=0; i<method_list.length; ++i)
      {
	if (method_list[i].getName().equals(method_name) && method_list[i].getSignature().equals(method_signature) )
	  break;
      }

      if ( i<method_list.length )
      {
	InvokeInstruction invoke_op = factory.createInvoke( superclass_jclass.getClassName(), method_name, Type.VOID, new Type[] { arg_type }, Constants.INVOKESPECIAL );

	MethodGen mg_method = new MethodGen( method_list[i], class_name, cpg );
	InstructionList il_method = mg_method.getInstructionList();

	Instruction[] ia = il_method.getInstructions();

	boolean insert = true;

	for ( int j=0; j<ia.length; ++j )
	  if ( ia[j].getTag()==Constants.INVOKESPECIAL)
	  {
	    InvokeInstruction invoke = (InvokeInstruction) ia[j];
	    if (invoke.getClassName(cpg).equals(superclass_name) && invoke.getMethodName(cpg).equals(method_name) )
	      insert = false;
	    else
	      insert = true;
	    break;
	  }

	if ( insert )
	{
	  cg.removeMethod( method_list[i] );

	  InstructionList il_patch = new InstructionList();

	  // aggiunge this sullo stack
	  il_patch.append( factory.createLoad( Type.OBJECT, 0 ) );

	  // aggiunge cis/cos sullo stack
	  il_patch.append( factory.createLoad( Type.OBJECT, 1 ) );

	  // ** le istruzioni sono inserite in ordine inverso
	  // ** in quanto stiamo inserendo in testa alla lista delle istruzioni

	  il_patch.append( invoke_op );

	  il_method.insert( il_patch );

	  // finalizing
	  mg_method.setMaxStack();
	  mg_method.setMaxLocals();
	  cg.addMethod( mg_method.getMethod() );

	  // garbage-collection
	  il_patch.dispose();
	}
      }
      else
      {
	log.info( "addSUperInvoke(): non trovato il metodo da modificare" );
      }
    }
  }

  private static void addSuperSizeOf( ClassGen cg, ConstantPoolGen cpg, InstructionFactory factory)
  {
    String class_name = cg.getJavaClass().getClassName();
    String superclass_name = cg.getJavaClass().getSuperclassName();
    JavaClass superclass_jclass = Repository.lookupClass( superclass_name );
    String method_signature = sizeof_signature;
    String method_name = "sizeOf";

    if ( superclass_jclass.instanceOf( TypeRepository.FastTransportable_jclass) )
    {
      Method[] method_list = cg.getMethods();
      int i;

      for ( i=0; i<method_list.length; ++i)
      {
	if (method_list[i].getName().equals(method_name) && method_list[i].getSignature().equals(method_signature) )
	  break;
      }

      if ( i<method_list.length )
      {
	MethodGen mg_method = new MethodGen( method_list[i], class_name, cpg );
	cg.removeMethod( method_list[i] );

	InstructionList il_method = mg_method.getInstructionList();

	InstructionList il_patch = new InstructionList();

	// aggiunge this sullo stack
	il_patch.append( factory.createLoad( Type.OBJECT, 0 ) );

	// ** le istruzioni sono inserite in ordine inverso
	// ** in quanto stiamo inserendo in testa alla lista delle istruzioni

	InvokeInstruction invoke_op = factory.createInvoke( superclass_jclass.getClassName(), method_name, Type.INT, Type.NO_ARGS, Constants.INVOKESPECIAL );
	il_patch.append( invoke_op );

//	PUSH push = new PUSH( cpg, 0 );
//	il_patch.append( push );
//	IADD iadd_op = new IADD();
//	il_patch.append( iadd_op );

	il_method.insert( il_patch );

	il_method.insert(il_method.getEnd(), new IADD() );

	// finalizing
	mg_method.setMaxStack();
	mg_method.setMaxLocals();
	cg.addMethod( mg_method.getMethod() );

	// garbage-collection
	il_patch.dispose();
      }
      else
      {
	log.info( "addSUperSizeof(): non trovato il metodo da modificare" );
      }
    }
  }


}


    /*
    // ok la superclasse ha il default constructor

    MethodGen mg_init;
    InstructionList il_init;

    il_init = new InstructionList();

    InvokeInstruction invoke_op;
    ReturnInstruction return_op;

//     * Stato dello stack
//     * 1)         this,                                                               TOP dello stack
    il_init.append( factory.createLoad( Type.OBJECT, 0 ) );

    // aggiunge l'invocazione

//    invoke_op = factory.createInvoke( superclass_jclass.getClassName(), "<init>", new ObjectType( superclass_jclass.getClassName() ), Type.NO_ARGS, Constants.INVOKESPECIAL );
    invoke_op = factory.createInvoke( superclass_jclass.getClassName(), "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL );
    il_init.append( invoke_op );

    return_op = factory.createReturn( Type.VOID );
    il_init.append( return_op );


//    invoke = factory.createInvoke( "foreign.Base", "<init>", Type.OBJECT, Type.NO_ARGS, Constants.INVOKESPECIAL );

    mg_init = new MethodGen( Constants.ACC_PUBLIC, Type.VOID,
			      Type.NO_ARGS, new String[0],
			      "<init>",  class_name,
			       il_init, cpg );

    mg_init.setMaxStack();
    mg_init.setMaxLocals();

    cg.addMethod( mg_init.getMethod() );

    il_init.dispose();
    return true;
*/






