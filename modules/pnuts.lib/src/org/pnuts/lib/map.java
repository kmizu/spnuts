/*
 * map.java
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
import pnuts.lang.Package;

/*
 * function map()
 * function map(arg)
 * function map(arg, type)
 * function map(arg, func)
 * function map(arg, func, intValue)
 */
public class map extends PnutsFunction {

	private final static Integer ZERO = new Integer(0);
	private final static Integer ONE = new Integer(1);

	public map(){
		super("map");
	}

	static Map createMap(String type, Context context){
		try {
			type = type.toUpperCase();
			Class c;
			if ("H".equals(type)){
				c = HashMap.class;
			} else if ("HT".equals(type)){
				c = Hashtable.class;
			} else if ("LH".equals(type)){
				c = LinkedHashMap.class;
			} else if ("WH".equals(type)){
				c = WeakHashMap.class;
			} else if ("T".equals(type)){
				c = TreeMap.class;
			} else if ("P".equals(type)){
				c = Properties.class;
			} else if ("IH".equals(type)){
				c = IdentityHashMap.class;
			} else {
				throw new IllegalArgumentException();
			}
			return (Map)c.newInstance();
		} catch (InstantiationException e1){
			throw new PnutsException(e1, context);
		} catch (IllegalAccessException e2){
			throw new PnutsException(e2, context);
		}
	}

	static Map getMap(Context context, Object arg, String type){
		Map m = createMap(type, context);
		populate(context, arg, m);
		return m;
	}

	static Map populate(Context context, Object arg, Map map){
		final Configuration conf = context.getConfiguration();
		if (arg == null){
		    return map;
		} else if (arg instanceof Map){
			map.putAll((Map)arg);
			return map;
		} else if (arg instanceof Iterator){
			for (Iterator it = (Iterator)arg; it.hasNext(); ){
				Object elem = it.next();
				map.put(conf.getElement(context, elem, ZERO),
						conf.getElement(context, elem, ONE));
			} 
			return map;
		} else if (arg instanceof Enumeration){
			for (Enumeration en = (Enumeration)arg; en.hasMoreElements(); ){
				Object elem = en.nextElement();
				map.put(conf.getElement(context, elem, ZERO),
						conf.getElement(context, elem, ONE));
			} 
			return map;
		} else if (Runtime.isArray(arg)){
			int len = Runtime.getArrayLength(arg);
			for (int i = 0; i < len; i++){
				Object elem = Array.get(arg, i);
				map.put(conf.getElement(context, elem, ZERO),
						conf.getElement(context, elem, ONE));
			}
			return map;
		} else if (arg instanceof List){
			List lst = (List)arg;
			int len = lst.size();
			for (int i = 0; i < len; i++){
				Object elem = lst.get(i);
				map.put(conf.getElement(context, elem, ZERO),
						conf.getElement(context, elem, ONE));
			}
			return map;
		} else if (arg instanceof Generator){
			final Map m = map;
			Runtime.applyGenerator((Generator)arg, new PnutsFunction() {
				protected Object exec(Object[] args, Context context){
				    Object elem = args[0];
				    m.put(conf.getElement(context, elem, ZERO),
					  conf.getElement(context, elem, ONE));
				    return null;
				}
			    }, context);
			return m;
		} else if (arg instanceof String){
			return (Map)new MapPackage.Function().call(new Object[]{map, arg}, context);
		} else if (arg instanceof Package){
			return ((Package)arg).asMap();
		} else {
		    throw new IllegalArgumentException(String.valueOf(arg));
		}
	}

	public boolean defined(int nargs){
		return nargs >= 0 && nargs <= 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			return new HashMap();
		} else if (nargs == 1){
			return getMap(context, args[0], "H");
		} else if (nargs == 2){
		    Object arg1 = args[1];
		    if (arg1 instanceof PnutsFunction){
			Map m = new TreeMap(new FunctionComparator((PnutsFunction)arg1, context));
			m = populate(context, args[0], m);
			return m;
		    } else if (arg1 instanceof String){
			return getMap(context, args[0], (String)args[1]);
		    } else {
			throw new IllegalArgumentException();
		    }
		} else if (nargs == 3){
			Object arg1 = args[1];
			if (arg1 instanceof PnutsFunction){ // TreeSet
			    Object arg2 = args[2];
			    if (arg2 instanceof Number){
				Map m = new TreeMap(new FunctionComparator((PnutsFunction)arg1,
									   context,
									   ((Number)arg2).intValue()));
				populate(context, args[0], m);
				return m;
			    }
			}
			throw new IllegalArgumentException();
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function map({ arg {, type }}) or (arg, func {, num})";
	}
}
