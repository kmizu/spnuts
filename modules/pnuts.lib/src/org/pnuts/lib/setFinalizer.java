/*
 * @(#)setFinalizer.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Runtime;

/*
 * function setFinalizer(target, func())
 */
public class setFinalizer extends PnutsFunction {

	private final static Object[] NO_ARGS = new Object[]{};
	private final static String FINALIZER_KEY = "pnuts.lib.setFinalizer.key".intern();

	public setFinalizer(){
		super("setFinalizer");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	private static Runnable getFinalizeCommand(final PnutsFunction func, final Context context){
		final Thread currentThread = Thread.currentThread();
		final ClassLoader ccl = currentThread.getContextClassLoader();
		final ClassLoader cl = context.getClassLoader();
		final Context ctx = (Context)context.clone();
		return new Runnable(){
				public void run(){
					currentThread.setContextClassLoader(ccl);
					context.setClassLoader(cl);
					func.call(NO_ARGS, ctx);
				}
			};
	}

	protected Object exec(Object args[], Context context){
		if (args.length != 2){
			undefined(args, context);
		}
		Object target = args[0];
		PnutsFunction func = (PnutsFunction)args[1];
		Runnable cmd = getFinalizeCommand(func, context);
		Runtime.setElement(target, FINALIZER_KEY, Cleaner.create(target, cmd), context);
		return null;
	}

	public String toString(){
		return "function setFinalizer(target, func())";
	}
}
