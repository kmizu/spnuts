/*
 * SimpleArrayList.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;
import java.util.*;

class SimpleArrayList extends AbstractList
    implements RandomAccess, java.io.Serializable
{
    private final Object[] a;

    SimpleArrayList(Object[] array) {
	if (array==null)
	    throw new NullPointerException();
	a = array;
    }

    public int size() {
	return a.length;
    }
    
    public Object[] toArray() {
	return (Object[])a.clone();
    }

    public Object[] toArray(Object[] a) {
	int size = size();
	if (a.length < size){
	    for (int i = 0; i < a.length; i++){
		a[i] = this.a[i];
	    }
	    return a;
	}
	System.arraycopy(this.a, 0, a, 0, size);
	if (a.length > size){
	    a[size] = null;
	}
	return a;
    }

    public Object get(int index) {
	return a[index];
    }

    public Object set(int index, Object element) {
	Object oldValue = a[index];
	a[index] = element;
	return oldValue;
    }

    public int indexOf(Object o) {
	if (o==null) {
	    for (int i=0; i<a.length; i++)
		if (a[i]==null)
		    return i;
	} else {
	    for (int i=0; i<a.length; i++)
		if (o.equals(a[i]))
		    return i;
	}
	return -1;
    }

    public boolean contains(Object o) {
	return indexOf(o) != -1;
    }
}
