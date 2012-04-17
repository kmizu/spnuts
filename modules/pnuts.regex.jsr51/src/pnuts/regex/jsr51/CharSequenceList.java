/*
 * @(#)CharSequenceList.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.regex.jsr51;

import java.util.*;

class CharSequenceList extends AbstractList {
	private CharSequence text;
	private int[] start;
	private int[] end;

	protected CharSequenceList(){
	}

	public CharSequenceList(CharSequence text, int[] start, int[] end){
		this.text = text;
		this.start = start;
		this.end = end;
	}

	public Object get(int index){
		return text.subSequence(start[index], end[index]);
	}

	public Object set(int index, Object value){
		throw new UnsupportedOperationException();
	}

	public int size(){
		return start.length;
	}
}
