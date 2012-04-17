/*
 * @(#)pop.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;

import java.util.*;

/*
 * function pop(linkedListOrStack)
 */
public class pop extends PnutsFunction {

	public pop(){
		super("pop");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		Object arg = args[0];
		if (arg instanceof LinkedList){
			return ((LinkedList)args[0]).removeLast();
		} else if (arg instanceof Stack){
			return ((Stack)args[0]).pop();
		} else {
			throw new IllegalArgumentException(String.valueOf(arg));
		}
	}

	public String toString(){
		return "function pop(linkedListOrStack)";
	}
}
