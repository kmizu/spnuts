/*
 * @(#)binarySearch.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

public class binarySearch extends PnutsFunction {

	public binarySearch(){
		super("binarySearch");
	}

	public boolean defined(int narg){
		return narg == 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		Object elements = args[0];
		Object key = args[1];
		if (elements instanceof List){
			return new Integer(Collections.binarySearch((List)elements,
														key,
														new PnutsComparator(context)));
		} else if (elements instanceof Object[]){
			return new Integer(Arrays.binarySearch((Object[])elements,
												   key,
												   new PnutsComparator(context)));
		} else if (elements instanceof int[]){
			return new Integer(Arrays.binarySearch((int[])elements,
												   ((Number)key).intValue()));
		} else if (elements instanceof byte[]){
			return new Integer(Arrays.binarySearch((byte[])elements,
												   ((Number)key).byteValue()));
		} else if (elements instanceof char[]){
			return new Integer(Arrays.binarySearch((char[])elements,
												   ((Character)key).charValue()));
		} else if (elements instanceof short[]){
			return new Integer(Arrays.binarySearch((short[])elements,
												   ((Number)key).shortValue()));
		} else if (elements instanceof long[]){
			return new Integer(Arrays.binarySearch((long[])elements,
												   ((Number)key).longValue()));
		} else if (elements instanceof float[]){
			return new Integer(Arrays.binarySearch((float[])elements,
												   ((Number)key).floatValue()));
		} else if (elements instanceof double[]){
			return new Integer(Arrays.binarySearch((double[])elements,
												   ((Number)key).doubleValue()));
		} else {
			throw new IllegalArgumentException(String.valueOf(elements));
		}
	}

	public String toString(){
		return "binarySearch(elements, key)";
	}
}
