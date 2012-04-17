/*
 * @(#)DateTimeFormat.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import pnuts.lang.Context;

class DateTimeFormat {
	final static String DATEFORMAT_CACHE = "pnuts$lib$dateformat_cache".intern();
	final static String DATETIME_FORMAT = "pnuts$lib$datetime_format".intern();
	final static String DATE_FORMAT = "pnuts$lib$date_format".intern();
	final static String TIME_FORMAT = "pnuts$lib$time_format".intern();
	final static String DEFAULT = "DEFAULT";
	final static String defaultDatePattern = "MM/dd/yyyy";

	static void reset(Context context){
		context.set(DATEFORMAT_CACHE, null);
		context.set(DATETIME_FORMAT, null);
		context.set(DATE_FORMAT, null);
		context.set(TIME_FORMAT, null);
	}

	static DateFormat getDateFormat(String pattern, Context context){
		DateFormatCache cache;
		synchronized (context){
			cache = (DateFormatCache)context.get(DATEFORMAT_CACHE);
			if (cache == null){
				cache = new DateFormatCache(context);
				context.set(DATEFORMAT_CACHE, cache);
			}
		}
		return cache.get(pattern);
	}

	static Date parse(String expr, Context context){
		return parse(expr, defaultDatePattern, context);
	}

	static Date parse(String expr, String pattern, Context context){
		try {
			return getDateFormat(pattern, context).parse(expr);
		} catch (Exception e){
			return null;
		}
	}

	static DateFormat getDateTimeFormat(String type, Context context){
		synchronized (context){
			DateFormat df = (DateFormat)context.get(type);
			if (df == null){
				df = DateFormat.getDateTimeInstance();
				df.setTimeZone(date.getTimeZone(context));
				context.set(type, df);
			}
			return df;
		}
	}

	static String formatDateTime(Date date, Context context){
		return getDateTimeFormat(DATETIME_FORMAT, context).format(date);
	}

	static String formatDateTime(Date date, String pattern, Context context){
		return getDateFormat(pattern, context).format(date);
	}

	static String formatDateTime(Date d, String style1, String style2, Context context){
		DateFormat dateFormat =
			DateFormat.getDateTimeInstance(dateStyle(style1),
										   dateStyle(style2),
										   locale.getLocale(context));
		dateFormat.setTimeZone(date.getTimeZone(context));
		return dateFormat.format(d);
	}

	static String formatDate(Date date, Context context){
		return getDateTimeFormat(DATE_FORMAT, context).format(date);
	}

	static String formatDate(Date d, String style, Context context){
		DateFormat dateFormat =
			DateFormat.getDateInstance(dateStyle(style), locale.getLocale(context));
		dateFormat.setTimeZone(date.getTimeZone(context));
		return dateFormat.format(d);
	}

	static String formatTime(Date date, Context context){
		return getDateTimeFormat(TIME_FORMAT, context).format(date);
	}

	static String formatTime(Date d, String style, Context context){
		DateFormat dateFormat =
			DateFormat.getTimeInstance(dateStyle(style), locale.getLocale(context));
		dateFormat.setTimeZone(date.getTimeZone(context));
		return dateFormat.format(d);
	}

	private static int dateStyle(String s){
		s = s.toLowerCase();
	
		if ("full".equals(s)){
			return DateFormat.FULL;
		}
		if ("long".equals(s)){
			return DateFormat.LONG;
		}
		if ("medium".equals(s)){
			return DateFormat.MEDIUM;
		}
		if ("short".equals(s)){
			return DateFormat.SHORT;
		}
		return DateFormat.DEFAULT;
	}
}
