/*
 * @(#)join.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import pnuts.lang.Generator;
import java.util.List;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.lang.reflect.Array;

public class join extends PnutsFunction {

	public join(){
		super("join");
	}

	public boolean defined(int narg){
		return (narg == 2);
	}

	static String concat(final String delim, Object target, Context context){
		final StringBuffer sbuf = new StringBuffer();
		Object elem;
		if (target instanceof Object[]){
			Object[] array = (Object[])target;
			int len = array.length;
			for (int i = 0; i < len - 1; i++){
				elem = array[i];
				if (elem != null){
					sbuf.append(elem.toString());
				}
				sbuf.append(delim);
			} 
			if (len > 0){
				elem = array[len - 1];
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
		} else if (target instanceof List){
			List lst = (List)target;
			int len = lst.size();
			for (int i = 0; i < len - 1; i++){
				elem = lst.get(i);
				if (elem != null){
					sbuf.append(elem.toString());
				}
				sbuf.append(delim);
			} 
			if (len > 0){
				elem = lst.get(len - 1);
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
		} else if (Runtime.isArray(target)){
			int len = Array.getLength(target);
			for (int i = 0; i < len - 1; i++){
				elem = Array.get(target, i);
				if (elem != null){
					sbuf.append(elem.toString());
				}
				sbuf.append(delim);
			} 
			if (len > 0){
				elem = Array.get(target, len - 1);
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
		} else if (target instanceof Iterator){
			Iterator it = (Iterator)target;
			if (it.hasNext()){
				elem = it.next();
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
			while (it.hasNext()){
				sbuf.append(delim);
				elem = it.next();
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
		} else if (target instanceof Enumeration){
			Enumeration en = (Enumeration)target;
			if (en.hasMoreElements()){
				elem = en.nextElement();
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
			while (en.hasMoreElements()){
				sbuf.append(delim);
				elem = en.nextElement();
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
		} else if (target instanceof Collection){
			Iterator it = ((Collection)target).iterator();
			if (it.hasNext()){
				elem = it.next();
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
			while (it.hasNext()){
				sbuf.append(delim);
				elem = it.next();
				if (elem != null){
					sbuf.append(elem.toString());
				}
			}
		} else if (target instanceof Generator){
			class F extends PnutsFunction {
				boolean first = true;
				protected Object exec(Object[] args, Context c){
					Object arg = args[0];
					if (arg != null){
						if (first){
							first = false;
						} else {
							sbuf.append(delim);
						}
						sbuf.append(arg.toString());
					}
					return null;
				}
			}
			((Generator)target).apply(new F(), context);
		} else if (target == null){
			return "";
		} else {
			throw new IllegalArgumentException(String.valueOf(target));
		}
		return sbuf.toString();
	}

	static void call(final Object delim, Object target, final PnutsFunction func, final Context context){
		final Object[] arg = new Object[1];
		if (target instanceof Object[]){
			Object[] array = (Object[])target;
			int len = array.length;
			for (int i = 0; i < len - 1; i++){
				arg[0] = array[i];
				func.call(arg, context);
				arg[0] = delim;
				func.call(arg, context);
			} 
			if (len > 0){
				arg[0] = array[len - 1];
				func.call(arg, context);
			}
		} else if (target instanceof List){
			List lst = (List)target;
			int len = lst.size();
			for (int i = 0; i < len - 1; i++){
				arg[0] = lst.get(i);
				func.call(arg, context);
				arg[0] = delim;
				func.call(arg, context);
			} 
			if (len > 0){
				arg[0] = lst.get(len - 1);
				func.call(arg, context);
			}
		} else if (Runtime.isArray(target)){
			int len = Array.getLength(target);
			for (int i = 0; i < len - 1; i++){
				arg[0] = Array.get(target, i);
				func.call(arg, context);
				arg[0] = delim;
				func.call(arg, context);
			} 
			if (len > 0){
				arg[0] = Array.get(target, len - 1);
				func.call(arg, context);
			}
		} else if (target instanceof Iterator){
			Iterator it = (Iterator)target;
			if (it.hasNext()){
				arg[0] = it.next();
				func.call(arg, context);
			}
			while (it.hasNext()){
				arg[0] = delim;
				func.call(arg, context);
				arg[0] = it.next();
				func.call(arg, context);
			}
		} else if (target instanceof Enumeration){
			Enumeration en = (Enumeration)target;
			if (en.hasMoreElements()){
				arg[0] = en.nextElement();
				func.call(arg, context);
			}
			while (en.hasMoreElements()){
				arg[0] = delim;
				func.call(arg, context);
				arg[0] = en.nextElement();
				func.call(arg, context);
			}
		} else if (target instanceof Collection){
			Iterator it = ((Collection)target).iterator();
			if (it.hasNext()){
				arg[0] = it.next();
				func.call(arg, context);
			}
			while (it.hasNext()){
				arg[0] = delim;
				func.call(arg, context);
				arg[0] = it.next();
				func.call(arg, context);
			}
		} else if (target instanceof Generator){
			class F extends PnutsFunction {
				boolean first = true;
				protected Object exec(Object[] args, Context c){
					Object a0 = args[0];
					if (first){
						first = false;
					} else {
						arg[0] = delim;
						func.call(arg, context);
					}
					arg[0] = a0;
					func.call(arg, context);
					return null;
				}
			}
			((Generator)target).apply(new F(), context);
		} else if (target == null){
			// skip
		} else {
			throw new IllegalArgumentException();
		}
	}

	protected Object exec(Object args[], Context context){
		if (args.length == 2){
			Object delim = args[0];
			Object target = args[1];
			return concat(delim.toString(), target, context);
		} else if (args.length == 3){
			Object delim = args[0];
			Object target = args[1];
			PnutsFunction callback = (PnutsFunction)args[2];
			call(delim, target, callback, context);
			return null;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function join(delimiter, objects {, func(elem) } )";
	}
}
