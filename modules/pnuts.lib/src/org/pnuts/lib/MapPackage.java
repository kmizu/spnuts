/*
 * @(#)MapPackage.java 1.3 05/02/17
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.Map;
import java.util.Collections;
import java.util.Enumeration;
import pnuts.lang.Pnuts;
import pnuts.lang.NamedValue;
import pnuts.lang.PnutsImpl;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Package;

/**
 * A wrapper class that makes pnuts.lang.Package from java.util.Map
 */
class MapPackage extends Package {

	private Map map;
	private static PnutsImpl pureImpl = new PnutsImpl();

	/*
	 * Constructor
	 *
	 * @param map the map
	 */
	public MapPackage(Map map){
		this.map = map;
	}

	/**
	 * Add key-value mappings from a Pnuts expression <em>def</em> to the specified <em>map</em>.
	 *
	 * @param map the map
	 * @param def a Pnuts expression
	 * @param context the context in which the Pnuts expression is evaluated.
	 */
	public static void defineMap(Map map, String def, Context context){
		Context ctx = new Context(context);
		ctx.setImplementation(pureImpl);
		ctx.setCurrentPackage(new MapPackage(map));
		Pnuts.eval(def, ctx);
	}

	public Object get(String symbol, Context context){
		return map.get(symbol);
	}

	public void set(String symbol, Object value, Context context){
		map.put(symbol, value);
	}

	public boolean defined(String name, Context context){
		return map.containsKey(name);
	}

	public Enumeration keys(){
		return Collections.enumeration(map.keySet());
	}

	public Enumeration values(){
		return Collections.enumeration(map.values());
	}

	public int size(){
		return map.size();
	}

	public NamedValue lookup(final String symbol, Context context){
		final Object value = map.get(symbol);
		if (value != null){
			return new NamedValue(){
					public String getName(){
						return symbol;
					}
					public Object get(){
						return value;
					}
					public void set(Object obj){
						map.put(symbol, obj);
					}
				};
		} else {
			return null;
		}
	}

	public static class Function extends PnutsFunction {
		protected Object exec(Object[] args, Context context){
			if (args.length != 2){
				undefined(args, context);
				return null;
			}
			Map m = (Map)args[0];
			String def = (String)args[1];
			defineMap(m, def, context);
			return m;
		}
	}
}
