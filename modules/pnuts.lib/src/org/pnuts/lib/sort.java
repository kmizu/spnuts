/*
 * @(#)sort.java 1.3 05/01/14
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.util.*;

public class sort extends PnutsFunction {

	public sort(){
		super("sort");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		Comparator comp;
		Object obj;
		int nargs = args.length;
		if (nargs == 1){
			obj = args[0];
			comp = new PnutsComparator(context);
		} else if (nargs == 2){
			obj = args[0];
			Object c = args[1];
			if (c instanceof PnutsFunction){
				comp = new FunctionComparator((PnutsFunction)c, context);
			} else if (c instanceof Comparator){
				comp = (Comparator)c;
			} else {
				throw new IllegalArgumentException(String.valueOf(c));
			}
		} else {
			undefined(args, context);
			return null;
		}
		if (obj instanceof List){
			Collections.sort((List)obj, comp);
		} else if (obj instanceof Object[]){
			Arrays.sort((Object[])obj, comp);
		} else if (obj instanceof int[]){
			Arrays.sort((int[])obj);
		} else if (obj instanceof char[]){
			Arrays.sort((char[])obj);
		} else if (obj instanceof long[]){
			Arrays.sort((long[])obj);
		} else if (obj instanceof float[]){
			Arrays.sort((float[])obj);
		} else if (obj instanceof double[]){
			Arrays.sort((double[])obj);
		} else if (obj instanceof byte[]){
			Arrays.sort((byte[])obj);
		} else if (obj instanceof TreeSet){
			return obj;
		} else if (obj instanceof Set){
			TreeSet ts = new TreeSet(comp);
			ts.addAll((Set)obj);
			return ts;
		} else {
			throw new IllegalArgumentException(String.valueOf(obj));
		}
		return obj;
	}

	public String toString(){
		return "function sort(elements, {func(arg) | Comparator})";
	}
}
