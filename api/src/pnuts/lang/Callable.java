/*
 * @(#)Callable.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * Callable object can be the target of function call expression.
 * 
 * <pre>
 * 
 *    callable(arg1, arg2, ...)
 *  
 * </pre>
 */
public interface Callable {
	/**
	 * Executes the callable object
	 * 
	 * @param args
	 *            the arguments
	 * @param context
	 *            the context in which the object is called
	 * @return the result of the call
	 */
	public Object call(Object[] args, Context context);
}