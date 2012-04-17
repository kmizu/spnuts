/*
 * @(#)LocalNameMap.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;
import org.xml.sax.Attributes;

/*
 * A read-only Map object that consists of {localName->value} mappings of a Attributes object.
 * entrySet()/keySet()/valueSet() conserve the order of the attributes.
 *
 * Since Attributes object is mutable, an instance of this class is also mutable. That is,
 * the state of an AttributeMap object is changing during XML parsing.
 */
class LocalNameMap extends SAXAttributeMap {
	HashMap map;
	ArrayList entries;

	void setAttributes(Attributes attributes){
		super.setAttributes(attributes);
		this.map = null;
		this.entries = null;
	}

	public int size(){
		if (map == null){
			return attributes.getLength();
		} else {
			return map.size();
		}
	}

	public Object get(Object key){
		if (map == null){
			prepareLocalNameMap();
		}
		return map.get(key);
	}

	private void prepareLocalNameMap(){
		entries = new ArrayList();
		map = new HashMap();
		for (int i = 0; i < attributes.getLength(); i++){
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			entries.add(new Entry(name, value));
			map.put(name, value);
		}
	}
	
	public Set keySet(){
		if (entries == null){
			prepareLocalNameMap();
		}
		return new KeySet();
	}

	public Collection values(){
		if (entries == null){
			prepareLocalNameMap();
		}
		return new ValueSet();
	}

	public Set entrySet(){
		if (entries == null){
			prepareLocalNameMap();
		}
		return new EntrySet();
	}


	static class Entry implements Map.Entry {
		Object key;
		Object value;
	
		Entry(Object key, Object value){
			this.key = key;
			this.value = value;
		}
	
		public Object getKey(){
			return key;
		}

		public Object getValue(){
			return value;
		}

		public Object setValue(Object value){
			throw new UnsupportedOperationException();
		}
	}

	class EntrySet extends AbstractCollection implements Set {
		public int size(){
			return entries.size();
		}
	
		public boolean isEmpty(){
			return entries.isEmpty();
		}

		public Iterator iterator(){
			return new EntrySetIterator();
		}
	}

	class ValueSet extends EntrySet {
		public Iterator iterator(){
			return new ValueSetIterator();
		}
	}

	class KeySet extends EntrySet {
		public Iterator iterator(){
			return new KeySetIterator();
		}
	}

	class EntrySetIterator implements Iterator {
		private int max;
		private int pos;

		EntrySetIterator(){
			this.max = entries.size();
			this.pos = 0;
		}
	
		public boolean hasNext(){
			return pos <= max - 1;
		}

		protected Object get(int idx){
			return entries.get(idx);
		}
	
		public Object next(){
			if (pos > max - 1){
				throw new NoSuchElementException();
			}
			return get(pos++);
		}

		public void remove(){
			throw new UnsupportedOperationException();
		}
	}

	class ValueSetIterator extends EntrySetIterator {
		protected Object get(int idx){
			return ((Map.Entry)entries.get(idx)).getValue();
		}
	}

	class KeySetIterator extends EntrySetIterator {
		protected Object get(int idx){
			return ((Map.Entry)entries.get(idx)).getKey();
		}
	}
}
