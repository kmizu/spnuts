/*
 * @(#)formatNumber.java 1.2 04/12/06
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

public class formatNumber extends PnutsFunction {

	public formatNumber(){
		super("formatNumber");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2 || nargs == 5;
	}
	
	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			return NumberFormatHelper.formatNumber((Number)args[0], -1, -1, -1, -1, context);
		} else if (nargs == 2){
			Number num = (Number)args[0];
			Object arg1 = args[1];
			if (arg1 instanceof String){
				return NumberFormatHelper.formatDecimal(num, (String)arg1);
			} else if (arg1 instanceof Integer){
				return NumberFormatHelper.formatNumber(num, -1, -1, ((Integer)arg1).intValue(), -1, context);
			} else {
				throw new IllegalArgumentException();
			}
		} else if (nargs == 5){
			Number num = (Number)args[0];
			int min = ((Number)args[1]).intValue();
			int max = ((Number)args[2]).intValue();
			int fmin = ((Number)args[3]).intValue();
			int fmax = ((Number)args[4]).intValue();
			return NumberFormatHelper.formatNumber(num, min, max, fmin, fmax, context);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function formatNumber(num),(num,{format|min}),(num,min,max,fmin,fmax)";
	}
}
