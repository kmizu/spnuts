/*
 * @(#)hex.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.math.*;

public class hex extends PnutsFunction {

	static char[] hex_chars = {'0','1','2','3','4','5','6','7',
							   '8','9','a','b','c','d','e','f'};

	public hex(){
		super("hex");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	public static String hex(int val){
		char[] buf = new char[32];
		int pos = 32;
		do {
			buf[--pos] = hex_chars[val & ((1 << 4) - 1)];
			val >>>= 4;
		} while (val != 0);
		return new String(buf, pos, 32 - pos);
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		} else {
			Object n = args[0];
			String s;
			if (n instanceof Integer){
				s = hex(((Integer)n).intValue());
			} else if (n instanceof byte[]){
				byte[] b = (byte[])n;
				int len = b.length;
				char[] result = new char[len * 2];
				for (int i = 0, j = 0; i < len; i++, j += 2){
					byte x = b[i];
					int q = ((x + 256) % 256);
					result[j] = hex_chars[q >> 4];
					result[j + 1] = hex_chars[q & 0x0f];
				}
				return new String(result);
			} else if (n instanceof Long){
				long val = ((Long)n).longValue();
				s = Long.toHexString(val);
			} else if (n instanceof Character){
				s = hex((int)((Character)n).charValue());
			} else {
				throw new IllegalArgumentException();
			}
			return s;
		}
	}

	public String toString(){
		return "function hex(Integer|Long|Character|byte[])";
	}
}
