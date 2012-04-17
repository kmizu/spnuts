/*
 * @(#)printf.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import pnuts.lang.*;

public class printf extends PnutsFunction {

	public printf(){
		super("printf");
	}

	public boolean defined(int nargs){
		return nargs > 0;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			undefined(args, context);
			return null;
		}
		Object[] fargs = new Object[nargs - 1];
		System.arraycopy(args, 1, fargs, 0, nargs - 1);
		PnutsFunction print = (PnutsFunction)context.resolveSymbol("print");
		if (print == null){
			throw new RuntimeException();
		}
		print.call(new Object[]{new PrintfFormat((String)args[0]).sprintf(fargs)}, context);
		return null;
	}
}
