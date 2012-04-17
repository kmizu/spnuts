/*
 * @(#)sync.java 1.2 04/12/06
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map;
import java.util.SortedMap;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;

public class sync extends PnutsFunction {

	public sync(){
		super("sync");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	static Object sync(Object arg){
		return sync(arg, arg);
	}

	static Object sync(Object arg, Object lock){
		if (arg instanceof PnutsFunction){
			PnutsFunction func = (PnutsFunction)arg;
			return new SynchronizedFunction(func, lock);
		} else if (arg instanceof Collection){
			if (arg instanceof List){
				return Collections.synchronizedList((List)arg);
			} else if (arg instanceof Set){
				if (arg instanceof SortedSet){
					return Collections.synchronizedSortedSet((SortedSet)arg);
				} else {
					return Collections.synchronizedSet((Set)arg);
				}
			} else {
				return Collections.synchronizedCollection((Collection)arg);
			}
		} else if (arg instanceof Map){
			if (arg instanceof SortedMap){
				return Collections.synchronizedSortedMap((SortedMap)arg);
			} else {
				return Collections.synchronizedMap((Map)arg);
			}
		} else {
			throw new IllegalArgumentException(String.valueOf(arg));
		}
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			return sync(args[0]);
		} else if (nargs == 2){
			return sync(args[0], args[1]);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function sync(func | collection | map {, lock } )";
	}
}
