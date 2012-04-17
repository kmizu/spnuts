/*
 * iterable.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.Generator;
import pnuts.lang.PnutsFunction;

public class iterable extends PnutsFunction {

	public iterable(){
		super("iterable");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object[] args, Context context){
		if (args.length == 1){
			Object obj = args[0];
			return (obj instanceof Generator) || (context.getConfiguration().toEnumeration(obj) != null)
			    ? Boolean.TRUE
			    : Boolean.FALSE;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function iterable(obj)";
	}
}
