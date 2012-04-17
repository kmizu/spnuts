/*
 * @(#)isFunction.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class isFunction extends PnutsFunction {

	public isFunction(){
		super("isFunction");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			return new Boolean(args[0] instanceof PnutsFunction);
		} else if (nargs == 2){
			Object arg = args[0];
			int arity = ((Integer)args[1]).intValue();
			if (arg instanceof PnutsFunction){
				PnutsFunction func = (PnutsFunction)arg;
				if (arity >= 0 &&
					(func.defined(arity) || func.defined(-1)))
				{
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function isFunction(obj {, nargs })";
	}
}
