/*
 * @(#)javaAdapter.java 1.2 04/12/06
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
import java.util.Enumeration;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import org.pnuts.lang.SubtypeGenerator;

public class javaAdapter extends PnutsFunction {

	public javaAdapter(){
		super("javaAdapter");
	}

	public boolean defined(int nargs){
		return nargs >= 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length < 2){
			undefined(args, context);
			return null;
		}
		Object supertypes = args[0];
		Class superclass = null;
		ArrayList interfaces = null;
		Class c;
		if (Runtime.isArray(supertypes) || (supertypes instanceof Collection)){
			interfaces = new ArrayList();
			Enumeration e = Runtime.toEnumeration(supertypes, context);
			while (e.hasMoreElements()){
				c = (Class)e.nextElement();
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
			c = (Class)supertypes;
			if (c.isInterface()){
				interfaces = new ArrayList();
				interfaces.add(c);
			} else {
				superclass = c;
			}
		}
		Object arg1 = args[1];
		Package pkg = null;
		if (arg1 instanceof Map){
			pkg = new MapPackage((Map)arg1);
		} else if (arg1 instanceof Package){
			pkg = (Package)arg1;
		} else {
			throw new IllegalArgumentException(String.valueOf(arg1));
		}
		Object[] a;
		if (args.length > 2){
			a = new Object[args.length - 2];
			System.arraycopy(args, 2, a, 0, args.length - 2);
		} else {
			a = new Object[]{};
		}
		Class[] array = null;
		if (interfaces != null){
			array = new Class[interfaces.size()];
			interfaces.toArray(array);
		}
		return SubtypeGenerator.instantiateSubtype(context, superclass, array, pkg, a);
	}

	public String toString(){
		return "function javaAdapter((supertype|supertypes[]), pkgOrMap, args...)";
	}
}
