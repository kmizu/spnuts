/*
 * @(#)ResourceCache.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;
import java.lang.ref.*;

/**
 * Resource cache implemented with SoftReference.
 */
class ResourceCache {

	final static Object NULL = new Object();

	WeakHashMap resources = new WeakHashMap();

	/**
	 * Constructor
	 */
	public ResourceCache(){
	}

	/**
	 * Finds a resource associated with the specified key.
	 * Returns null if not found or already expired.
	 *
	 * @param key the key of the resource
	 * @return the resource or null
	 */
	protected synchronized Object findResource(Object key){
		if (key == null){
			key = NULL;
		}
		SoftReference ref = (SoftReference)resources.get(key);
		if (ref != null){
			return ref.get();
		} else {
			return null;
		}
	}

	/**
	 * Creates a new resource associated with the specified key.
	 *
	 * @param key the key of the resource
	 * @return a newly created resource
	 */
	protected Object createResource(Object key){
		return null;
	}

	/**
	 * Gets a resource associated with the specified key.
	 * If the resource has been expired, a new one is created.
	 *
	 * @param key the key of the resource
	 * @return a resource associated with the key
	 */
	public Object getResource(Object key){
		Object resource = findResource(key);
		if (resource == null){
			resource = createResource(key);
			if (key == null){
				key = NULL;
			}
			resources.put(key, new SoftReference(resource));
		}
		return resource;
	}

	/**
	 * Invalidate the resource associated with the specified key
	 */
       public synchronized void invalidate(Object key){
	   resources.remove(key);
	}

	/**
	 * Discard all cached resources
	 */
	public synchronized void reset(){
		resources.clear();
	}
}
