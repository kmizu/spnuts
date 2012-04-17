/*
 * @(#)FilterIterator.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;

class FilterIterator implements Iterator {
	private Iterator it;
	private Object next;
	private boolean end;
	private boolean needToFindNext = true;

	public FilterIterator(){
	}

	public FilterIterator(Iterator it){
		this.it = it;
	}

	protected boolean shouldInclude(Object obj){
		return true;
	}

	protected boolean findNext(){
		while (it.hasNext()){
			Object n = it.next();
			if (shouldInclude(n)){
				next = n;
				return true;
			}
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
