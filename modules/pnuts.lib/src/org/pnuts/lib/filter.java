/*
 * filter.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.ref.SoftReference;
import org.pnuts.lang.ConstraintsTransformer;
import pnuts.compiler.Compiler;

public class filter extends PnutsFunction {

	public filter(){
		super("filter");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	static class MyPnuts extends Pnuts {
		MyPnuts(SimpleNode n){
			this.startNodes = n;
		}
	}

	static PnutsFunction buildFunc(String expr, Context context) throws ParseException {
	    SimpleNode startNode;
	    try {
		startNode = new PnutsParser(new StringReader(expr)).StartSet(new ParseEnvironment(){
				public void handleParseException(ParseException e) throws ParseException {
					throw e;
				}
			});
	    } catch (Exception e){
		throw new InternalError();
	    }
		SimpleNode converted = ConstraintsTransformer.buildExpression(startNode);
		MyPnuts mp = new MyPnuts(converted);
		Compiler compiler = new Compiler("_filter", false, true);
		Pnuts compiled = compiler.compile(mp, context);
		return (PnutsFunction)compiled.run(context);
	}

	private final static String FILTER_SYMBOL = "pnuts.lib.filter.condition".intern();

	protected Object exec(Object[] args, final Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		Object target = args[0];
		Object cond = args[1];
		PnutsFunction func = null;

		if (cond instanceof PnutsFunction){
			func = (PnutsFunction)cond;
		} else if (cond instanceof String){
			try {
				WeakHashMap whm;
				synchronized (context){
					whm = (WeakHashMap)context.get(FILTER_SYMBOL);
					if (whm == null){
						context.set(FILTER_SYMBOL, whm = new WeakHashMap());
					}
				}
				SoftReference ref = (SoftReference)whm.get(cond);
				if (ref != null){
					func = (PnutsFunction)ref.get();
				}
				if (func == null){
					func = buildFunc((String)cond, context);
					whm.put(cond, new SoftReference(func));
				}
			} catch (ParseException e){
				throw new PnutsException(e, context);
			}
		} else {
			throw new IllegalArgumentException(String.valueOf(cond));
		}

		final PnutsFunction function = func;
	
		class _Iterator extends FilterIterator {
			protected _Iterator(){
			}
		
			public _Iterator(Iterator it){
				super(it);
			}
		
			protected boolean shouldInclude(Object obj){
				return ((Boolean)function.call(new Object[]{obj}, context)).booleanValue();
			}
		}

		class _Enumeration extends FilterEnumeration {
			protected _Enumeration(){
			}
		
			public _Enumeration(Enumeration en){
				super(en);
			}
		
			protected boolean shouldInclude(Object obj){
				return ((Boolean)function.call(new Object[]{obj}, context)).booleanValue();
			}
		}

		class _Map extends FilterMap {
			protected _Map(Map m, boolean dual){
				super(m, dual);
			}

			protected boolean shouldInclude(Object key){
				return ((Boolean)function.call(new Object[]{key}, context)).booleanValue();
			}

			protected boolean shouldInclude(Object key, Object value){
				return ((Boolean)function.call(new Object[]{key, value}, context)).booleanValue();
			}
		}

		if (target instanceof Iterator){
			return new _Iterator((Iterator)target);
		} else if (target instanceof Enumeration){
			return new _Enumeration((Enumeration)target);
		} else if (target instanceof Collection){
			return new _Iterator(((Collection)target).iterator());
		} else if (target instanceof Object[]){
			Object[] array = (Object[])target;
			ArrayList lst = new ArrayList();
			for (int i = 0; i < array.length; i++){
				Object elem = array[i];
				if (((Boolean)function.call(new Object[]{elem}, context)).booleanValue()){
					lst.add(elem);
				}
			}
			return lst.iterator();
		} else if (target instanceof Generator){
			final Generator g = (Generator)target;
			return new Generator() {
					public Object apply(final PnutsFunction closure, final Context context){
						return g.apply(new PnutsFunction(){
								protected Object exec(Object[] a, Context ctx){
									if (((Boolean)function.call(a, ctx)).booleanValue()){
										closure.call(a, context);
									}
									return null;
								}
							}, context);
					}
				};
		} else if (Runtime.isArray(target)){
			ArrayList lst = new ArrayList();
			int len = Runtime.getArrayLength(target);
			for (int i = 0; i < len; i++){
				Object elem = Array.get(target, i);
				if (((Boolean)function.call(new Object[]{elem}, context)).booleanValue()){
					lst.add(elem);
				}
			}
			return lst.iterator();
		} else if (target instanceof Map){
			if (function.defined(2)){
				return new _Map((Map)target, true);
			} else if (function.defined(1)){
				return new _Map((Map)target, false);
			} else {
				throw new IllegalArgumentException(String.valueOf(function));
			}
		} else {
			Enumeration e = context.getConfiguration().toEnumeration(target);
			if (e != null){
			    return new _Enumeration(e);
			} else {
			    throw new IllegalArgumentException(String.valueOf(target));
			}
		}
	}

	public String toString(){
		return "function filter( (Collection|Iterator|Enumeration|array|Generator|Map) , condition)";
	}
}
