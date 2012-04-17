/*
 * @(#)DateElement.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.Date;
import java.util.Calendar;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;

class DateElement extends PnutsFunction {
	private int element;

	DateElement(int element, String name){
		super(name);
		this.element = element;
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		Date d = (Date)args[0];
		Calendar c = date.getCalendar(d, context);
		if (element == Calendar.MONTH){
			return new Integer(c.get(element) + 1);
		} else {
			return new Integer(c.get(element));
		}
	}

	public String toString(){
		return "function " + name + "(Date)";
	}
}
