/*
 * @(#)calendar.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.Calendar;
import pnuts.lang.Executable;
import pnuts.lang.Context;
import pnuts.lang.Package;

public class calendar implements Executable {

	static String[] names0 = {
		"getYear",
		"getMonth",
		"getWeekOfYear",
		"getDayOfYear",
		"getHour",
		"getMinute",
		"getSecond",
		"getMillisecond",
		"getDayOfMonth",
		"getDayOfWeek"
	};

	static String[] names1 = {
		"addYear",
		"addMonth",
		"addWeek",
		"addDay",
		"addHour",
		"addMinute",
		"addSecond",
		"addMillisecond"
	};

	static String[] names2 = {
		"diffYear",
		"diffMonth",
		"diffWeek",
		"diffDay",
		"diffHour",
		"diffMinute",
		"diffSecond",
		"diffMillisecond"
	};

	static int[] elements = {
		Calendar.YEAR,
		Calendar.MONTH,
		Calendar.WEEK_OF_YEAR,
		Calendar.DAY_OF_YEAR,
		Calendar.HOUR_OF_DAY,
		Calendar.MINUTE,
		Calendar.SECOND,
		Calendar.MILLISECOND,
		Calendar.DAY_OF_MONTH,
		Calendar.DAY_OF_WEEK
	};

	public Object run(Context context){
		Package pkg = Package.getPackage("pnuts.lib", context);
		for (int i = 0; i < names0.length; i++){
			String name = names0[i].intern();
			pkg.set(name, new DateElement(elements[i], name), context);
			pkg.export(name);
		}
		for (int i = 0; i < names1.length; i++){
			String name = names1[i].intern();
			pkg.set(name, new DateElementAdd(elements[i], name), context);
			pkg.export(name);
		}
		for (int i = 0; i < names2.length; i++){
			String name = names2[i].intern();
			pkg.set(name, new DateElementDiff(elements[i], name), context);
			pkg.export(name);
		}
		return null;
	}
}
