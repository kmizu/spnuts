/*
 * @(#)round.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.math;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;

public class round extends PnutsFunction {

	public round(){
		super("round");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		Object arg = args[0];
		if (arg instanceof Float){
			return new Integer(Math.round(((Float)arg).floatValue()));
		} else {
			return new Long(Math.round(((Number)arg).doubleValue()));
		}
	}

	public String toString(){
		return "function round(number)";
	}
}
