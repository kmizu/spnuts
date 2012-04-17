/*
 * @(#)AbstractData.java 1.2 04/12/06
 *
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * Method call of an object implements this interface causes a call of the <tt>invoke()</tt> method.
 *
 * See <a href="http://pnuts.org/doc/lang.html#sugar">Pnuts Language Specification</a>.
 *
 * @version	1.1
 */
public interface AbstractData extends Property {

	/**
	 * Defines the behavior of a method call.
	 *
	 * @param name the method name
	 * @param args the arguments
	 * @param context the context in which the method is called.
	 * @return the result of the method call
	 */
	public Object invoke(String name, java.lang.Object[] args, Context context);
}
