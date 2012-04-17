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

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		} else {
			Object n = args[0];
			String s;
			boolean minus;
			if (n instanceof byte[]){
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
			} else if (n instanceof BigInteger){
				BigInteger bint = (BigInteger)n;
				minus = (bint.signum() < 0);
				s = bint.toString(16);
//		} else if (n instanceof BigDecimal){
//		BigDecimal bdec = (BigDecimal)n;
//		minus = (bdec.signum() < 0);
//		s = bdec.toBigInteger().toString(16);
			} else if (n instanceof Long){
				long val = ((Long)n).longValue();
				minus = val < 0;
				s = Long.toString(val, 16);
			} else {
				int val = ((Integer)n).intValue();
				minus = val < 0;
				s = Integer.toString(val, 16);
			}
			return s;
		}
	}
}
