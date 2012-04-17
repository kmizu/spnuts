/*
 * @(#)unparse.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsFunction;

public class unparse extends PnutsFunction {

	public unparse(){
		super("unparse");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object args[], Context context){
		if (args.length == 1){
			Object arg = args[0];
			if (!(arg instanceof Pnuts)){
				throw new IllegalArgumentException();
			}
			return ((Pnuts)arg).unparse();
		} else {
			undefined(args, context);
			return null;
		}
	}
	public String toString(){
		return "function unparse(Pnuts)";
	}
}
