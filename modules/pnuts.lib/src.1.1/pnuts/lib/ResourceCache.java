/*
 * @(#)ResourceCache.java 1.2 04/12/06
 *
 * Copyright (c) 2002,2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib;

import java.util.*;
import org.pnuts.util.LRUCache;

/**
 * Resource cache implemented with LRU cache
 */
class ResourceCache {

	final static Object NULL = new Object();

	LRUCache resources = new LRUCache(64);

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
	protected Object findResource(Object key){
		if (key == null){
			key = NULL;
		}
		return resources.get(key);
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
			resources.put(key, resource);
		}
		return resource;
	}

	/**
	 * Discard all cached resources
	 */
	public void reset(){
		resources.reset();
	}
}
