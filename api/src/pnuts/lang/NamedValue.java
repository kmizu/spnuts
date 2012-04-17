/*
 * @(#)NamedValue.java 1.3 05/02/17
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * Objects of this class are returned by Package.lookup() method.
 */
public interface NamedValue extends Value {

	/**
	 * Gets the name of the value.
	 * 
	 * @return the intern()'d String that identifies the value
	 */
	public String getName();


	/**
	 * Sets the value
	 *
	 * @param obj the new value
	 */
	public void set(Object obj);
}
