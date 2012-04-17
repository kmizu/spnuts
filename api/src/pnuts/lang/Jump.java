/*
 * @(#)Jump.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * This class is a special Exception class in a Pnuts runtime in that it's not
 * checked by exception handlers. This class is used to implement handled
 * exceptions by catch() function and return() function.
 */
public class Jump extends Escape {

	protected Jump() {
	}

	public Jump(Object value) {
		super(value);
	}
}