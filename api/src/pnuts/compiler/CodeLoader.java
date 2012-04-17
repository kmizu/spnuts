/*
 * @(#)CodeLoader.java 1.2 04/12/06 
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

/**
 * This class is used with JDK1.2 or higher, to define classes from bytecode.
 * The resulting class is in the same protection domain of
 * pnuts.compiler.CodeLoader class.
 */
class CodeLoader extends ClassLoader {

	private final static boolean DEBUG = false;

	private int count = 0;

	private ProtectionDomain domain;

	CodeLoader() {
		init();
	}

	CodeLoader(ClassLoader loader) {
		super(loader);
		init();
	}

	void init() {
		this.domain = getClass().getProtectionDomain();
	}

	protected Class findClass(String name) throws ClassNotFoundException {

		ClassLoader parent = (ClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return getParent();
					}
				});
		try {
			if (parent != null) {
				return parent.loadClass(name);
			}
		} catch (ClassNotFoundException e) {
		}
		return findSystemClass(name);
	}

	/**
	 * Resolve a class
	 */
	void resolve(Class c) {
		try {
			resolveClass(c);
		} catch (VerifyError ve) {
			throw ve;
		}
	}

	/**
	 * Defines a class from a byte code array. This method can be called until
	 * seal() method is called,
	 */
	Class define(final String name, final byte data[], final int offset,
			final int length) {
		if (DEBUG) {
			//	    System.out.println(name + ", " + data.length + ", " + offset + ",
			// " + length);
			try {
				FileOutputStream fout = new FileOutputStream(name + ".class");
				fout.write(data, offset, length);
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return (Class) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return defineClass(name, data, offset, length, domain);
			}
		});
	}

	synchronized int nextCount() {
		return count++;
	}
}