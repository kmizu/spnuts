/*
 * @(#)setProperty.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;

/*
 * function setProperty(propertyName, value)
 */
public class setProperty extends PnutsFunction {

	public setProperty(){
		super("setProperty");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		return System.setProperty((String)args[0], (String)args[1]);
	}

	public String toString(){
		return "function setProperty(propertyName, value)";
	}
}
