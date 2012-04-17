/*
 * @(#)ImmutableBinding.java 1.2 04/12/06
 * 
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

class ImmutableBinding extends Binding {

	ImmutableBinding(int h, String name, Object v, Binding n) {
		super(h, name, v, n);
	}

	/**
	 * Always throws IllegalStateException
	 */
	public void set(Object value) {
		throw new IllegalStateException();
	}
}