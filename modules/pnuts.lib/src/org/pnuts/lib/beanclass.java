/*
 * @(#)beanclass.java 1.3 05/04/20
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import pnuts.compiler.ClassFile;
import pnuts.compiler.ClassFileHandler;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import org.pnuts.lang.SubtypeGenerator;
import org.pnuts.lang.ClassFileLoader;

public class beanclass extends PnutsFunction {

	private ClassFileHandler handler;

	public beanclass(){
		this(null);
	}

	public beanclass(ClassFileHandler handler){
		super("beanclass");
		this.handler = handler;
	}

	public boolean defined(int nargs){
		return nargs == 3 || nargs == 4;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 3 && nargs != 4){
			undefined(args, context);
			return null;
		}
		String className = (String)args[0];

		Object supertypes = args[1];
		ArrayList list;

		list = new ArrayList();
		Class superclass = subclass.parseSupertypes(supertypes, list, context);
		boolean serializable = false;
		for (Iterator it = list.iterator(); it.hasNext();){
			Class type = (Class)it.next();
			if (type.isAssignableFrom(java.io.Serializable.class)){
				serializable = true;
			}
		}
		if (!serializable){
			list.add(java.io.Serializable.class);
		}
		String[] superInterfaces = new String[list.size()];
		for (int i = 0; i < superInterfaces.length; i++){
			Class type = (Class)list.get(i);
			superInterfaces[i] = type.getName();
		}
		Map typeMap = (Map)args[2];
		String[] constructorParams;
		if (args.length == 4){
			Object arg3 = args[3];
			if (arg3 instanceof String[]){
				constructorParams = (String[])arg3;
			} else if (arg3 instanceof Object[]){
				int len = ((Object[])arg3).length;
				constructorParams = new String[len];
				System.arraycopy(arg3, 0, constructorParams, 0, len);
			} else if (arg3 instanceof List){
				List lst = (List)arg3;
				int len = lst.size();
				constructorParams = new String[len];
				for (int i = 0; i < len; i++){
					constructorParams[i] = (String)lst.get(i);
				}
			} else {
				throw new IllegalArgumentException(String.valueOf(arg3));
			}
		} else {
			constructorParams = null;
		}
		try {
			if (handler == null){
				Class[] types = new Class[list.size() + 1];
				types[0] = superclass;
				for (int i = 0; i < list.size(); i++){
					types[i + 1] = (Class)list.get(i);
				}
				ClassLoader ccl = Thread.currentThread().getContextClassLoader();
				ClassLoader cl = SubtypeGenerator.mergeClassLoader(types, ccl);
				handler = new ClassFileLoader(cl);
				ClassLoader newCl = (ClassLoader)handler;
				context.setClassLoader(newCl);
				Thread.currentThread().setContextClassLoader(newCl);
			}
			ClassFile cf = BeanClassGenerator.generateClassFile(typeMap,
									    className,
									    superclass.getName(),
									    superInterfaces,
									    constructorParams);
			return handler.handle(cf);
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function beanclass(className, superInterfaces, typeMap {, constructorParams })";
	}
}
