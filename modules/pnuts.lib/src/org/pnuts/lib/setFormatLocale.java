/*
 * @(#)setFormatLocale.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import java.util.Locale;
import java.util.StringTokenizer;

public class setFormatLocale extends PnutsFunction {

	final static String LOCALE = "pnuts$lib$locale".intern();

	public setFormatLocale(){
		super("setFormatLocale");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	static Locale getLocale(Object loc){
		if (loc instanceof Locale){
			return (Locale)loc;
		} else if (loc instanceof String){
			StringTokenizer st = new StringTokenizer((String)loc, "_");
			int n = st.countTokens();
			if (n == 1){
				return new Locale(st.nextToken());
			} else if (n == 2){
				return new Locale(st.nextToken(), st.nextToken());
			} else if (n > 2){
				return new Locale(st.nextToken(), st.nextToken(), st.nextToken());
			}
		}
		throw new IllegalArgumentException(String.valueOf(loc));
	}

	protected Object exec(Object[] args, Context context){
		if (args.length == 1){
			context.set(LOCALE, getLocale(args[0]));
		} else {
			undefined(args, context);
		}
		return null;
	}

	public String toString(){
		return "function setFormatLocale(String|Locale)";
	}
}
