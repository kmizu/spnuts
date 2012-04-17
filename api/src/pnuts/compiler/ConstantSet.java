/*
 * @(#)ConstantSet.java 1.3 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

class ConstantSet {

	Slot[] table;
	int count = 0;
	static final float THRESHOLD = 0.7f;

	public ConstantSet() {
		this(256);
	}

	public ConstantSet(int initialCapacity){
		table = new Slot[initialCapacity];
	}

	public Slot getSlot(Object key){
		int hash = key.hashCode();
		int idx = (hash & 0x7FFFFFFF) % table.length;
		for (Slot s = table[idx]; s != null; s = s.chain){
			if (key.equals(s.key)){
				return s;
			}
		}
		if (count >= table.length * THRESHOLD){
			rehash(2 * table.length);
		}
		count++;
		Slot s = new Slot(key, null);
		s.chain = table[idx];
		table[idx] = s;

		return s;
	}

	void rehash(int new_capacity){
		Slot[] new_table = new Slot[new_capacity];
		for (int i = table.length;  --i >= 0;) {
			Slot prev = null;
			for (Slot cur = table[i];  cur != null; ){
				Slot next = cur.chain;
				cur.chain = prev;
				prev = cur;
				cur = next;
			}
			table[i] = prev;

			for (Slot cur = table[i]; cur != null; ){
				int hash = cur.key.hashCode();
				int new_index = (hash & 0x7FFFFFFF) % new_capacity;
				Slot next = cur.chain;
				cur.chain = new_table[new_index];
				new_table[new_index] = cur;
				cur = next;
			}
		}
		table = new_table;
	}
}
