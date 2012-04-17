/*
 * ServletParameter.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.AbstractSet;
import org.pnuts.net.URLEncoding;
import pnuts.lang.Pnuts;

class ServletParameter implements Map {
	private Map map;

	public ServletParameter(Map map){
		this.map = map;
	}

	public int size(){
		return map.size();
	}
	
	public boolean isEmpty(){
		return map.isEmpty();
	}

	public boolean containsKey(Object key){
		return map.containsKey(key);
	}

	public boolean containsValue(Object value){
		throw new UnsupportedOperationException();
	}

	public Set keySet() {
		return map.keySet();
	}

	public Set entrySet(){
	    return new ParameterSet(map.entrySet());
	}

	public Object[] getAll(String name) {
		return (Object[])map.get(name);
	}

	public Object get(Object name){
		return get(name, null);
	}

	public Object get(Object name, String defaultValue){
		Object[] array = (Object[])map.get(name);
		if (array == null){
			return defaultValue;
		} else {
			return array[0];
		}
	}

	public Object put(Object key, Object value){
		if (value instanceof Object[]){
			return map.put(key, (Object[])value);
		} else {
			return map.put(key, new Object[]{value});
		}
	}
	
	public void putAll(Map t){
		map.putAll(t);
	}

	public Object remove(Object key){
		return map.remove(key);
	}

	public void clear(){
		map.clear();
	}

	public Collection values(){
	    return new ValueSet(map.values());
	}

	public void copyInto(Map m){
		Iterator it = map.keySet().iterator();
		if (it.hasNext()){
			Object key = it.next();
			Object value = map.get(key);
			if (value instanceof Object[]){
				value = ((Object[])value)[0];
			}
			m.put(key, value);
		}
	}

	public String toQueryString(String encoding) throws UnsupportedEncodingException {
		StringBuffer sbuf = new StringBuffer();
		Iterator it = map.entrySet().iterator();
		boolean first = true;
		if (it.hasNext()){
			Map.Entry entry = (Map.Entry)it.next();
			String key = (String)entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Object[]){
				Object[] array = (Object[])value;
				for (int i = 0; i < array.length; i++){
					if (!first){
						sbuf.append('&');
						first = false;
					}
					sbuf.append(URLEncoding.encode(key, encoding));
					sbuf.append('=');
					sbuf.append(URLEncoding.encode((String)array[i], encoding));
				}
			} else {
				first = false;
				sbuf.append(URLEncoding.encode(key, encoding));
				sbuf.append('=');
				sbuf.append(URLEncoding.encode((String)value, encoding));
			}
		}
		while (it.hasNext()){
			Map.Entry entry = (Map.Entry)it.next();
			String key = (String)entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Object[]){
				Object[] array = (Object[])value;
				for (int i = 0; i < array.length; i++){
					sbuf.append('&');
					sbuf.append(URLEncoding.encode(key, encoding));
					sbuf.append('=');
					sbuf.append(URLEncoding.encode((String)array[i], encoding));
				}
			} else {
				sbuf.append('&');
				sbuf.append(URLEncoding.encode(key, encoding));
				sbuf.append('=');
				sbuf.append(URLEncoding.encode((String)value, encoding));
			}
		}
		return sbuf.toString();
	}

	public String toString(){
		StringBuffer sbuf = new StringBuffer();
		sbuf.append('{');
		Iterator it = map.keySet().iterator();
		Object key, value;
		if (it.hasNext()){
			sbuf.append(key = it.next());
			sbuf.append('=');
			sbuf.append(Pnuts.format(map.get(key)));
		}
		while (it.hasNext()){
			sbuf.append(',');
			sbuf.append(key = it.next());
			sbuf.append('=');
			sbuf.append(Pnuts.format(map.get(key)));
		}
		sbuf.append('}');
		return sbuf.toString();
	}

	static class ValueSet extends AbstractSet {
		private Collection col;

		ValueSet(Collection col){
		    this.col = col;
		}

		public Iterator iterator(){
		    return new ParameterIterator(this.col.iterator(), 1);
		}

		public int size(){
		    return col.size();
		}
	}

	static class ParameterSet extends AbstractSet {
		private Set set;

		ParameterSet(Set set){
		    this.set = set;
		}

		public Iterator iterator(){
		    return new ParameterIterator(this.set.iterator(), 0);
		}

		public int size(){
		    return set.size();
		}
	}

	static class ParameterIterator implements Iterator {
		private Iterator it;
		private ParameterEntry entry;
		private int type; // 0==Entry,1==Value,2==Key

		ParameterIterator(Iterator it, int type){
			this.it = it;
			this.type = type;
			if (type == 0){
			    this.entry = new ParameterEntry();
			}
		}

		public boolean hasNext(){
	    		return it.hasNext();
		}

		public Object next(){
		    if (type == 0){
			Map.Entry n = (Map.Entry)it.next();
			entry.attach(n);
			return entry;
		    } else if (type == 1){
			Object[] n = (Object[])it.next();
			if (n != null && n.length > 0){
			    return n[0];
			} else {
			    return null;
			}
		    } else {
			return it.next();
		    }
		}

		public void remove(){
			this.it.remove();
		}
	}

	static class ParameterEntry implements Map.Entry {
		private Map.Entry entry;

		ParameterEntry(){
		}

		void attach(Map.Entry entry){
			this.entry = entry;
		}

		public Object getKey(){
			return this.entry.getKey();
		}

		public Object getValue(){
			return ((Object[])entry.getValue())[0];
		}

		public Object[] getValues(){
			return (Object[])entry.getValue();
		}

		public Object setValue(Object value){
			return (Object[])entry.setValue(new Object[]{value});
		}

		public int hashCode(){
		    return entry.getKey().hashCode() ^ entry.getValue().hashCode();
		}

		public boolean equals(Object obj){
		    if (obj instanceof ParameterEntry){
			ParameterEntry that = (ParameterEntry)obj;
			Object k1 = getKey();
			Object k2 = that.getKey();
			Object v1 = getValue();
			Object v2 = that.getValue();
			return (k1 == k2 || (k1 != null && k1.equals(k2))) && (v1 == v2 || (v1 != null && v1.equals(v2)));
		    } else {
			return false;
		    }
		}
	}
}
