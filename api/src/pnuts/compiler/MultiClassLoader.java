/*
 * @(#)MultiClassLoader.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.util.Stack;

public class MultiClassLoader extends ClassLoader {

	private ClassLoader[] loaders;

	public MultiClassLoader(ClassLoader cl1, ClassLoader cl2) {
		this(cl1, new ClassLoader[] { cl2 });
	}

	public MultiClassLoader(ClassLoader parent, ClassLoader[] cl) {
		super(parent);
		this.loaders = init(cl);
	}

	ClassLoader[] init(ClassLoader[] base) {
		if (base == null || base.length < 1) {
			return null;
		}
		Stack v = new Stack();
		ClassLoader cl0 = base[0];
		Class c0 = cl0.getClass();
		v.push(cl0);
		for (int i = 1; i < base.length; i++) {
			ClassLoader cl = base[i];
			Class c = cl.getClass();
			if (c != c0) {
				if (c.isInstance(cl0)) {
					v.pop();
					v.push(cl);
				} else if (!c0.isInstance(cl)) {
					v.push(cl);
				}
			}
			cl0 = cl;
			c0 = c;
		}
		ClassLoader[] ret = new ClassLoader[v.size()];
		v.copyInto(ret);
		return ret;
	}

	protected Class findClass(final String name) throws ClassNotFoundException {
		if (loaders != null) {
			for (int i = 0; i < loaders.length; i++) {
				ClassLoader cl = loaders[i];
				try {
					return cl.loadClass(name);
				} catch (ClassNotFoundException e) {
				}
			}
		}
		throw new ClassNotFoundException(name);
	}
}