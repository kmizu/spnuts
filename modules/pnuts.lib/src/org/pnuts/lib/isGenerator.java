/*
 * @(#)isGenerator.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Generator;

public class isGenerator extends PnutsFunction {

	public isGenerator(){
		super("isGenerator");
	}

	public boolean defined(int narg){
		return narg == 1;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			return new Boolean(args[0] instanceof Generator);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function isGenerator(obj)";
	}
}
