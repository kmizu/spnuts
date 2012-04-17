/*
 * @(#)reverse.java 1.3 05/01/14
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;
import java.lang.reflect.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;

/*
 * function reverse(arrayOrList)
 */
public class reverse extends PnutsFunction {

	public reverse(){
		super("reverse");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 1){
			undefined(args, context);
			return null;
		}
		Object arg = args[0];
		if (arg instanceof Object[]){
			Object[] a = (Object[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				Object t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof int[]){
			int[] a = (int[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				int t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof byte[]){
			byte[] a = (byte[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				byte t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof char[]){
			char[] a = (char[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				char t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof short[]){
			short[] a = (short[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				short t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof float[]){
			float[] a = (float[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				float t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof double[]){
			double[] a = (double[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				double t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof long[]){
			long[] a = (long[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				long t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof boolean[]){
			boolean[] a = (boolean[])arg;
			int len = a.length;
			for (int i = 0; i < len / 2; i++){
				boolean t = a[i];
				int j = len - i - 1;
				a[i] = a[j];
				a[j] = t;
			}
		} else if (arg instanceof List){
			Collections.reverse((List)arg);
		} else if (arg instanceof Map){
			return reverseMap((Map)arg);
		} else if (arg instanceof String){
		    String str = (String)arg;
		    int len = str.length();
		    StringBuffer sbuf = new StringBuffer();
		    for (int i = 0; i < len; i++){
			sbuf.append(str.charAt(len - i - 1));
		    }
		    return sbuf.toString();
		} else {
			throw new IllegalArgumentException();
		}
		return arg;
	}

	static Map reverseMap(Map map){
		Map output = new HashMap();
		reverseMap(map, output);
		return output;
	}

	static void reverseMap(Map map, Map output){
		for (Iterator it = map.entrySet().iterator(); it.hasNext(); ){
			Map.Entry entry = (Map.Entry)it.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			List lst = (List)output.get(value);
			if (lst == null){
				lst = new ArrayList();
				output.put(value, lst);
			}
			lst.add(key);
		}
	}

	public String toString(){
		return "function reverse(arrayOrListOrMap)";
	}
}
