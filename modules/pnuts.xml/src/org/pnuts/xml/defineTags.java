/*
 * @(#)defineTags.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import pnuts.lang.*;
import pnuts.lang.Package;

public class defineTags extends PnutsFunction {
	final static PnutsFunction elementFunc = new element();

	public defineTags(){
		super("defineTags");
	}

	public boolean defined(int args){
		return true;
	}

	protected Object exec(Object[] args, Context context){
		Package pkg = context.getCurrentPackage();
		for (int i = 0; i < args.length; i++){
			String name = ((String)args[i]).intern();
			pkg.set(name, elementFunc.call(new String[]{name}, context), context);
		}
		return null;
	}

	public String toString(){
		return "function defineTags(tag1, tag2, ...)";
	}
}
