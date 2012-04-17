/*
 * @(#)setBeanProperties.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.PnutsImpl;
import pnuts.lang.NamedValue;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;
import pnuts.lang.Runtime;
import pnuts.lang.Package;
import java.lang.reflect.*;
import java.beans.*;
import java.util.*;
import pnuts.beans.BeanUtil;

/**
 * Set properties at a time using list
 *   setBeanProperties(bean, [["prop1", value], ...])
 *   setBeanProperties(bean, $(`prop1=value; ...`))
 *   setBeanProperties(bean, "prop1=...;...")
 */
public class setBeanProperties extends PnutsFunction {

	private static PnutsImpl pureImpl = new PnutsImpl();

	public setBeanProperties(){
		super("setBeanProperties");
	}

	public boolean defined(int narg){
		return (narg == 2);
	}

	static Map getMap(Object arg, Context context){
		Map map = new HashMap();

		if (arg instanceof Object[]){
			Object[] array = (Object[])arg;
			for (int i = 0; i < array.length; i++){
				Object[] pair = (Object[])array[i];
				String name = (String)pair[0];
				Object value = pair[1];
				map.put(name, value);
			}
		} else if (arg instanceof String){
			Context ctx = new Context(context);
			Package pkg = new Package(null, null);
			ctx.setImplementation(pureImpl);
			ctx.setCurrentPackage(pkg);
			Pnuts.eval((String)arg, ctx);

			for (Enumeration _enum = pkg.bindings(); _enum.hasMoreElements(); ){
				NamedValue val = (NamedValue)_enum.nextElement();
				String name = val.getName();
				Object value = val.get();
				map.put(name, value);
			}
		} else if (arg instanceof Map){
			map = (Map)arg;
		} else if (arg instanceof Package){
			Package pkg = (Package)arg;
			for (Enumeration e = pkg.keys(); e.hasMoreElements(); ){
				String name = (String)e.nextElement();
				Object value = pkg.get(name, context);
				map.put(name, value);
			}
		} else {
			throw new IllegalArgumentException();
		}
		return map;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;

		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		Object bean = args[0];
		Object arg = args[1];
		Map map = getMap(arg, context);

		try {
			BeanUtil.setProperties(bean,  map);
		} catch (IntrospectionException e){
			throw new PnutsException(e, context);
		} catch (IllegalAccessException e2){
			throw new PnutsException(e2, context);
		} catch (InvocationTargetException e3){
			throw new PnutsException(e3, context);
		}
		return bean;
	}

	public String toString(){
		return "function setBeanProperties(bean, [[\"prop1\", value], ...]) or (bean, \"prop1=...;...\") or (bean, java.util.Map) or (bean, pnuts.lang.Package)";
	}
}
