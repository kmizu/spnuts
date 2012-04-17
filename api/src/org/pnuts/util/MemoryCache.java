/*
 * @(#)MemoryCache.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import pnuts.lang.Runtime;
import java.lang.ref.SoftReference;
import java.util.WeakHashMap;
import java.util.Map;

/**
 * A Cache implementation that uses SoftReference.
 */
public class MemoryCache implements Cache {

	private Map map;

	public MemoryCache() {
	    this(Runtime.createWeakMap());
	}

	public MemoryCache(Map map) {
	    this.map = map;
	}

	public Object get(Object key) {
		SoftReference ref = (SoftReference) map.get(key);
		if (ref == null) {
			return null;
		} else {
			return ref.get();
		}
	}

	public Object put(Object key, Object value) {
		map.put(key, new SoftReference(value));
		return null;
	}

	public void reset() {
		this.map = new WeakHashMap();
	}
}
