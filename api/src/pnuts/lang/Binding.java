/*
 * @(#)Binding.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

class Binding implements NamedValue, Cloneable, Serializable {

	String name;
	Object value;
	int hash;
	Binding chain;

	Binding(int h, String name, Object v, Binding n) {
		this.value = v;
		this.chain = n;
		this.name = name;
		this.hash = h;
	}

	public Object get() {
		return this.value;
	}

	public void set(Object value) {
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Deep copy
	 */
	protected Object clone() {
		try {
			Binding b = (Binding) super.clone();
			if (chain != null) {
				b.chain = (Binding) chain.clone();
			}
			return b;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		name = name.intern();
	}
}
