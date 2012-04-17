/*
 * LRUCache.java
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import java.util.*;

/**
 * An implementation of LRU cache
 */
public class LRUCache implements Cache {

	private Cell[] cells;

	private int size;

	private Cell head;

	private Cell tail;

	private int count = 0;

	static class Cell implements Map.Entry {
		Cell next;
		Cell prev;
		Cell chain;
		Object key;
		Object value;
		int index;

		Cell(int index, Cell prev, Cell chain, Object key, Object value) {
			this.index = index;
			this.prev = prev;
			this.chain = chain;
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
			Object old = this.value;
			this.value = value;
			return old;
		}

		public int hashCode(){
			return key.hashCode() + value.hashCode();
		}

		public boolean equals(Object obj){
			if (obj instanceof Cell){
				Cell c = (Cell)obj;
				return (key == null && c.key == null || key.equals(c.key)) &&
					(value == null && c.value == null || value.equals(c.value));
			} else {
				return false;
			}
		}

		public String toString(){
			return key + "=" + value;
		}
	}

	protected LRUCache() {
	}

	/**
	 * @param size
	 *            cache size
	 */
	public LRUCache(int size) {
		cells = new Cell[size];
		this.size = size;
	}

	void update(Cell b) {
		if (b != tail) {
			if (b.prev != null) {
				b.prev.next = b.next;
			}
			if (b.next != null) {
				b.next.prev = b.prev;
			}
			tail.next = b;
			b.prev = tail;
			tail = b;
		}
		if (b == head) {
			head = b.next;
		}
		b.next = null;
	}

	private Cell findCell(Object key) {
		int index = (key.hashCode() & 0x7FFFFFFF) % size;
		Cell b = cells[index];
		while (b != null) {
			if (b.key.equals(key)) {
				update(b);
				return b;
			}
			b = b.chain;
		}
		return null;
	}

	/**
	 * If key is in the cache it returns value, otherwise null.
	 */
	public Object get(Object key) {
		Cell cell = findCell(key);
		if (cell != null) {
			return cell.value;
		} else {
			return null;
		}
	}

	/**
	 * Register key and its value into the cache.
	 */
	public Object put(Object key, Object value) {
		int index = (key.hashCode() & 0x7FFFFFFF) % size;

		Cell b = cells[index];
		Cell n = null;
		Object old = null;
		while (b != null) {
			if (b.key.equals(key)) {
				old = b.value;
				b.value = value;
				update(b);
				return old;
			}
			b = b.chain;
		}
		if (head != null) {
			if (count >= size) {
				Cell second = head.next;
				n = head;
				expired(n.value);
				n.prev = tail;
				tail.next = n;
				n.next = null;
				n.key = key;
				n.value = value;
				if (n.index == index) {
					if (n != cells[index]) {
						for (Cell x = cells[index]; ; x = x.chain) {
							if (x.chain == null) {
								throw new RuntimeException("[BUG] Reused cell must be found in the chain of index=" + index);
							} else if (x.chain == n) {
								x.chain = n.chain;
								break;
							}
						}
						n.chain = cells[index];
						cells[index] = n;
					}
				} else {
					Cell t = cells[n.index];
					if (t == n) {
						cells[n.index] = n.chain;
					} else {
						while (t != null && t.chain != null) {
							if (t.chain == n) {
								t.chain = n.chain;
								break;
							}
							t = t.chain;
						}
					}
					n.chain = cells[index];
					cells[index] = n;
				}
				n.index = index;
				head = second;
				if (head == null){
					head = n;
				}
				tail = n;
			} else {
				count++;
				n = new Cell(index, tail, cells[index], key, value);
				cells[index] = n;
				tail.next = n;
				tail = n;
			}
		} else {
			count++;
			n = new Cell(index, null, cells[index], key, value);
			n.prev = n;
			n.next = n;
			cells[index] = n;
			head = n;
			tail = n;
		}
		return old;
	}

	/**
	 * Called when an object is expired from the cache
	 *
	 * @param old an expired object
	 */
	public void expired(Object old){
		// skip
	}

	/**
	 * Initializes the cache
	 */
	public void reset() {
		head = tail = null;
		cells = new Cell[size];
		count = 0;
	}

	/**
	 * Returns the number of items in the cache
	 */
	public int size(){
		return count;
	}

	public Set keySet(){
		Set keys = new HashSet();
		for (Iterator it = new Itr(0); it.hasNext();){
			keys.add(it.next());
		}
		return keys;
	}

	public Set entrySet(){
		Set entries = new HashSet();
		for (Iterator it = new Itr(2); it.hasNext();){
			entries.add(it.next());
		}
		return entries;
	}

	public Collection values(){
		ArrayList v = new ArrayList();
		for (Iterator it = new Itr(1); it.hasNext();){
			v.add(it.next());
		}
		return v;
	}

	class Itr implements Iterator {
		Cell ref = head;
		int kind;
		
		Itr(int kind){
			this.kind = kind;
		}

		public boolean hasNext(){
			return ref != null;
		}

		public Object next(){
			switch (kind){
			case 0:
				Object key = ref.key;
				ref = ref.next;
				return key;
			case 1:
				Object value = ref.value;
				ref = ref.next;
				return value;
			case 2:
				Object entry = ref;
				ref = ref.next;
				return entry;
			default:
				throw new RuntimeException();
			}
		}

		public void remove(){
			throw new UnsupportedOperationException();
		}
	}
}
