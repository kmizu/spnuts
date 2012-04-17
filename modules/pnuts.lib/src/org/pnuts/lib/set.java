/*
 * set.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
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
 * function set()
 * function set(arg)
 * function set(arg, type)
 * function set(arg, func)
 * function set(arg, func, intValue)
 */
public class set extends PnutsFunction {

	public set(){
		super("set");
	}

	static Set createSet(String type, Context context){
		try {
			type = type.toUpperCase();
			Class c;
			if ("H".equals(type)){
				c = HashSet.class;
			} else if ("LH".equals(type)){
				c = LinkedHashSet.class;
			} else if ("T".equals(type)){
				c = TreeSet.class;
			} else {
				throw new IllegalArgumentException();
			}
			return (Set)c.newInstance();
		} catch (InstantiationException e1){
			throw new PnutsException(e1, context);
		} catch (IllegalAccessException e2){
			throw new PnutsException(e2, context);
		}
	}

	static Set getSet(Context context, Object arg, String type){
	    Set set = createSet(type, context);
	    populate(context, arg, set);
	    return set;
	}

	static void populate(Context context, Object arg, Set set){
		if (arg == null){
		    return;
		} else if (arg instanceof Collection){
			set.addAll((Collection)arg);
		} else if (arg instanceof Iterator){
			for (Iterator it = (Iterator)arg; it.hasNext(); ){
				set.add(it.next());
			} 
		} else if (arg instanceof Enumeration){
			for (Enumeration en = (Enumeration)arg; en.hasMoreElements(); ){
				set.add(en.nextElement());
			} 
		} else if (Runtime.isArray(arg)){
			int len = Runtime.getArrayLength(arg);
			for (int i = 0; i < len; i++){
				set.add(Array.get(arg, i));
			}
		} else if (arg instanceof Generator){
			final Set s = set;
			PnutsFunction func = new PnutsFunction(){
					protected Object exec(Object[] args, Context ctx){
						s.add(args[0]);
						return null;
					}
				};
			Runtime.applyGenerator((Generator)arg, func, context);
		} else {
			Enumeration e = context.getConfiguration().toEnumeration(arg);
			for (Enumeration en = (Enumeration)arg; en.hasMoreElements(); ){
				set.add(en.nextElement());
			} 
		}

	}

	public boolean defined(int nargs){
		return nargs >= 0 && nargs <= 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			return new HashSet();
		} else if (nargs == 1){
			return getSet(context, args[0], "H");
		} else if (nargs == 2){
			Object arg1 = args[1];
			if (arg1 instanceof PnutsFunction){ // TreeSet
			    Set s = new TreeSet(new FunctionComparator((PnutsFunction)arg1, context));
			    populate(context, args[0], s);
			    return s;
			} else if (arg1 instanceof String){
			    return getSet(context, args[0], (String)args[1]);
			} else {
			    throw new IllegalArgumentException();
			}
		} else if (nargs == 3){
			Object arg1 = args[1];
			if (arg1 instanceof PnutsFunction){ // TreeSet
			    Object arg2 = args[2];
			    if (arg2 instanceof Number){
				Set s = new TreeSet(new FunctionComparator((PnutsFunction)arg1,
									   context,
									   ((Number)arg2).intValue()));
				populate(context, args[0], s);
				return s;
			    }
			}
			throw new IllegalArgumentException();
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function set({ arg {, type }}) or (arg, func {, num})";
	}
}
