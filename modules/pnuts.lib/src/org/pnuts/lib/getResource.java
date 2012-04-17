/*
 * @(#)getResource.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;

public class getResource extends PnutsFunction {
	public getResource(){
		super("getResource");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length == 1){
			return Pnuts.getResource((String)args[0], context);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function getResource(name)";
	}
}
