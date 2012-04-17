/*
 * @(#)Symbol.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

class Symbol {
	int seq;
	String prefix;

	Symbol(){
		this.prefix = "_";
	}
	Symbol(String prefix){
		this.prefix = prefix;
	}
	String gen(){
		return prefix + seq++;
	}
}
