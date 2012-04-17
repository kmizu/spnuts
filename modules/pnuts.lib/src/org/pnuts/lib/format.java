/*
 * @(#)format.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;

public class format extends PnutsFunction {

	public format(){
		super("format");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object args[], Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		return pnuts.lang.Pnuts.format(args[0]);
	}

	public String toString(){
		return "function format(obj)";
	}
}
