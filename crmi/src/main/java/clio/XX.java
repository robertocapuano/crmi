/* 
 * Copyright (c) 2014, Roberto Capuano <roberto@2think.it> Capuano <Roberto Capuano <roberto@2think.it>@2think.it>
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

public class XX extends Exception {

  public XX() {

 	/**
	 * 2) new field_type
	 * Stack: this, ref
	 *              reference al nuovo oggetto
	 *
	 */

	il_read.append( factory.createNew( (ObjectType) field_type ) );
/**
 * 3) dup del reference
 * Stack: this, ref, ref
 */

	il_read.append( factory.createDup( 1 ) ); // duplica il reference dell'oggetto per la putfield

/**
 * 4) costruttore consuma solo il reference
 * Stack: this, ref
 */
	il_read.append( factory.createInvoke( field_type.toString(), "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL ) );

/**
 * 5) putfield nella nostra istanza di classe
 * Stack:
 */
	put_field_op = factory.createPutField( field_type.toString(), field_name, field_type );
	il_read.append( put_field_op );

/**
 * 6) // load del reference this per la successiva operazione: putfield
 * Stack: this
 */

	il_read.append( factory.createLoad( Type.OBJECT, 0 ) );

/**
 * 7) Carica il reference appena inizializzato sullo stack
 * Stack:
 */
	get_field_op = factory.createGetField( field_type.toString(), field_name, field_type );
	il_read.append( get_field_op );

/**
 * 8) carica il primo argomento nello stack: ref
 * Stack: ref
 */

/**
 * 9) Passa l'argomento della readExternal come primo argomento della ref.readExternal
 * Stack: ref, in
 */
	il_read.append( factory.createLoad( Type.OBJECT, 1 ) );

/**
 * 10) Esegue: ref.readExternal(in);
 * Stack:
 */
	invoke_op = factory.createInvoke( "java.io.ObjectInput", input_method, input_type, Type.NO_ARGS, Constants.INVOKEINTERFACE );
	il_read.append( invoke_op );


  }
}