/*
 * @(#)isEmpty.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import pnuts.lang.Package;
import java.lang.reflect.Array;
import java.util.*;

public class isEmpty extends PnutsFunction {

	public isEmpty(){
		super("isEmpty");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
		}
		Object arg0 = args[0];
		if (arg0 instanceof Collection){
			return ((Collection)arg0).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		} else if (arg0 instanceof Map){
			return ((Map)arg0).isEmpty() ? Boolean.TRUE : Boolean.FALSE;
		} else if (arg0 instanceof Object[]){
			return ((Object[])arg0).length == 0 ? Boolean.TRUE : Boolean.FALSE;
		} else if (Runtime.isArray(arg0)){
			return Array.getLength(arg0) == 0 ? Boolean.TRUE : Boolean.FALSE;
		} else if (arg0 instanceof Package){
			return ((Package)arg0).size() == 0 ? Boolean.TRUE : Boolean.FALSE;
		} else {
			throw new IllegalArgumentException(String.valueOf(arg0));
		}
	}

	public String toString(){
		return "function isEmpty(collection)";
	}
}
