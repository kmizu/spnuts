/*
 * SAXAttributeMap.java
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;
import org.xml.sax.Attributes;

/*
 * A read-only Map object that consists of {qName->value} mappings of a Attributes object.
 * entrySet()/keySet()/valueSet() conserve the order of the attributes.
 *
 * Since Attributes object is mutable, an instance of this class is also mutable. That is,
 * the state of an AttributeMap object is changing during XML parsing.
 */
class SAXAttributeMap implements Map {
	protected Attributes attributes;

	void setAttributes(Attributes attributes){
		this.attributes = attributes;
	}

	Attributes getAttributes(){
		return attributes;
	}

	public int size(){
		return attributes.getLength();
	}

	public boolean isEmpty(){
		return size() > 0;
	}

	public boolean containsKey(Object key){
		return get(key) != null;
	}

	public boolean containsValue(Object value){
		return values().contains(value);
	}

	public Object get(Object key){
		return attributes.getValue((String)key);
	}

	public Set keySet(){
		return new KeySet();
	}

	public Collection values(){
		return new ValueSet();
	}

	public Set entrySet(){
		return new EntrySet();
	}

	/* Unsupported Operations ...*/

	public Object put(Object key, Object value){
		throw new UnsupportedOperationException();
	}

	public Object remove(Object key){
		throw new UnsupportedOperationException();
	}

	public void putAll(Map t){
		throw new UnsupportedOperationException();
	}

	public void clear(){
		throw new UnsupportedOperationException();
	}


	class EntrySet extends AbstractCollection implements Set {
		public int size(){
			return attributes.getLength();
		}

		public boolean isEmpty(){
			return attributes.getLength() > 0;
		}

		public Iterator iterator(){
			return new EntrySetIterator();
		}
	}

	class KeySet extends EntrySet {
		public Iterator iterator(){
			return new KeySetIterator();
		}
	}

	class ValueSet extends EntrySet {
		public Iterator iterator(){
			return new ValuesIterator();
		}
	}

	class KeySetIterator extends EntrySetIterator {
		protected Object get(int idx){
			return attributes.getQName(idx);
		}
	}

	class ValuesIterator extends EntrySetIterator {
		protected Object get(int idx){
			return attributes.getValue(idx);
		}
	}

	class EntrySetIterator implements Iterator {
		private int pos = 0;
		private int max;

		EntrySetIterator(){
			this.max = attributes.getLength();
		}

		public boolean hasNext(){
			return pos < max;
		}

		protected Object get(int idx){
			return new Entry(idx);
		}

		public Object next(){
			int idx = pos++;
			if (pos > max){
				throw new NoSuchElementException();
			}
			return get(idx);
		}

		public void remove(){
			throw new UnsupportedOperationException();
		}
	}

	class Entry implements Map.Entry {
		private int idx;

		Entry(int idx){
			this.idx = idx;
		}

		public Object getKey(){
			return attributes.getQName(idx);
		}

		public Object getValue(){
			return attributes.getValue(idx);
		}

		public Object setValue(Object value){
			throw new UnsupportedOperationException();
		}
	}
}
