/*
 * RefMap.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import java.util.*;
import java.lang.ref.*;

public class RefMap extends AbstractMap implements Map {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final Object NULL_KEY = new Object();
    private static final String CACHE_CLEANER_NAME = "Cache Cleaner";
    private static final ReferenceQueue queue = new ReferenceQueue();

    private Entry[] table;
    private int size;
    private int threshold;
    private final float loadFactor;
    private volatile int modCount;

    public RefMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0){
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
	}
        if (initialCapacity > MAXIMUM_CAPACITY){
            initialCapacity = MAXIMUM_CAPACITY;
	}
        if (loadFactor <= 0 || Float.isNaN(loadFactor)){
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
	}
        int capacity = 1;
        while (capacity < initialCapacity){
            capacity <<= 1;
	}
        table = new Entry[capacity];
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
    }

    public RefMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public RefMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }

    public RefMap(Map m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1, 16),
             DEFAULT_LOAD_FACTOR);
        putAll(m);
    }

    private static Object maskNull(Object key) {
        return (key == null ? NULL_KEY : key);
    }

    private static Object unmaskNull(Object key) {
        return (key == NULL_KEY ? null : key);
    }

    static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    static int indexFor(int h, int length) {
        return h & (length-1);
    }

    private synchronized void expungeStaleEntry(Entry e) {
            int h = e.hash;
            int i = indexFor(h, table.length);
            Entry prev = table[i];
            Entry p = prev;
            while (p != null) {
                Entry next = p.next;
                if (p == e) {
                    if (prev == e){
                        table[i] = next;
                    } else {
                        prev.next = next;
		    }
                    e.next = null;
                    e.value = null;
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
    }

    public int size() {
        if (size == 0){
            return 0;
	}
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Object get(Object key) {
        Object k = maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = this.table;
        int index = indexFor(h, tab.length);
        Entry e = tab[index];
        while (e != null) {
            if (e.hash == h && eq(k, e.get())){
                return e.value;
	    }
            e = e.next;
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    Entry getEntry(Object key) {
        Object k = maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = this.table;
        int index = indexFor(h, tab.length);
        Entry e = tab[index];
        while (e != null && !(e.hash == h && eq(k, e.get()))){
            e = e.next;
	}
        return e;
    }

    public synchronized Object put(Object key, Object value) {
        Object k = maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = this.table;
        int i = indexFor(h, tab.length);

        for (Entry e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && eq(k, e.get())) {
                Object oldValue = e.value;
                if (value != oldValue){
                    e.value = value;
		}
                return oldValue;
            }
        }

        modCount++;
	Entry e = tab[i];
        tab[i] = new Entry(k, value, queue, h, e, this);
        if (++size >= threshold){
            resize(tab.length * 2);
	}
        return null;
    }

    synchronized void resize(int newCapacity) {
        Entry[] oldTable = this.table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry[] newTable = new Entry[newCapacity];
        transfer(oldTable, newTable);
        table = newTable;

        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        } else {
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    private void transfer(Entry[] src, Entry[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry e = src[j];
            src[j] = null;
            while (e != null) {
                Entry next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    public void putAll(Map m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0){
            return;
	}
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY){
                targetCapacity = MAXIMUM_CAPACITY;
	    }
            int newCapacity = table.length;
            while (newCapacity < targetCapacity){
                newCapacity <<= 1;
	    }
            if (newCapacity > table.length){
                resize(newCapacity);
	    }
        }

        for (Iterator it = m.entrySet().iterator(); it.hasNext();){
	    Map.Entry e = (Map.Entry)it.next();
            put(e.getKey(), e.getValue());
	}
    }

    public Object remove(Object key) {
        Object k = maskNull(key);
        int h = hash(k.hashCode());
        Entry[] tab = this.table;
        int i = indexFor(h, tab.length);
        Entry prev = tab[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;
            if (h == e.hash && eq(k, e.get())) {
                modCount++;
                size--;
                if (prev == e){
                    tab[i] = next;
		} else {
                    prev.next = next;
		}
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    Entry removeMapping(Object o) {
        if (!(o instanceof Map.Entry)){
            return null;
	}
        Entry[] tab = this.table;
        Map.Entry entry = (Map.Entry)o;
        Object k = maskNull(entry.getKey());
        int h = hash(k.hashCode());
        int i = indexFor(h, tab.length);
        Entry prev = tab[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e){
                    tab[i] = next;
                } else {
                    prev.next = next;
		}
                return e;
            }
            prev = e;
            e = next;
        }
        return null;
    }

    public void clear() {
        modCount++;
        Entry[] tab = table;
        for (int i = 0; i < tab.length; ++i){
            tab[i] = null;
	}
        size = 0;
    }

    public boolean containsValue(Object value) {
	if (value == null){
            return containsNullValue();
	}
	Entry[] tab = this.table;
        for (int i = tab.length ; i-- > 0 ;){
            for (Entry e = tab[i] ; e != null ; e = e.next){
                if (value.equals(e.value)){
                    return true;
		}
	    }
	}
	return false;
    }

    private boolean containsNullValue() {
	Entry[] tab = this.table;
        for (int i = tab.length ; i-- > 0 ;){
            for (Entry e = tab[i] ; e != null ; e = e.next){
                if (e.value==null){
                    return true;
		}
	    }
	}
	return false;
    }

    private static class Entry extends WeakReference implements Map.Entry {
        private Object value;
        private final int hash;
        private Entry next;
	private RefMap container;

        Entry(Object key, Object value, ReferenceQueue queue, int hash, Entry next, RefMap container) {
            super(key, queue);
            this.value = value;
            this.hash  = hash;
            this.next  = next;
	    this.container = container;
        }

        public Object getKey() {
            return RefMap.unmaskNull(get());
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object newValue) {
	    Object oldValue = value;
            value = newValue;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)){
                return false;
	    }
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))){
                    return true;
		}
            }
            return false;
        }

        public int hashCode() {
            Object k = getKey();
            Object v = getValue();
            return  ((k==null ? 0 : k.hashCode()) ^
                     (v==null ? 0 : v.hashCode()));
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    public Set keySet() {
	throw new UnsupportedOperationException();
    }

    public Collection values() {
	throw new UnsupportedOperationException();
    }

    public Set entrySet() {
	throw new UnsupportedOperationException();
    }

    static int hash(int h) {
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    public static void startCleanerThread(){
	Thread cleanerThread = new Thread(CACHE_CLEANER_NAME){
		public void run(){
		    try {
			while (true){
			    Entry entry = (Entry)queue.remove();
			    entry.container.expungeStaleEntry(entry);
			}
		    } catch (InterruptedException e){
			e.printStackTrace();
		    }
		}
	    };
	cleanerThread.setDaemon(true);
	cleanerThread.start();
    }
    static {
	startCleanerThread();
    }
}
