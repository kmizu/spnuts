/*
 * @(#)currentTimeMillis.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;

public class currentTimeMillis extends PnutsFunction {

	public currentTimeMillis(){
		super("currentTimeMillis");
	}

	public boolean defined(int nargs){
		return nargs == 0;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 0){
			undefined(args, context);
			return null;
		}
		return new Long(System.currentTimeMillis());
	}

	public String toString(){
		return "function currentTimeMillis()";
	}
}
