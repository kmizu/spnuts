/*
 * @(#)formatTime.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.Date;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class formatTime extends PnutsFunction {

	public formatTime(){
		super("formatTime");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}
	
	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			return DateTimeFormat.formatTime((Date)args[0], context);
		} else if (nargs == 2){
			return DateTimeFormat.formatTime((Date)args[0], (String)args[1], context);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function formatTime(date {, style })";
	}
}
