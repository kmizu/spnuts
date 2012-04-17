/*
 * @(#)SessionMap.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import java.util.*;
import javax.servlet.http.*;

/**
 * Adapter from HttpSession to java.util.Map
 */
class SessionMap implements Map {

	private HttpSession session;

	public SessionMap(HttpSession session){
		this.session = session;
	}

	/**
	 * Gets the number of session attributes
	 */
	public int size(){
		if (session == null){
			return 0;
		}
		int count = 0;
		for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
			count++;
		}
		return count;
	}

	/**
	 * Checks if the session has any attribute
	 */
	public boolean isEmpty(){
		if (session == null){
			return true;
		}
		for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
			return false;
		}
		return true;
	}

	/**
	 * Checks if the session has any attribute with the key
	 */
	public boolean containsKey(Object key){
		if (session == null){
			return false;
		}
		return session.getAttribute((String)key) != null;
	}

	/**
	 * Checks if the session has any attribute with the value
	 */
	public boolean containsValue(Object value){
		if (session == null){
			return false;
		}
		for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
			if (value.equals(get(e.nextElement()))){
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the value of specified session attribute
	 */
	public Object get(Object key){
		if (session == null){
			return null;
		}
		return session.getAttribute((String)key);
	}

	/**
	 * Sets the value of specified session attribute
	 */
	public Object put(Object key, Object value){
		if (session == null){
			return null;
		}
		session.setAttribute((String)key, value);
		return null;
	}

	/**
	 * Removes the specified session attribute
	 */
	public Object remove(Object key){
		if (session == null){
			return null;
		}
		Object result = get(key);
		session.removeAttribute((String)key);
		return result;
	}

	/**
	 * Sets a series of attributes to the session
	 */
	public void putAll(Map t){
		if (session == null){
			return;
		}
		for (Iterator it = t.entrySet().iterator(); it.hasNext(); ){
			Map.Entry entry = (Map.Entry)it.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Removes all session attributes
	 */
	public void clear(){
		if (session == null){
			return;
		}
		for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
			remove((String)e.nextElement());
		}
	}

	public Set keySet(){
		HashSet set = new HashSet();
		if (session != null){
			for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
				set.add(e.nextElement());
			}
		}
		return set;
	}

	/**
	 * Collects the values of the session attributes and return them as a List
	 */
	public Collection values(){
		ArrayList list = new ArrayList();
		if (session != null){
			for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
				list.add(get(e.nextElement()));
			}
		}
		return list;
	}

	/**
	 * Collects the session attributes and return them as a Set of Map.Entry objects.
	 */
	public Set entrySet(){
		HashSet set = new HashSet();
		if (session != null){
			for (Enumeration e = session.getAttributeNames(); e.hasMoreElements(); ){
				String name = (String)e.nextElement();
				set.add(new Entry(name, get(name)));
			}
		}
		return set;
	}

	static class Entry implements Map.Entry {
		String attr;
		Object value;
		Entry(String attr, Object value){
			this.attr = attr;
			this.value = value;
		}
		public Object getKey(){
			return attr;
		}
		public Object getValue(){
			return value;
		}

		public Object setValue(Object value){
			Object r = this.value;
			this.value = value;
			return r;
		}

		public int hashCode(){
			return attr.hashCode();
		}

		public boolean equals(Object that){
			if (that instanceof Entry){
				Entry e = (Entry)that;
				return this.attr.equals(e.attr) && this.value.equals(e.value);
			}
			return false;
		}
	}
}
