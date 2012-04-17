/*
 * @(#)LocalInfo.java 1.3 05/04/22
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

class LocalInfo {
	String symbol;
	int map;
	int index;
	boolean initialized;
	Frame frame;

	LocalInfo(String symbol, int map, Frame frame) {
		this.symbol = symbol;
		this.map = map;
		this.index = -1;
		this.initialized = false;
		this.frame = frame;
	}
	
	LocalInfo(String symbol, int map, int index, boolean initialized) {
		this.symbol = symbol;
		this.map = map;
		this.index = index;
		this.initialized = initialized;
	}

	public String toString(){
		return getClass().getName() + "[" + symbol + "," + map + "," + index + "," + initialized + "]";
	}
}
