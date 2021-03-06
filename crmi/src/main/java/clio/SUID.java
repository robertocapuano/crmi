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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.io.*;

import java.util.*;

import com.borland.primetime.util.Debug;

import de.fub.bytecode.*;
import de.fub.bytecode.classfile.*;
import de.fub.bytecode.generic.*;


/**
 * Il SUID calcolato mediante questa funzione e' diverso dal valore:
 * @code java.io.ObjectStreamClass.lookup( X1.class ).getSerialVersionUID(). In quanto le librerie JavaClass e Reflect funzionano in maniera diversa.
 * java.lang.reflect funziona sulla classe gia' linkata, quindi:
 * 1) risolvendo i riferimenti multipli ad interfaccie implementate.
 *    es.: implements Transportable, FastTransportable diventa --> FastTransportable
 * 2) contiene tutti i campi i metodi ereditati.
 * 3) contiene tutti i metodi ereditati.
 *
 * JavaClass opera sul singolo file .class
 *
 * Inoltre JavaClass
 *
 */
public class SUID {
    private final static ClusterClassLoader ccl = ClusterClassLoader.getClusterClassLoader();

    private static Comparator compareByName = new CompareByName();

    private static class CompareByName implements Comparator
    {
	public int compare(Object o1, Object o2) {
	    de.fub.bytecode.classfile.FieldOrMethod c1 = (de.fub.bytecode.classfile.FieldOrMethod) o1;
	    de.fub.bytecode.classfile.FieldOrMethod c2 = (de.fub.bytecode.classfile.FieldOrMethod) o2;

	    return (c1.getName()).compareTo(c2.getName());
	}
    }

    public final static long getSUID( Class type )
    {
      long SUID = 0;

      if (!type.isArray())
      {
	try { SUID = type.getField( "SUID" ).getLong( null ); }
	catch (Exception reflect_exception)
	{
//	   throw new SerializationException( reflect_exception.getMessage() );
	}
      }
      else
      {
	SUID = clio.SUID.computeSUID( type );
//	SUID = ObjectStreamClass.lookup(type).getSerialVersionUID();
      }

      return SUID;
    }

    private static long computeSUID( Class cl )
//    public static long computeSUID( Class cl )
    {
      if ( cl.isArray() )
	return java.io.ObjectStreamClass.lookup( cl ).getSerialVersionUID();
      else
      {
	String class_name = cl.getName();
        return computeSUID( class_name );
      }
    }

    public static long computeSUID( String class_name )
    {
      JavaClass j_cl = ccl.loadFromUserRepository( class_name );
      if (j_cl==null)
      {
	j_cl = Repository.lookupClass( class_name );
      }

      if (j_cl!=null)
        return computeSUID( j_cl );
      else
	return 0;
    }

    /**
     * L'algoritmo e' il medesimo specificato sulla RMI specification.
     * Il risultato di computeSUID() e' diverso da quello di ObjectStreamClass
     * in quanto quest'ultimo usa la reflection. Mentre computeSUID() BCEL.
     * Il package java.lang.reflect opera su classi i cui membri (campi e metodi)
     * sono tutti quelli che la classe ha ereditato. Mentre JavaClass opera sulla classe cosi'
     * come viene letta dal file, senza i campi ereditati.
     */
    static long computeSUID( JavaClass j_cl )
    {

	ByteArrayOutputStream devnull = new ByteArrayOutputStream(512);

	long h = 0;
	try {
	    MessageDigest md = MessageDigest.getInstance("SHA");
	    DigestOutputStream mdo = new DigestOutputStream(devnull, md);
	    DataOutputStream data = new DataOutputStream(mdo);
	    String classname = j_cl.getClassName();
	    data.writeUTF( classname );

	    int classaccess = j_cl.getAccessFlags();
	    classaccess &= (Constants.ACC_PUBLIC | Constants.ACC_FINAL |
			    Constants.ACC_INTERFACE | Constants.ACC_ABSTRACT);

	    Method[] method = j_cl.getMethods();
	    Arrays.sort(method, compareByName);
	    if ((classaccess & Constants.ACC_INTERFACE) != 0) {
		classaccess &= (~Constants.ACC_ABSTRACT);
		if (method.length > 0) {
		    classaccess |= Constants.ACC_ABSTRACT;
		}
	    }

	    data.writeInt(classaccess);

	      if ( j_cl.isClass() )
	     //	    if (!cl.isArray())
	      {
		 String[] interfaces_name = j_cl.getInterfaceNames();
		Arrays.sort(interfaces_name );

		for (int i = 0; i < interfaces_name.length; i++) {
		    data.writeUTF(interfaces_name[i]);
		}
	      }

	    Field[] field = j_cl.getFields();
	    Arrays.sort(field, compareByName);

	    for (int i = 0; i < field.length; i++) {
		Field f = field[i];
		if (f.isPrivate() || f.isStatic() || f.isTransient() )
		    continue;
  		data.writeUTF(f.getName());
		data.writeInt(f.getAccessFlags());
		data.writeUTF(f.getSignature() );
	    }

	    for ( int i=0; i<method.length; ++i )
	    {
	      if (method[i].getName().equals("<clinit>") && method[i].getSignature().equals( "()V" ) )
	      {
		  data.writeUTF("<clinit>");
		  data.writeInt(Constants.ACC_STATIC); // TBD: what modifiers does it have
		  data.writeUTF("()V");
	      }
	    }

	    for (int i = 0; i <method.length; i++)
	    {
		if (method[i].getName().equals("<init>") )
		{
		  data.writeUTF( method[i].getName() );
		  data.writeInt( method[i].getAccessFlags() );
		  data.writeUTF( method[i].getSignature() );
		}
	    }

	    for (int i = 0; i <method.length; i++)
	    {
		if ( !method[i].isPrivate() && !method[i].isStatic() )
		{
		  String n = method[i].getName();
		  data.writeUTF( method[i].getName() );
		  data.writeInt( method[i].getAccessFlags() );
		  String sig = method[i].getSignature();
		  data.writeUTF( method[i].getSignature() );
		}
	    }


	    /* Compute the hash value for this class.
	     * Use only the first 64 bits of the hash.
	     */
	    data.flush();
	    byte hasharray[] = md.digest();
	    for (int i = 0; i < Math.min(8, hasharray.length); i++) {
		h += (long)(hasharray[i] & 255) << (i * 8);
	    }
	} catch (IOException ignore) {
	    /* can't happen, but be deterministic anyway. */
	    h = -1;
	} catch (NoSuchAlgorithmException complain) {
	    throw new SecurityException(complain.getMessage());
	}
	return h;
    }

}

