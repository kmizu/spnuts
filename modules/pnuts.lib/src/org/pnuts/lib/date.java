/*
 * @(#)date.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;

public class date extends PnutsFunction {

	final static String TIMEZONE = "pnuts$lib$timezone".intern();
	final static String CALENDAR = "pnuts$lib$calendar".intern();
	final static String CALENDAR_POOL = "pnuts$lib$calendar_pool".intern();

	public date(){
		super("date");
	}

	public boolean defined(int nargs){
		return (nargs < 4 || nargs == 6);
	}

	static TimeZone getTimeZone(Context context){
		TimeZone zone = (TimeZone)context.get(TIMEZONE);
		if (zone == null){
			context.set(TIMEZONE, zone = TimeZone.getDefault());
		}
		return zone;
	}

	static void setTimeZone(Context context, TimeZone zone){
		CalendarCache cc = (CalendarCache)context.get(CALENDAR_POOL);
		if (cc != null){
			cc.reset();
		}
		context.set(TIMEZONE, zone);
		context.set(CALENDAR, null);
		DateTimeFormat.reset(context);
	}

	static Calendar getCalendar(Context context){
		Calendar c;
		synchronized (context){
			c = (Calendar)context.get(CALENDAR);
			if (c == null){
				c = Calendar.getInstance(getTimeZone(context));
				context.set(CALENDAR, c);
			}
			return c;
		}
	}

	static Calendar getCalendar(Date date, Context context){
		CalendarCache cc;
		synchronized (context){
			cc = (CalendarCache)context.get(CALENDAR_POOL);
			if (cc == null){
				context.set(CALENDAR_POOL, cc = new CalendarCache(context));
			}
		}
		return cc.get(date);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		switch (nargs){
		case 0:
			return new Date();
		case 1: {
			Object arg = args[0];
			if (arg instanceof Date){
				return arg;
			} else if (arg instanceof Number){
				return new Date(((Number)arg).longValue());
			} else if (arg instanceof String){
				return DateTimeFormat.parse((String)arg, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg));
			}
		}
		case 2: {
			Object arg0 = args[0];
			Object arg1 = args[1];
			if (arg0 instanceof String && arg1 instanceof String){
				return DateTimeFormat.parse((String)arg0, (String)arg1, context);
			} else {
				throw new IllegalArgumentException(arg0 + ", " + arg1);
			}
		}
		case 3: {
			int year = ((Number)args[0]).intValue();
			int month = ((Number)args[1]).intValue();
			int day = ((Number)args[2]).intValue();
			Date d = new Date(0L);
			Calendar c = getCalendar(d, context);
			c.set(year, month - 1, day, 0, 0, 0);
			c.set(Calendar.MILLISECOND, 0);
			d.setTime(c.getTimeInMillis());
			return d;
		}
		case 6: {
			int year = ((Number)args[0]).intValue();
			int month = ((Number)args[1]).intValue();
			int day = ((Number)args[2]).intValue();
			int hour = ((Number)args[3]).intValue();
			int minute = ((Number)args[4]).intValue();
			int second = ((Number)args[5]).intValue();
			Date d = new Date(0L);
			Calendar c = getCalendar(d, context);
			c.set(year, month - 1, day, hour, minute, second);
			c.set(Calendar.MILLISECOND, 0);
			d.setTime(c.getTimeInMillis());
			return d;
		}
		default:
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function date(),(expr),(expr,pattern),(year,month,day),(year,month,day,hour,minute,second)";
	}
}
