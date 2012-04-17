/*
 * @(#)fork.java 1.1 05/04/04
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class fork extends createThread {

	public fork(){
		super("fork");
	}

	public boolean defined(int nargs){
		return nargs >= 1 && nargs <= 3;
	}

	static Thread fork(PnutsFunction f,
			   int prio,
			   boolean daemon,
			   Context context)
	{
		Thread th = createThread(f, prio, daemon, context);
		th.start();
		return th;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			PnutsFunction f = (PnutsFunction)args[0];
			return fork(f, Thread.NORM_PRIORITY, false, context);
		} else if (nargs == 2){
			PnutsFunction f = (PnutsFunction)args[0];
			int prio = ((Number)args[1]).intValue();
			return fork(f, prio, false, context);
		} else if (nargs == 3){
			PnutsFunction f = (PnutsFunction)args[0];
			int prio = ((Number)args[1]).intValue();
			boolean daemon = ((Boolean)args[2]).booleanValue();
			return fork(f, prio, daemon, context);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function fork(func {, prio {, daemon }})";
	}
}
