/*
 * @(#)modifiers.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

public class modifiers implements Executable {

	static class ModifierChecker extends PnutsFunction {
		private int mask;

		public ModifierChecker(String name, int mask){
			super(name);
			this.mask = mask;
		}

		public boolean defined(int nargs){
			return nargs == 1;
		}

		protected Object exec(Object[] args, Context context){
			if (args.length != 1){
				undefined(args, context);
			}
			Object target = args[0];
			int modifiers;
			if (target instanceof Class){
				modifiers = ((Class)target).getModifiers();
			} else if (target instanceof Member){
				modifiers = ((Member)target).getModifiers();
			} else {
				throw new IllegalArgumentException();
			}
			if ((mask & modifiers) == mask){
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}
	}

	static String[] names = {"isAbstract", "isPublic", "isProtected", "isPrivate", "isStatic", "isInterface"};
	static int[] modifiers = {
		Modifier.ABSTRACT,
		Modifier.PUBLIC,
		Modifier.PROTECTED,
		Modifier.PRIVATE,
		Modifier.STATIC,
		Modifier.INTERFACE
	};

	public Object run(Context context){
		Package pkg = Package.getPackage("pnuts.lib", context);
		for (int i = 0; i < names.length; i++){
			pkg.set(names[i].intern(), new ModifierChecker(names[i], modifiers[i]), context);
		}
		return null;
	}
}
