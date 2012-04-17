/*
 * @(#)makeProxy.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.compiler.DynamicRuntime;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

public class makeProxy extends PnutsFunction {

	public makeProxy(){
		super("makeProxy");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		Object target = args[0];
		if (target instanceof Method){
			return DynamicRuntime.makeProxy((Method)target);
		} else if (target instanceof Constructor){
			return DynamicRuntime.makeProxy((Constructor)target);
		} else {
			throw new IllegalArgumentException(String.valueOf(target));
		}
	}

	public String toString(){
		return "function makeProxy(methodOrConstructor)";
	}
}
