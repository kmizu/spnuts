/*
 * @(#)_threadPool.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

/*
 * function threadPool(max)
 * function threadPool(max, min)
 * function threadPool(max, min, keepalive)
 */
public class _threadPool extends PnutsFunction {
	private final static long DEFAULT_KEEPALIVE = 1000;
	
	public _threadPool(){
		super("threadPool");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2 || nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		int max, min = 1;
		long keepalive = DEFAULT_KEEPALIVE;
		switch (nargs){
		case 3:
			keepalive = ((Number)args[2]).longValue();
		case 2:
			min = ((Integer)args[1]).intValue();
		case 1:
			max = ((Integer)args[0]).intValue();
			break;
		default:
			undefined(args, context);
			return null;
		}
		return new ThreadPool(max, min, keepalive);
	}

	public String toString(){
		return "function threadPool( max {, min {, keepalive }} )";
	}
}
