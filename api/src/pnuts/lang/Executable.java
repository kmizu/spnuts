/*
 * @(#)Executable.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * Common interface for executable objects
 * 
 * Objects that represents parsed/compiled scripts implement this interface, so
 * that they can be executed by calling run(Context) method.
 */
public interface Executable {

	/**
	 * Executes the executable object;
	 * 
	 * @param context
	 *            the context in which the script is executed
	 * @return the result of the execution
	 */
	public Object run(Context context);
}