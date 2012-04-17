/*
 * @(#)mapFunction.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.lang.reflect.Array;
import java.util.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;
import pnuts.lang.Package;

public class mapFunction extends PnutsFunction {

	public mapFunction(){
		super("mapFunction");
	}
	
	public boolean defined(int nargs){
		return (nargs == 2 || nargs == 3);
	}

	protected Object exec(Object args[], final Context context){
		int nargs = args.length;
		if (nargs != 2 && nargs != 3){
			undefined(args, context);
		}
		final PnutsFunction func = (PnutsFunction)args[0];
		Object array = args[1];

		int len = 0;
		int type1 = 0; // 1==array, 2==Collection, 3==Iterator, 4==Enumeration, 5==Map
		if (Runtime.isArray(array)){
			len = Runtime.getArrayLength(array);
			type1 = 1;
		} else if (array instanceof Collection){
			len = ((Collection)array).size();
			type1 = 2;
		} else if (array instanceof Iterator){
			type1 = 3;
		} else if (array instanceof Enumeration){
			type1 = 4;
		} else if (array instanceof Map){
			type1 = 5;
		} else if (array instanceof Package){
			type1 = 6;
		} else if (array instanceof Generator){
			type1 = 7;
		} else {
			throw new IllegalArgumentException();
		}

		Collection output = null;
		if (nargs == 3){
			if (args[2] instanceof Collection){
				output = (Collection)args[2];
			} else {
				throw new IllegalArgumentException();
			}
		}
		   
		if (type1 == 1){ // array
			if (output == null){
				for (int i = 0; i < len; ++i){
					func.call(new Object[]{Array.get(array, i)}, context);
				}
			} else {
				for (int i = 0; i < len; ++i){
					output.add(func.call(new Object[]{Array.get(array, i)}, context));
				}
			}
		} else if (type1 == 2){ // Collection
			if (output == null){
				for (Iterator it = ((Collection)array).iterator(); it.hasNext(); ){
					func.call(new Object[]{it.next()}, context);
				}
			} else {
				for (Iterator it = ((Collection)array).iterator(); it.hasNext(); ){
					output.add(func.call(new Object[]{it.next()}, context));
				}
			}
		} else if (type1 == 3){ // Iterator
			if (output == null){
				for (Iterator it = (Iterator)array; it.hasNext(); ){
					func.call(new Object[]{it.next()}, context);
				}
			} else {
				for (Iterator it = (Iterator)array; it.hasNext(); ){
					output.add(func.call(new Object[]{it.next()}, context));
				}
			}
		} else if (type1 == 4){ // Enumeration
			if (output == null){
				for (Enumeration e = (Enumeration)array; e.hasMoreElements(); ){
					func.call(new Object[]{e.nextElement()}, context);
				}
			} else {
				for (Enumeration e = (Enumeration)array; e.hasMoreElements(); ){
					output.add(func.call(new Object[]{e.nextElement()}, context));
				}
			}
		} else if (type1 == 5){ // Map
			if (output == null){
				for (Iterator it = ((Map)array).entrySet().iterator(); it.hasNext(); ){
					Map.Entry entry = (Map.Entry)it.next();
					func.call(new Object[]{entry.getKey(), entry.getValue()}, context);
				}
			} else {
				for (Iterator it = ((Map)array).entrySet().iterator(); it.hasNext(); ){
					Map.Entry entry = (Map.Entry)it.next();
					output.add(func.call(new Object[]{entry.getKey(), entry.getValue()}, context));
				}
			}
		} else if (type1 == 6){ // Package
			if (output == null){
				for (Enumeration e = ((Package)array).bindings(); e.hasMoreElements(); ){
					NamedValue value = (NamedValue)e.nextElement();
					func.call(new Object[]{value.getName(), value.get()}, context);
				}
			} else {
				for (Enumeration e = ((Package)array).bindings(); e.hasMoreElements(); ){
					NamedValue value = (NamedValue)e.nextElement();
					output.add(func.call(new Object[]{value.getName(), value.get()}, context));
				}
			}
		} else if (type1 == 7){
			Generator g = (Generator)array;
			if (output == null){
				return Runtime.applyGenerator(g,
											  new PnutsFunction(){
												  protected Object exec(Object[] args, Context ctx){
													  func.call(args, ctx);
													  return null;
												  }
											  }, context);
			} else {
				final Collection output2 = output;
				return Runtime.applyGenerator(g,
											  new PnutsFunction(){
												  protected Object exec(Object[] args, Context ctx){
													  output2.add(func.call(args, ctx));
													  return null;
												  }
											  }, context);
			}
		}
		return null;
	}

	public String toString(){
		return "function mapFunction(func, (array|Collection|Iterator|Enumeration|Map|Package|Generator ) {, Collection } )";
	}
}
