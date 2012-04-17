/*
 * @(#)DateElementDiff.java 1.3 05/05/07
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

class DateElementDiff extends PnutsFunction {
	private int element;

	DateElementDiff(int element, String name){
		super(name);
		this.element = element;
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	static int diffYear(Calendar c0, Calendar c1){
		return c1.get(Calendar.YEAR) - c0.get(Calendar.YEAR);
	}

	static int diffMonth(Calendar c0, Calendar c1){
		return diffYear(c1, c0) * 12 + c1.get(Calendar.MONTH) - c0.get(Calendar.MONTH);
	}

	static int diffHour(Calendar c0, Calendar c1){
		return diffDay(c0, c1) * 24 + c1.get(Calendar.HOUR_OF_DAY) - c0.get(Calendar.HOUR_OF_DAY);
	}

	static int diffMinute(Calendar c0, Calendar c1){
		return diffHour(c0, c1) * 60 + c1.get(Calendar.MINUTE) - c0.get(Calendar.MINUTE);
	}

	static long diffSecond(Calendar c0, Calendar c1){
		return diffMinute(c0, c1) * 60L + c1.get(Calendar.SECOND) - c0.get(Calendar.SECOND);
	}	

	static long diffMilliSecond(Calendar c0, Calendar c1){
		return diffSecond(c0, c1) * 1000L + c1.get(Calendar.MILLISECOND) - c0.get(Calendar.MILLISECOND);
	}	

	static int diffDay(Calendar c0, Calendar c1){
		int year0, year1;
		int day0, day1;
		year0 = c0.get(Calendar.YEAR);
		day0 = c0.get(Calendar.DAY_OF_YEAR);
		year1 = c1.get(Calendar.YEAR);
		day1 = c1.get(Calendar.DAY_OF_YEAR);

		if (year0 == year1) {
			return day1 - day0;
		} else {
			boolean negative;
			if (year0 > year1){
				int tmp = year0;
				year0 = year1;
				year1 = tmp;
				tmp = day0;
				day0 = day1;
				day1 = tmp;
				negative = true;
			} else {
				negative = false;
			}
			int days;
			Calendar c = (Calendar)c0.clone();
			c.set(year0, 11, 31);
			days = c.get(Calendar.DAY_OF_YEAR) - day0;
			for (int i = year0 + 1; i < year1; i++){
				c.set(i, 11, 31);
				days += c.get(Calendar.DAY_OF_YEAR);
			}
			days += day1;
			if (negative) {
				return -days;
			} else {
				return days;
			}
		}
	}

	static int diffWeekOfYear(Calendar c0, Calendar c1){
		int days = diffDay(c0, c1);
		int w, firstDay;
		if (days < 0){
			w = c1.get(Calendar.DAY_OF_WEEK);
			firstDay = c0.getFirstDayOfWeek();
		} else {
			w = c0.get(Calendar.DAY_OF_WEEK);
			firstDay = c0.getFirstDayOfWeek();
		}
		return (days - (7 - (w - firstDay) % 7)) / 7 + 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		Calendar c0 = date.getCalendar((Date)args[0], context);
		Calendar c1 = date.getCalendar((Date)args[1], context);

		switch (element){
		case Calendar.YEAR:
			return new Integer(diffYear(c0, c1));
		case Calendar.MONTH:
			return new Integer(diffMonth(c0, c1));
		case Calendar.WEEK_OF_YEAR:
			return new Integer(diffWeekOfYear(c0, c1));
		case Calendar.DAY_OF_YEAR:
			return new Integer(diffDay(c0, c1));
		case Calendar.HOUR_OF_DAY:
			return new Integer(diffHour(c0, c1));
		case Calendar.MINUTE:
			return new Integer(diffMinute(c0, c1));
		case Calendar.SECOND: {
			long l = diffSecond(c0, c1);
			if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE){
				return new Long(l);
			} else {
				return new Integer((int)l);
			}
		}
		case Calendar.MILLISECOND: {
			long l = diffMilliSecond(c0, c1);
			if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE){
				return new Long(l);
			} else {
				return new Integer((int)l);
			}
		}
		default:
			throw new IllegalArgumentException(String.valueOf(element));
		}
	}

	public String toString(){
		return "function " + name + "(Date, Date)";
	}
}
