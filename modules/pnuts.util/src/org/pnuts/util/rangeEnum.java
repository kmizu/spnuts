/*
 * @(#)rangeEnum.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import pnuts.lang.*;
import java.util.Enumeration;

/*
 * function rangeEnum(start, end {, step})
 */
public class rangeEnum extends PnutsFunction {

	public rangeEnum(){
		super("rangeEnum");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 2 || nargs == 3){
			final int start = ((Integer)args[0]).intValue();
			final int end = ((Integer)args[1]).intValue();
			int step;
			if (nargs == 3){
				step = ((Integer)args[2]).intValue();
			} else {
				if (start > end){
					step = -1;
				} else {
					step = 1;
				}
			}
			final int fstep = step;
			class Enum implements Enumeration {
				int pos = start;
				boolean increase = (fstep > 0);
				public boolean hasMoreElements(){
					if (increase){
						return pos <= end;
					} else {
						return pos >= end;
					}
				}

				public Object nextElement(){
					Object ret = new Integer(pos);
					pos += fstep;
					return ret;
				}
			}
			return new Enum();
		
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function rangeEnum(start, end {, step})";
	}
}
