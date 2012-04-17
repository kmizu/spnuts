/*
 * @(#)_interface.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import pnuts.lang.Package;
import pnuts.lang.Runtime;
import pnuts.compiler.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.pnuts.lang.SubtypeGenerator;
import org.pnuts.lang.ClassFileLoader;

public class _interface extends PnutsFunction {

	private ClassFileHandler handler;

	public _interface(){
		this(null);
	}

	public _interface(ClassFileHandler handler){
		super("interface");
		this.handler = handler;
	}

	public boolean defined(int nargs){
		return nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 3){
			undefined(args, context);
			return null;
		}
		String className = (String)args[0];

		Object supertypes = args[1];
		ArrayList list;

		Enumeration en;
		list = new ArrayList();
		if (supertypes != null){
			en = Runtime.toEnumeration(supertypes, context);
			if (en == null){
				throw new IllegalArgumentException(String.valueOf(supertypes));
			}
			while (en.hasMoreElements()){
				Object type = en.nextElement();
				if (!(type instanceof Class) || !((Class)type).isInterface()){
					throw new IllegalArgumentException(String.valueOf(type));
				}
				list.add(type);
			}
		}
		Class[] superInterfaces = new Class[list.size()];
		list.toArray(superInterfaces);

		Object arg2 = args[2];
		list = new ArrayList();
		en = Runtime.toEnumeration(arg2, context);
		if (en == null){
			throw new IllegalArgumentException(String.valueOf(arg2));
		}
		while (en.hasMoreElements()){
			Object sig = en.nextElement();
			if (!(sig instanceof String)){
				throw new IllegalArgumentException(String.valueOf(sig));
			}
			list.add(sig);
		}
		String[] signatures = new String[list.size()];
		list.toArray(signatures);

		try {
			if (handler == null){
				ClassLoader cl = SubtypeGenerator.mergeClassLoader(superInterfaces, context.getClassLoader());
				handler = new ClassFileLoader(cl);
				context.setClassLoader((ClassLoader)handler);
			}
			ClassFile cf =
				SubtypeGenerator.getClassFileForInterface(className,
														  superInterfaces,
														  signatures,
														  context,
														  (short)(Constants.ACC_PUBLIC | Constants.ACC_INTERFACE));
			return handler.handle(cf);
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function interface(className, supertypes, signatures)";
	}
}
