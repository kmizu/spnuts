/*
 * @(#)getLocale.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import java.util.Locale;
import java.util.StringTokenizer;

public class getLocale extends PnutsFunction {

	public getLocale(){
		super("getLocale");
	}

	public boolean defined(int nargs){
		return nargs == 0 || nargs == 1;
	}
	
	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			return locale.getLocale(context);
		} else if (nargs == 1){
			return locale.toLocale(args[0]);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function getLocale( { loc } )";
	}
}
