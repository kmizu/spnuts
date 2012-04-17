/*
 * @(#)rootScope.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Package;
import pnuts.lang.Context;

public class rootScope extends PnutsFunction {

	public rootScope(){
		super("rootScope");
	}

	public boolean defined(int narg){
		return (narg == 0);
	}

	protected Object exec(Object args[], Context context){
		if (args.length != 0){
			undefined(args, context);
			return null;
		}
		Package p = context.getCurrentPackage();
		Package parent = p.getParent();
		while (parent != null){
			p = parent;
			parent = p.getParent();
		}
		return p;
	}

	public String toString(){
		return "function rootScope()";
	}
}
