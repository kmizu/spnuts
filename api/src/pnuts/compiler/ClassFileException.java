/*
 * @(#)ClassFileException.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

public class ClassFileException extends RuntimeException {
	public ClassFileException() {
	}

	public ClassFileException(String msg) {
		super(msg);
	}
}
