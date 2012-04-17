/*
 * @(#)push.java 1.2 04/12/06
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
 * function push(list, elem)
 */
public class push extends PnutsFunction {

	public push(){
		super("push");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		((Collection)args[0]).add(args[1]);
		return null;
	}

	public String toString(){
		return "function push(collection, elem)";
	}
}
