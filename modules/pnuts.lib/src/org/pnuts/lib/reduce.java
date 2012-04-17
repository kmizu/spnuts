/*
 * @(#)reduce.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.util.*;
import java.lang.reflect.Array;

public class reduce extends PnutsFunction {

	public reduce(){
		super("reduce");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	protected Object exec(Object[] args, final Context context){
		int nargs = args.length;
		Object arg0;
		Object target;
		Object initial;
		if (nargs == 3){
			arg0 = args[0];
			target = args[1];
			initial = args[2];
		} else if (nargs == 2){
			arg0 = args[0];
			target = args[1];
			initial = null;
		} else {
			undefined(args, context);
			return null;
		}

		if (!(arg0 instanceof PnutsFunction)){
			throw new IllegalArgumentException(String.valueOf(arg0));
		}
		final PnutsFunction func = (PnutsFunction)arg0;

		Object[] fargs = new Object[2];
		if (target instanceof Iterator){
			Iterator it = (Iterator)target;
			if (nargs == 3){
				fargs[0] = initial;
			} else {
				if (it.hasNext()){
					fargs[0] = it.next();
				}
			}
			while (it.hasNext()){
				fargs[1] = it.next();
				fargs[0] = func.call(fargs, context);
			}
			return fargs[0];
		} else if (target instanceof Enumeration){
			Enumeration en = (Enumeration)target;
			if (nargs == 3){
				fargs[0] = initial;
			} else {
				if (en.hasMoreElements()){
					fargs[0] = en.nextElement();
				}
			}
			while (en.hasMoreElements()){
				fargs[1] = en.nextElement();
				fargs[0] = func.call(fargs, context);
			}
			return fargs[0];
		} else if (target instanceof Collection){
			Iterator it = ((Collection)target).iterator();
			if (nargs == 3){
				fargs[0] = initial;
			} else {
				if (it.hasNext()){
					fargs[0] = it.next();
				}
			}
			while (it.hasNext()){
				fargs[1] = it.next();
				fargs[0] = func.call(fargs, context);
			}
			return fargs[0];
		} else if (target instanceof Object[]){
			Object[] array = (Object[])target;
			int i = 0;
			if (nargs == 3){
				fargs[0] = initial;
			} else {
				if (array.length > 1){
					fargs[0] = array[i++];
				}
			}
			for (; i < array.length; i++){
				fargs[1] = array[i];
				fargs[0] = func.call(fargs, context);
			}
			return fargs[0];
		} else if (target instanceof Generator){
			final Generator g = (Generator)target;
			final Object[] fa = fargs;
			if (nargs == 2){
				class F extends PnutsFunction {
					boolean first = true;
					protected Object exec(Object[] a, Context ctx){
						if (first){
							fa[0] = a[0];
							first = false;
						} else {
							fa[1] = a[0];
							fa[0] = func.call(fa, context);
						}
						return null;
					}
				}
				g.apply(new F(), context);
			} else {
				fa[0] = initial;
				class F extends PnutsFunction {
					protected Object exec(Object[] a, Context ctx){
						fa[1] = a[0];
						fa[0] = func.call(fa, context);
						return null;
					}
				}
				g.apply(new F(), context);
			}
			return fa[0];
		} else if (Runtime.isArray(target)){
			ArrayList lst = new ArrayList();
			int len = Runtime.getArrayLength(target);
			int i = 0;
			if (nargs == 3){
				fargs[0] = initial;
			} else {
				if (len > 1){
					fargs[0] = Array.get(target, i++);
				}
			}
			for (; i < len; i++){
				fargs[1] = Array.get(target, i);
				fargs[0] = func.call(fargs, context);
			}
			return fargs[0];
		} else {
			throw new IllegalArgumentException();
		}
	}

	public String toString(){
		return "function reduce( func(a, b), elements { , intialValue} )";
	}
}
