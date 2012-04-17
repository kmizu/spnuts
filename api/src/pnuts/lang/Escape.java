/*
 * @(#)Escape.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * This class is a special Exception class in a Pnuts runtime in that it's not
 * checked by exception handlers. This class is used to implement quit()
 * function.
 * 
 * @see pnuts.lang.Jump
 */
public class Escape extends RuntimeException {
	private Object value;

	protected Escape() {
	}

	protected Escape(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}
}