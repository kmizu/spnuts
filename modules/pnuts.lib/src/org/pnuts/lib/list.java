/*
 * @(#)list.java 1.4 05/01/24
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;
import java.lang.reflect.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;

/*
 * function list()
 * function list(arg)
 * function list(arg, type)
 */
public class list extends PnutsFunction {

	public list(){
		super("list");
	}

	static List getList(Context context, Object arg, String type){
		type = type.toUpperCase();
		Class c;
		if ("L".equals(type)){
			c = LinkedList.class;
		} else if ("A".equals(type)){
			c = ArrayList.class;
		} else if ("V".equals(type)){
			c = Vector.class;
		} else {
			throw new IllegalArgumentException();
		}
		List l;
		try {
			if (arg instanceof List){
				return (List)arg;
			} else if (arg instanceof Iterator){
				l = (List)c.newInstance();
				for (Iterator it = (Iterator)arg; it.hasNext(); ){
					l.add(it.next());
				} 
				return l;
			} else if (arg instanceof Enumeration){
				l = (List)c.newInstance();
				for (Enumeration en = (Enumeration)arg; en.hasMoreElements(); ){
					l.add(en.nextElement());
				} 
				return l;
			} else if (Runtime.isArray(arg)){
				l = (List)c.newInstance();
				int len = Runtime.getArrayLength(arg);
				for (int i = 0; i < len; i++){
					l.add(Array.get(arg, i));
				}
				return l;
			} else if (arg instanceof Generator){
				l = (List)c.newInstance();
				final List lst = l;
				PnutsFunction func = new PnutsFunction(){
						protected Object exec(Object[] args, Context ctx){
							lst.add(args[0]);
							return null;
						}
					};
				Runtime.applyGenerator((Generator)arg, func, context);
				return l;
			} else {
				Enumeration e = context.getConfiguration().toEnumeration(arg);
				if (e != null){
				    l = (List)c.newInstance();
				    for (Enumeration en = (Enumeration)e; en.hasMoreElements(); ){
					l.add(en.nextElement());
				    } 
				    return l;
				} else {
				    throw new IllegalArgumentException(String.valueOf(arg));
				}
			}
		} catch (InstantiationException e1){
			throw new PnutsException(e1, context);
		} catch (IllegalAccessException e2){
			throw new PnutsException(e2, context);
		}
	}

	public boolean defined(int nargs){
		return nargs >= 0 && nargs <= 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			return new ArrayList();
		} else if (nargs == 1){
			return getList(context, args[0], "A");
		} else if (nargs == 2){
			if (!(args[1] instanceof String)){
				throw new IllegalArgumentException();
			}
			return getList(context, args[0], (String)args[1]);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function list({ arg {, type }})";
	}
}
