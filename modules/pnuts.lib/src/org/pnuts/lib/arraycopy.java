/*
 * @(#)arraycopy.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.io.*;
import java.lang.reflect.*;

public class arraycopy extends PnutsFunction {

	public arraycopy(){
		super("arraycopy");
	}

	public boolean defined(int narg){
		return (narg == 2 || narg == 5);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 2){
			Object src = args[0];
			System.arraycopy(src, 0, args[1], 0, Runtime.getArrayLength(src));
		} else if (nargs == 5){
			System.arraycopy(args[0],
							 ((Integer)args[1]).intValue(),
							 args[2],
							 ((Integer)args[3]).intValue(),
							 ((Integer)args[4]).intValue());
		} else {
			undefined(args, context);
		}
		return null;
	}

	public String toString(){
		return "function arraycopy(src, dest) or (src, srcindex, dst, dstindex, len)";
	}
}
