/*
 * FilterMap
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;

class FilterMap extends AbstractMap {
	private Map m;
	boolean dualArgs;

	protected FilterMap(){
	}

	public FilterMap(Map m, boolean dualArgs){
		this.m = m;
		this.dualArgs = dualArgs;
	}

	protected boolean shouldInclude(Object key){
		return true;
	}

	protected boolean shouldInclude(Object key, Object value){
		return true;
	}

	public Object get(Object key){
		Object value = m.get(key);
		if ((dualArgs && shouldInclude(key, value)) || shouldInclude(key)){
			return value;
		} else {
			return null;
		}
	}

	public boolean containsKey(Object key){
	    if (dualArgs){
		return (m.containsKey(key) && shouldInclude(key, get(key)));
	    } else {
		return (m.containsKey(key) && shouldInclude(key));
	    }
	}

	public Object put(Object key, Object value){
		throw new UnsupportedOperationException();
	}

	public void putAll(Map m) {
		throw new UnsupportedOperationException();
	}

	public Object remove(Object obj){
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public Set keySet(){
		return new FilterSet(m.keySet());
	}

	public Set entrySet(){
		return new EntrySet(m.entrySet());
	}

	public Collection values(){
	    return new AbstractCollection(){
		    public Iterator iterator(){
			return new ProjectionIterator(entrySet().iterator()){
				protected Object project(Object obj){
				    Map.Entry entry = (Map.Entry)obj;
				    return entry.getValue();
				}
			    };
		    }
		    public int size(){
			return entrySet().size();
		    }
		};
	}

	final class FilterSet extends AbstractSet {
		private Set set;

		FilterSet(Set set){
			this.set = set;
		}

		public Iterator iterator() {
		    if (dualArgs){
			return new FilterIterator(set.iterator()){
				protected boolean shouldInclude(Object element){
					Object value = FilterMap.this.get(element);
					return FilterMap.this.shouldInclude(element, value);
				}
			    };
		    } else {
			return new FilterIterator(set.iterator()){
				protected boolean shouldInclude(Object element){
					return FilterMap.this.shouldInclude(element);
				}
			    };
		    }
		}

		public int size(){
			// scan
			int c = 0;
			for (Iterator it = iterator(); it.hasNext(); it.next()){
				c++;
			}
			return c;
		}
	}

	private final class EntrySet extends AbstractSet {
		Set entrySet;

		EntrySet(Set entrySet){
			this.entrySet = entrySet;
		}

		public Iterator iterator() {
		    if (dualArgs){
			return new FilterIterator(entrySet.iterator()){
				protected boolean shouldInclude(Object element){
				    Map.Entry entry = (Map.Entry)element;
				    return FilterMap.this.shouldInclude(entry.getKey(), entry.getValue());
				}
			    };
		    } else {
			return new FilterIterator(entrySet.iterator()){
				protected boolean shouldInclude(Object element){
				    Map.Entry entry = (Map.Entry)element;
				    return FilterMap.this.shouldInclude(entry.getKey());
				}
			    };
		    }
		}

		public boolean contains(Object o) {
			if (o instanceof Map.Entry){
				Map.Entry entry = (Map.Entry)o;
				Object key = entry.getKey();
				Object value = entry.getValue();
				if (dualArgs){
				    return (shouldInclude(key, value) && containsKey(key));
				} else {
				    return (shouldInclude(key) && containsKey(key));
				}
			}
			return false;
		}

		public boolean remove(Object o) {
			return false;
		}

		public int size(){
			// scan
			int c = 0;
			for (Iterator it = iterator(); it.hasNext(); it.next()){
				c++;
			}
			return c;
		}

		public void clear() {
			FilterMap.this.clear();
		}
	}
}
