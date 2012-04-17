/*
 * @(#)subclass.java 1.3 05/04/29
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
import pnuts.lang.Package;
import pnuts.lang.Runtime;
import pnuts.compiler.ClassFile;
import pnuts.compiler.ClassFileHandler;
import java.util.Enumeration;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.pnuts.lang.ClassFileLoader;
import org.pnuts.lang.SubtypeGenerator;

public class subclass extends PnutsFunction {

	private ClassFileHandler handler;

	public subclass(){
		this(null);
	}

	public subclass(ClassFileHandler handler){
		super("subclass");
		this.handler = handler;
	}

	public boolean defined(int nargs){
		return nargs >= 2 && nargs <= 5;
	}

	static Class parseSupertypes(Object supertypes, List interfaces, Context context){
		Class superclass = null;
		if (supertypes == null){
			superclass = Object.class;
		} else if (Runtime.isArray(supertypes) || (supertypes instanceof Collection)){
			Enumeration e = Runtime.toEnumeration(supertypes, context);
			while (e.hasMoreElements()){
				Class c = (Class)e.nextElement();
				if (c.isInterface()){
					interfaces.add(c);
				} else {
					if (superclass != null){
						throw new IllegalArgumentException("multiple inheritance: " + superclass.getName() + "," + c.getName());
					}
					superclass = c;
				}
			}
		} else {
			Class c = (Class)supertypes;
			if (c.isInterface()){
				interfaces.add(c);
			} else {
				superclass = c;
			}
		}
		if (superclass == null){
			return Object.class;
		} else {
			return superclass;
		}
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs < 2 || nargs > 5){
			undefined(args, context);
			return null;
		}
		int pos = 0;
		Object arg0 = args[0];
		String className;
		if (arg0 instanceof String){
			className = (String)arg0;
			pos++;
		} else {
			className = null;
		}
		Object supertypes = args[pos++];
		ArrayList interfaces = new ArrayList();
		Class superclass = parseSupertypes(supertypes, interfaces, context);

		Object arg1 = args[pos++];
		Package pkg = null;
		if (arg1 instanceof Map){
			pkg = new MapPackage((Map)arg1);
		} else if (arg1 instanceof Package){
			pkg = (Package)arg1;
		} else {
			throw new IllegalArgumentException(String.valueOf(arg1));
		}
		int mode = 0;
		if (pos <= nargs - 1){
			if (((Boolean)args[pos++]).booleanValue()){
				mode |= SubtypeGenerator.THIS;
			}
		}
		if (pos <= nargs - 1){
			if (((Boolean)args[pos++]).booleanValue()){
				mode |= SubtypeGenerator.SUPER;
			}
		}
		try {
			ClassFileHandler handler = this.handler;
			Class[] array = null;
			if (interfaces != null){
				array = new Class[interfaces.size()];
				interfaces.toArray(array);
			}
			if (handler == null){
				Class[] types = new Class[array.length + 1];
				types[0] = superclass;
				for (int i = 0; i < array.length; i++){
					types[i + 1] = array[i];
				}
				ClassLoader ccl = Thread.currentThread().getContextClassLoader();
				ClassLoader cl = SubtypeGenerator.mergeClassLoader(types, ccl);
				ClassFileLoader cfl = new ClassFileLoader(cl);
				handler = cfl;
				if (className == null){
					className = superclass.getName().replace('.', '_') + "$" + cfl.getId() + "$" + cfl.getClassCount();
				}

				ClassLoader newCl = (ClassLoader)handler;
				context.setClassLoader(newCl);
				Thread.currentThread().setContextClassLoader(newCl);
			}
			if (handler instanceof ClassFileLoader){
				ClassFileLoader cfl = (ClassFileLoader)handler;
				cfl.setup(pkg, context);
				if (className == null){
					className = superclass.getName().replace('.', '_') + "$" + cfl.getId() + "$" + cfl.getClassCount();
				}
//			} else {
//				mode |= SubtypeGenerator.SERIALIZE;
			}
			if (className == null){
				className = superclass.getName().replace('.', '_') + "__adapter";
			}

			ClassFile cf = SubtypeGenerator.getClassFileForSubclass(className,
										superclass,
										array,
										pkg,
										context,
										mode);
			return handler.handle(cf);
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function subclass({ className ,} (supertype|supertypes[]), pkgOrMap {, hasThis  { , hasSuper }} )";
	}
}
