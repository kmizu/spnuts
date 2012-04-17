/*
 * @(#)Indexed.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * Index-access to an instance of this interface is interpreted as the set/get
 * method call, which are defined in the implementation class.
 * 
 * <pre>
 * 
 *  indexed[idx]          ==&gt; indexed.get(idx)
 *  indexed[idx] = value  ==&gt; indexed.set(idx, value)
 *  
 * </pre>
 */
public interface Indexed {

	/**
	 * Write access to the index
	 * 
	 * @param idx
	 *            the index
	 * @param value
	 *            the object to be assigned
	 */
	public void set(int idx, Object value);

	/**
	 * Read access to the index
	 * 
	 * @param idx
	 *            the index
	 */
	public Object get(int idx);
}