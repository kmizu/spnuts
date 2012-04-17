/*
 * @(#)NumberFormatHelper.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.text.*;
import java.util.*;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

class NumberFormatHelper {

	private final static String FORMAT_LOCALE = "pnuts$lib$locale".intern();

	public static String formatDecimal(Number num, String fmt){
		DecimalFormat formatter = new DecimalFormat(fmt);
		if (num instanceof Double){
			return formatter.format(((Double)num).doubleValue());
		} else if (num instanceof Float){
			return formatter.format(((Float)num).doubleValue());
		} else if (num instanceof Long){
			return formatter.format(((Long)num).longValue());
		} else if (num instanceof Integer){
			return formatter.format(((Integer)num).longValue());
		} else if (num instanceof Short){
			return formatter.format(((Short)num).longValue());
		} else if (num instanceof Byte){
			return formatter.format(((Byte)num).longValue());
		} else if (num instanceof Number){
			return formatter.format(((Number)num).doubleValue());
		} else {
			throw new IllegalArgumentException();
		}
	}

	public static String formatNumber(Number num, int min, int max, int fmin, int fmax, Context context){
		NumberFormat fmt = NumberFormat.getNumberInstance(getFormatLocale(context));
		return doFormat(fmt, num, min, max, fmin, fmax);
	}

	public static String formatCurrency(Number num, int min, int max, int fmin, int fmax, Context context){
		NumberFormat fmt = NumberFormat.getCurrencyInstance(getFormatLocale(context));
		return doFormat(fmt, num, min, max, fmin, fmax);
	}

	public static String formatPercent(Number num, int min, int max, int fmin, int fmax, Context context){
		NumberFormat fmt = NumberFormat.getPercentInstance(getFormatLocale(context));
		return doFormat(fmt, num, min, max, fmin, fmax);
	}
	
	static String doFormat(NumberFormat fmt, Number num, int min, int max, int fmin, int fmax){
		setScale(fmt, min, max, fmin, fmax);
		return fmt.format(num);
	}
	
	static void setScale(NumberFormat fmt, int imin, int imax, int fmin, int fmax){
		if (imin >= 0){
			fmt.setMinimumIntegerDigits(imin);
		}
		if (imax >= 0){
			fmt.setMaximumIntegerDigits(imax);
		}
		if (fmin >= 0){
			fmt.setMinimumFractionDigits(fmin);
		}
		if (fmax >= 0){
			fmt.setMaximumFractionDigits(fmax);
		}
	}

	static Locale getFormatLocale(Context context){
		Locale lc = (Locale)context.get(FORMAT_LOCALE);
		if (lc == null){
			lc = locale.getLocale(context);
		}
		return lc;
	}
}
