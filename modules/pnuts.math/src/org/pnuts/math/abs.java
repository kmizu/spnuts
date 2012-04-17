/*
 * @(#)abs.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.math;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Runtime;

public class abs extends PnutsFunction {
	final static Integer ZERO = new Integer(0);

	public abs(){
		super("abs");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			Object arg = args[0];
			if (Runtime.gt(ZERO, arg)){
				return Runtime.negate(arg);
			} else {
				return arg;
			}
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "fuction abs(number)";
	}
}
