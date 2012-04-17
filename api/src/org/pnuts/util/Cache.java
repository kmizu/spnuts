/*
 * @(#)Cache.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

/**
 * Common interface for cache
 */
public interface Cache {

	/**
	 * If key is in the cache it returns value, otherwise null.
	 * 
	 * @param key
	 *            the key
	 */
	public Object get(Object key);

	/**
	 * Register key and its value into the cache.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value for the key
	 * @return the old value
	 */
	public Object put(Object key, Object value);

	/**
	 * Clear all cached data
	 */
	public void reset();
}