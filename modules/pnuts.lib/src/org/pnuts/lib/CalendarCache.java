/*
 * @(#)CalendarCache.java 1.2 04/12/06
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

class CalendarCache extends ResourceCache {
	private final static boolean DEBUG = false;

	Context context;

	CalendarCache(Context context){
		this.context = context;
	}

	protected Object createResource(Object key){
		if (DEBUG){
			System.out.println("createResource " + key);
		}
		Calendar c = Calendar.getInstance(date.getTimeZone(context),
										  locale.getLocale(context));
		c.setTime((Date)key);
		return c;
	}

	Calendar get(Date date){
		return (Calendar)super.getResource(date);
	}
}
