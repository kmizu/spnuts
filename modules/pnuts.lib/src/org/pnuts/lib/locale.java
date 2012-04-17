/*
 * @(#)locale.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.Locale;
import java.util.StringTokenizer;
import pnuts.lang.Context;

public class locale {

	final static String LOCALE = "pnuts$lib$locale".intern();

	static Locale toLocale(Object arg){
		if (arg instanceof Locale){
			return (Locale)arg;
		} else if (arg instanceof String){
			StringTokenizer st = new StringTokenizer((String)arg, "_");
			int n = st.countTokens();
			if (n == 1){
				return new Locale(st.nextToken());
			} else if (n == 2){
				return new Locale(st.nextToken(), st.nextToken());
			} else if (n > 2){
				return new Locale(st.nextToken(), st.nextToken(), st.nextToken());
			}
		}
		throw new IllegalArgumentException(String.valueOf(arg));
	}

	static Locale getLocale(Context context){
		Locale locale = (Locale)context.get(LOCALE);
		if (locale == null){
			context.set(LOCALE, locale = Locale.getDefault());
		}
		return locale;
	}

	static void setLocale(Locale locale, Context context){
		context.set(LOCALE, locale);
		DateTimeFormat.reset(context);
	}
}
