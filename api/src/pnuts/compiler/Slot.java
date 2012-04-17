/*
 * @(#)Slot.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

class Slot {

	Object key;
	Object value;
	Slot chain;

	Slot(){
	}

	Slot(Object key, Object value){
		this.key = key;
		this.value = value;
	}
	
	public void set(Object value){
		this.value = value;
	}

	public Object get(){
		return value;
	}
}
