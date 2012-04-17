/*
 * @(#)memcache.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.util.*;
import java.lang.ref.*;

/*
 * function memcache(func)
 */
public class memcache extends PnutsFunction {

	static Object[] NO_ARGS = new Object[]{};

	public memcache(){
		super("memcache");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		PnutsFunction handler = (PnutsFunction)args[0];
		Context ctx = (Context)context.clone();
		if (handler.defined(0)){
			return new MemoryCache(handler.call(NO_ARGS, ctx), handler, ctx);
		} else if (handler.defined(-1)){
			return new MemoryCache(handler.call(NO_ARGS, ctx), handler, ctx);
		} else if (handler.defined(1)){
			return new MemoryCache(null, handler, ctx);
		} else {
			throw new IllegalArgumentException();
		}
	}

	public String toString(){
		return "function memcache(func()) or (func(key))";
	}


	static class MemoryCache extends PnutsFunction implements Map {
		SoftReference ref;
		ResourceCache cache;
		Context context;
		PnutsFunction handler;

		MemoryCache(Object initial, final PnutsFunction handler, final Context context){
			this.handler = handler;
			this.context = context;
			if (initial != null){
				this.ref = new SoftReference(initial);
			}
			this.cache = new ResourceCache(){
					protected Object createResource(Object key){
						return handler.call(new Object[]{key}, context);
					}
				};
		}

		public boolean defined(int nargs){
			return nargs == 0;
		}

		protected Object exec(Object[] args, Context context){
			if (args.length != 0){
				undefined(args, context);
			}
			Object value;
			synchronized (this){
			    if (ref == null || (value = ref.get()) == null){
				value = handler.call(NO_ARGS, this.context);
				this.ref = new SoftReference(value);
			    }
			}
			return value;
		}
	
		/**
		 * Gets a resource associated with the specified key.
		 * If the resource has been expired, a new one is created.
		 *
		 * @param name the key of the resource
		 * @return a resource associated with the key
		 */
		public Object get(Object key){
			return cache.getResource(key);
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 *
		 * @param name the key of the resource
		 * @param value the value of the resource
		 */
		public Object put(Object key, Object value){
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public int size(){
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public boolean isEmpty(){
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public Object remove(Object key){
		    throw new UnsupportedOperationException();
		}

		public void invalidate(Object key){
		    cache.invalidate(key);
		}

		public synchronized void invalidate(){
		    ref = null;
		}


		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public void putAll(Map t){
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public void clear(){
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public Set keySet(){
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public Collection values(){
			throw new UnsupportedOperationException();
		}

		/**
		 * Throws an exception, since this operation is not supported in this class.
		 */
		public Set entrySet(){
			throw new UnsupportedOperationException();
		}
	}
}
