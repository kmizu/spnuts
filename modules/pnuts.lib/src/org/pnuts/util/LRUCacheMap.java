/*
 * LRUCacheMap.java
 *
 * Copyright (c) 2004,2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import java.util.*;
import org.pnuts.util.LRUCache;

public abstract class LRUCacheMap extends LRUCache implements Map {

	private static Object NULL_OBJECT = new Object();

	public LRUCacheMap(int max){
		super(max);
	}

	public synchronized Object get(Object key){
		Object k;
		if (key == null){
			k = NULL_OBJECT;
		} else {
			k = key;
		}
		Object value = super.get(k);
		if (value == null){
			value = construct(key);
			super.put(k, value);
		}
		return value;
	}

	/**
	 * Called when the cache entry has been expired
	 *
	 * @param key the key
	 * @return the value for the key
	 */
	protected abstract Object construct(Object key);


	/**
	 * Throws an exception, since this operation is not supported in this class.
	 */
	public boolean isEmpty(){
		return size() == 0;
	}

	/**
	 * Throws an exception, since this operation is not supported in this class.
	 */
	public boolean containsKey(Object key){
		throw new UnsupportedOperationException();
	}

	/**
	 * Throws an exception, since this operation is not supported in this class.
	 */
	public boolean containsValue(Object value){
		return values().contains(value);
	}

	/**
	 * Throws an exception, since this operation is not supported in this class.
	 */
	public Object remove(Object key){
		throw new UnsupportedOperationException();
	}

	/**
	 * Throws an exception, since this operation is not supported in this class.
	 */
	public void putAll(Map t){
		for (Iterator it = t.entrySet().iterator(); it.hasNext(); ){
			Map.Entry entry = (Map.Entry)it.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Throws an exception, since this operation is not supported in this class.
	 */
	public void clear(){
		reset();
	}
}
