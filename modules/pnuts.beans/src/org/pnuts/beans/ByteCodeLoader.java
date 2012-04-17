/*
 * @(#)ByteCodeLoader.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

class ByteCodeLoader extends ClassLoader {

	ByteCodeLoader(){
	}

	ByteCodeLoader(ClassLoader parent){
		super(parent);
	}

	Class define(String cname, byte[] bytecode, int offset, int size){
		Class c = defineClass(cname, bytecode, offset, size);
		resolveClass(c);
		return c;
	}
}
