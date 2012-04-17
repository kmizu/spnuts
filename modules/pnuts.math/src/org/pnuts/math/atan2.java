/*
 * @(#)atan2.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.math;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;

public class atan2 extends PnutsFunction {

	public atan2(){
		super("atan2");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		return new Double(Math.atan2(((Number)args[0]).doubleValue(),
									 ((Number)args[1]).doubleValue()));
	}

	public String toString(){
		return "function atan2(x, y)";
	}
}
