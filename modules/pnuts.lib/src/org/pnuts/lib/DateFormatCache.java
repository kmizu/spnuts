/*
 * @(#)DateFormatCache.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

class DateFormatCache extends ResourceCache {
	private final static boolean DEBUG = false;

	Context context;

	DateFormatCache(Context context){
		this.context = context;
	}

	protected Object createResource(Object pattern){
		if (DEBUG){
			System.out.println("createResorce " + pattern);
		}
		DateFormat df = new SimpleDateFormat((String)pattern, locale.getLocale(context));
		df.setTimeZone(date.getTimeZone(context));
		return df;
	}

	DateFormat get(String pattern){
		return (DateFormat)super.getResource(pattern);
	}
}
