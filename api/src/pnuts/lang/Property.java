/*
 * @(#)Property.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * In Pnuts, access to a property of an object implements this interface causes
 * a call of methods in this interface. See <a
 * href="http://pnuts.org/doc/lang.html#sugar">Pnuts Language Specification </a>
 * for details.
 * 
 * @version 1.1
 * @author Toyokazu Tomatsu
 */
public interface Property {
	/**
	 * This method defines the behavior of the following expression.
	 * 
	 * <pre>
	 *    <em>
	 * aProperty
	 * </em>
	 *  . 
	 * <em>
	 * name
	 * </em>
	 *  = 
	 * <em>
	 * value
	 * </em>
	 * </pre>
	 * 
	 * @param name
	 *            the <em>name</em>.
	 * @param value
	 *            the <em>value</em>.
	 * @param context
	 *            the context in which the expression is evaluated.
	 */
	void set(String name, Object value, Context context);

	/**
	 * This method defines the behavior of the following expression.
	 * <p>
	 * 
	 * <pre>
	 *  <em>
	 * aProperty
	 * </em>
	 *  . 
	 * <em>
	 * name
	 * </em>
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param name
	 *            the <em>name</em>.
	 * @param context
	 *            the context in which the expression is evaluated.
	 */
	Object get(String name, Context context);
}