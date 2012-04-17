/*
 * ProjectionList.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;
import org.pnuts.util.ComparableArrayList;

class ProjectionList extends ComparableArrayList {

    private List x;

    protected ProjectionList(List x){
	this.x = x;
    }

    protected Object project(Object obj){
	return obj;
    }

    public int size(){
	return x.size();
    }

    public Object get(int idx){
	return project(x.get(idx));
    }

    public Object set(int index, Object element) {
	throw new UnsupportedOperationException();
    }

    public int indexOf(Object o) {
	int size = size();
	if (o == null) {
	    for (int i = 0; i < size; i++){
		if (get(i)==null){
		    return i;
		}
	    }
	} else {
	    for (int i = 0; i < size; i++){
		if (o.equals(get(i))){
		    return i;
		}
	    }
	}
	return -1;
    }
    public int lastIndexOf(Object o) {
	int size = size();
	if (o == null) {
	    for (int i = size-1; i >= 0; i--){
		if (get(i)==null){
		    return i;
		}
	    }
	} else {
	    for (int i = size-1; i >= 0; i--){
		if (o.equals(get(i))){
		    return i;
		}
	    }
	}
	return -1;
    }

    public Iterator iterator(){
	return new ProjectionIterator(x.iterator()){
		public Object project(Object obj){
		    return ProjectionList.this.project(obj);
		}
	    };
    }

    public Object clone(){
	List s = new ArrayList();
	for (Iterator it = iterator(); it.hasNext();){
	    s.add(it.next());
	}
	return s;
    }
}
