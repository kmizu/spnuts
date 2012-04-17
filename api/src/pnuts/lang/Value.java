/*
 * @(#)Value.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * Objects of this class are returned by Package.lookup() method.
 * 
 * @see pnuts.lang.Package
 */
public interface Value {

	/**
	 * Gets the value.
	 */
	public Object get();
}