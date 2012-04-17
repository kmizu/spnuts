/*
 * @(#)getProperty.java 1.2 04/12/06
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
 * function getProperty(propertyName)
 */
public class getProperty extends PnutsFunction {

	public getProperty(){
		super("getProperty");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		return System.getProperty((String)args[0]);
	}

	public String toString(){
		return "function getProperty(propertyName)";
	}
}
