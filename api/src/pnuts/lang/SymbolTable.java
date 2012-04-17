/*
 * SymbolTable.java
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

class SymbolTable implements Cloneable, Serializable {

	private static final int INITIAL_CAPACITY = 8;
	private static final float LOAD_FACTOR = 0.75f;

	transient Binding[] table;
	int count;
	SymbolTable parent;
	private int threshold;

	public SymbolTable() {
		this.threshold = (int) (INITIAL_CAPACITY * LOAD_FACTOR);
		this.table = new Binding[INITIAL_CAPACITY];
	}

	public SymbolTable(SymbolTable parent) {
		this();
		this.parent = parent;
	}

	/**
	 * Gets the value of a variable.
	 * 
	 * @param interned
	 *            the name of the variable, which must be intern'd
	 * @return the value
	 */
	public synchronized Object get(String interned) {
		int hash = interned.hashCode() & 0x7fffffff;
		int i = hash & (table.length - 1);
		Binding b = table[i];
		while (true) {
			if (b == null) {
				return b;
			}
			if (interned == b.name) {
				return b.value;
			}
			b = b.chain;
		}
	}

	synchronized Binding lookup0(String interned) {
		int hash = interned.hashCode() & 0x7fffffff;
		int i = hash & (table.length - 1);
		Binding b = table[i];
		while (b != null && interned != b.name) {
			b = b.chain;
		}
		return b;
	}

	/**
	 * Looks for a name-value binding in the symbol table chain.
	 * 
	 * @param interned
	 *            the name of the variable, which must be intern'd
	 * @return a NamedValue
	 */
	public synchronized NamedValue lookup(String interned) {
		int hash = interned.hashCode() & 0x7fffffff;
		SymbolTable env = this;
		do {
			Binding[] env_tab = env.table;
			int i = hash & (env_tab.length - 1);
			Binding b = env_tab[i];
			while (b != null && interned != b.name) {
				b = b.chain;
			}
			if (b != null) {
				return b;
			}
			env = env.parent;
		} while (env != null);
		return null;
	}

	/**
	 * Defines a name-value binding in the symbol table.
	 * 
	 * @param interned  the name of the variable, which must be intern'd
	 * @param value the new value
	 * @exception IllegalStateException
	 *                thrown when the specified symbol has been defined as a
	 *                constant.
	 */
	public synchronized void set(String interned, Object value) {
		int hash = interned.hashCode() & 0x7fffffff;
		int i = hash & (table.length - 1);

		for (Binding b = table[i]; b != null; b = b.chain) {
			if (interned == b.name) {
				b.set(value);
				return;
			}
		}
		addBinding(hash, interned, value, i);
	}

	/**
	 * Defines a constant in the symbol table.
	 * 
	 * @param interned the name of the variable, which must be intern'd
	 * @param value the constant value
	 * @exception IllegalStateException
	 *                thrown when the specified symbol has been defined as a
	 *                constant
	 */
	public synchronized void setConstant(String interned, Object value) {
		int hash = interned.hashCode() & 0x7fffffff;
		int i = hash & (table.length - 1);

		for (Binding b = table[i]; b != null; b = b.chain) {
			if (interned == b.name) {
				if (b instanceof ImmutableBinding) {
					throw new IllegalStateException();
				}
				removeBinding(interned);
			}
		}
		addConstant(hash, interned, value, i);
	}

	synchronized void assign(String interned, Object value) {
		int hash = interned.hashCode() & 0x7fffffff;
		SymbolTable env = this;
		do {
			Binding[] env_tab = env.table;
			int i = hash & (env_tab.length - 1);
			Binding b = env_tab[i];
			while (b != null && interned != b.name) {
				b = b.chain;
			}
			if (b != null) {
				b.set(value);
				return;
			}
			env = env.parent;
		} while (env != null);

		addBinding(hash, interned, value, hash & (table.length - 1));
	}

	synchronized void addBinding(int hash, String interned, Object value, int index) {
		table[index] = new Binding(hash, interned, value, table[index]);
		if (count++ >= threshold) {
			ensureCapacity(table.length * 2);
		}
	}

	synchronized void addConstant(int hash, String interned, Object value, int index) {
		table[index] = new ImmutableBinding(hash, interned, value, table[index]);
		if (count++ >= threshold) {
			ensureCapacity(table.length * 2);
		}
	}

	void ensureCapacity(int newCapacity) {
		Binding[] oldTable = table;
		int oldCapacity = oldTable.length;
		if (oldCapacity == 1 << 30) {
			threshold = Integer.MAX_VALUE;
			return;
		}

		Binding[] newTable = new Binding[newCapacity];
		Binding[] src = table;
		for (int j = 0; j < src.length; j++) {
			Binding b = src[j];
			if (b != null) {
				src[j] = null;
				do {
					Binding next = b.chain;
					int i = b.hash & (newCapacity - 1);
					b.chain = newTable[i];
					newTable[i] = b;
					b = next;
				} while (b != null);
			}
		}
		table = newTable;
		threshold = (int) (newCapacity * LOAD_FACTOR);
	}

	synchronized Binding removeBinding(String interned) {
		int hash = interned.hashCode() & 0x7fffffff;
		int i = hash & (table.length - 1);

		Binding prev = table[i];
		Binding b = prev;

		while (b != null) {
			Binding next = b.chain;
			if (interned == b.name) {
				count--;
				if (prev == b) {
					table[i] = next;
				} else {
					prev.chain = next;
				}
				return b;
			}
			prev = b;
			b = next;
		}

		return b;
	}

	/**
	 * Deletes all name-value bindings.
	 */
	public synchronized void clear() {
		Binding tab[] = table;
		for (int i = 0; i < tab.length; i++) {
			tab[i] = null;
		}
		count = 0;
	}

	public int size() {
		return count;
	}

	/**
	 * Deep copy
	 */
	public Object clone() {
		try {
			SymbolTable copy = (SymbolTable) super.clone();
			Binding[] newTable = new Binding[table.length];
			for (int i = 0; i < table.length; i++) {
				Binding b = table[i];
				if (b != null) {
					newTable[i] = (Binding) b.clone();
				}
			}
			copy.table = newTable;
			if (parent != null) {
				copy.parent = (SymbolTable) parent.clone();
			}
			return copy;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	/**
	 * Returns an enumeration of the NamedValues in the symbol table.
	 * 
	 * @return an enumeration of the NamedValues
	 * @see pnuts.lang.NamedValue
	 */
	public Enumeration bindings() {
		return new Enumerator(0);
	}

	/**
	 * Returns an enumeration of the keys in the symbol table.
	 * 
	 * @return an enumeration of the keys
	 */
	public Enumeration keys() {
		return new Enumerator(1);
	}

	/**
	 * Returns an enumeration of the values in the symbol table.
	 * 
	 * @return an enumeration of the values
	 */
	public Enumeration values() {
		return new Enumerator(2);
	}

	private class Enumerator implements Enumeration {
		Binding bind = null;

		int index = table.length;

		int kind;

		Enumerator(int kind) { // 0==Binding, 1==key, 2==value
			this.kind = kind;
		}

		public boolean hasMoreElements() {
			while (bind == null && index > 0) {
				bind = table[--index];
			}
			return bind != null;
		}

		public Object nextElement() {
			while (bind == null && index > 0) {
				bind = table[--index];
			}
			if (bind != null) {
				Binding b = bind;
				bind = b.chain;
				if (kind == 0) {
					return b;
				} else if (kind == 1) {
					return b.name;
				} else {
					return b.value;
				}
			}
			throw new NoSuchElementException("SymbolTable Enumerator");
		}
	}

	static final long serialVersionUID = 61380568117862288L;

	private void writeObject(ObjectOutputStream s)
			throws IOException {
	    s.defaultWriteObject();
	    int count = 0;
	    for (int i = 0; i < table.length; i++){
		Binding b = table[i];
		while (b != null){
		    if (b.value instanceof Serializable){
			count++;
		    }
		    b = b.chain;
		}
	    }
	    s.writeInt(count);
	    for (int i = 0; i < table.length; i++){
		Binding b = table[i];
		while (b != null){
		    if (b.value instanceof Serializable){
			s.writeObject(b);
		    }
		    b = b.chain;
		}
	    }
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		int count = s.readInt();
		this.table = new Binding[INITIAL_CAPACITY];
		for (int i = 0; i < count; i++){
		    Binding b = (Binding)s.readObject();
		    set(b.name, b.value);
		}
	}
}
