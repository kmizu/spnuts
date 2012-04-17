/*
 * @(#)AutoloadHook.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * This interface defines how to find the value of a undefined variable.
 * Instances of this interface can be registered with
 * <tt>Package.autoload(String name, AutoloadHook loadHook, Context context)</tt>.
 * 
 * @version 1.1
 */
public interface AutoloadHook {

	/**
	 * When a registered name is first accessed and the name has not been
	 * defined, this method of a corresponding AutoloadHook object is called.
	 * 
	 * @param name
	 *            the name to be defined by autoloading
	 * @param context
	 *            the context with which the name is accessed.
	 */
	public void load(String name, Context context);
}