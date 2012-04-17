/*
 * FunctionComparator.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import java.util.Comparator;

/**
 * Comparator that compares the results of function calls.
 */
public class FunctionComparator implements Comparator {
	private PnutsFunction func;
	private Context context;
	private int equalValue;

	/**
	 * Constructor
	 *
	 * @param func the results of the function are compared
	 * @param context the context in which the function is called
	 */
	public FunctionComparator(PnutsFunction func, Context context){
	    this(func, context, 0);
	}

	/**
	 * Constructor
	 *
	 * @param func the results of the function are compared
	 * @param context the context in which the function is called
	 * @param equalValue compare() returns this when two objects are equal; the default is 0
	 */
	public FunctionComparator(PnutsFunction func, Context context, int equalValue){
		this.func = func;
		this.context = context;
		this.equalValue = equalValue;
	}

	public int compare(Object obj1, Object obj2){
		Object o1 = func.call(new Object[]{obj1}, context);
		Object o2 = func.call(new Object[]{obj2}, context);
		if (equalValue == 0){
			return Runtime.compareTo(o1, o2);
		} else {
			int result = Runtime.compareTo(o1, o2);
			if (result == 0) {
				if (obj1.equals(obj2)){
					return 0;
				} else {
					return equalValue;
				}
			} else {
				return result;
		    	}
		}
	}
}
