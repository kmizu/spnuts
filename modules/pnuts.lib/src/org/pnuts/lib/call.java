/*
 * @(#)call.java 1.3 05/04/20
 *
 * Copyright (c) 2004-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.util.*;

public class call extends PnutsFunction {

	public call(){
		super("call");
	}

	public call(String name){
		super(name);
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object args[], Context context){
		int nargs = args.length;
		Object target;
		Object a1 = null;
		Object[] arguments;
		if (nargs == 1){
			target = args[0];
		} else if (nargs == 2){
			target = args[0];
			a1 = args[1];
		} else {
			undefined(args, context);
			return null;
		}
		if (a1 == null){
			arguments = new Object[]{};
		} else if (a1 instanceof Object[]){
			arguments = (Object[])a1;
		} else if (a1 instanceof List){
			arguments = ((List)a1).toArray();
		} else if (a1 instanceof Collection){
			ArrayList list = new ArrayList((Collection)a1);
			arguments = list.toArray();
		} else if (a1 instanceof Iterator){
			ArrayList list = new ArrayList();
			for (Iterator it = (Iterator)a1; it.hasNext();){
				list.add(it.next());
			}
			arguments = list.toArray();
		} else if (a1 instanceof Enumeration){
			ArrayList list = new ArrayList();
			for (Enumeration en = (Enumeration)a1; en.hasMoreElements();){
				list.add(en.nextElement());
			}
			arguments = list.toArray();
		} else if (a1 instanceof Generator){
			Generator g = (Generator)a1;
			final ArrayList list = new ArrayList();
			g.apply(new PnutsFunction(){
					protected Object exec(Object[] args, Context context){
						list.add(args[0]);
						return null;
					}
				}, context);
			arguments = list.toArray();
		} else {
			throw new IllegalArgumentException(a1.getClass() + " (" + String.valueOf(a1) + ")");
		}
		return Runtime.call(context, target, arguments, null);
	}

	public String toString(){
		return "function call(callableOrClass {, arguments })";
	}
}
