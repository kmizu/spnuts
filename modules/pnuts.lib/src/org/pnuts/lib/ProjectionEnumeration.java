/*
 * @(#)ProjectionEnumeration.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;

class ProjectionEnumeration implements Iterator {
	private Enumeration en;
	private Object next;
	private boolean end;
	private boolean needToFindNext = true;

	protected ProjectionEnumeration(){
	}

	public ProjectionEnumeration(Enumeration en){
		this.en = en;
	}

	protected Object project(Object obj){
		return obj;
	}

	protected boolean findNext(){
		if (en.hasMoreElements()){
			next = project(en.nextElement());
			return true;
		}
		this.end = true;
		return false;
	}

	public boolean hasNext(){
		if (needToFindNext){
			findNext();
			this.needToFindNext = false;
		}
		return !end;
	}

	public Object next(){
		if (!hasNext()){
			throw new NoSuchElementException();
		}
		this.needToFindNext = true;
		return next;
	}

	public void remove(){
		throw new UnsupportedOperationException();
	}
}
