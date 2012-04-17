/*
 * pow.java
 *
 * Copyright (c) 2001-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.math;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import java.math.*;

public class pow extends PnutsFunction {

	public pow(){
		super("pow");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		Object arg1 = args[0];
		Object arg2 = args[1];
		if ((arg2 instanceof Double) ||
			(arg2 instanceof Float) ||
			(arg2 instanceof BigDecimal) ||
			(arg2 instanceof Number && ((Number)arg2).intValue() < 0))
		{
			return new Double(Math.pow(((Number)arg1).doubleValue(), ((Number)arg2).doubleValue()));
		} else if ((arg2 instanceof Integer) || (arg2 instanceof Short) || (arg2 instanceof Byte)){
		    if (arg1 instanceof Double){
			return new Double(ipow_d(((Double)arg1).doubleValue(), ((Number)arg2).intValue()));
		    } else if (arg1 instanceof Float){
			return new Float(ipow_f(((Float)arg1).floatValue(), ((Number)arg2).intValue()));
		    } else {
			return ipow(arg1, ((Number)arg2).intValue());
		    }
		} else {
			return npow(arg1, arg2);
		}
	}

	static final Integer zero = new Integer(0);
	static final Integer one = new Integer(1);
	static final Integer two = new Integer(2);

	static Object npow(Object n, Object m){
		if (Runtime.eq(m, zero)){
			return one;
		} else {
			Object m2 = Runtime.divide(m, two);
			Object mod = Runtime.mod(m, two);
			Object t = npow(n, m2);
			Object r = Runtime.multiply(t, t);
			if (Runtime.gt(mod, zero)){
				return Runtime.multiply(r, n);
			} else {
				return r;
			}
		}
	}

	static Object ipow(Object n, int m){
		if (m == 0){
			return one;
		} else {
			int m2 = m / 2;
			int mod = m % 2;
			Object t = ipow(n, m2);
			Object r = Runtime.multiply(t, t);
			if (mod > 0){
				return Runtime.multiply(r, n);
			} else {
				return r;
			}
		}
	}

	static double ipow_d(double d, int m){
		if (m == 0){
			return 1;
		} else {
		    int m2 = m / 2;
		    int mod = m % 2;
		    double t = ipow_d(d, m2);
		    double r = t * t;
		    if (mod > 0){
			return r * d;
		    } else {
			return r;
		    }
		}
	}

	static float ipow_f(float d, int m){
		if (m == 0){
			return 1;
		} else {
		    int m2 = m / 2;
		    int mod = m % 2;
		    float t = ipow_f(d, m2);
		    float r = t * t;
		    if (mod > 0){
			return r * d;
		    } else {
			return r;
		    }
		}
	}

	public String toString(){
		return "function pow(x, y)";
	}
}
