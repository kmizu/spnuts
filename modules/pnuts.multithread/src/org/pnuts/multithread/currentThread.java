/*
 * @(#)currentThread.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class currentThread extends PnutsFunction {

	public currentThread(){
		super("currentThread");
	}

	public boolean defined(int narg){
		return (narg == 0);
	}

	protected Object exec(Object args[], Context context){
		if (args.length != 0){
			undefined(args, context);
			return null;
		}
		return Thread.currentThread();
	}

	public String toString(){
		return "function currentThread()";
	}
}
