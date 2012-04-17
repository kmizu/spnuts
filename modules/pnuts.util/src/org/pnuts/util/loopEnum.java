/*
 * @(#)loopEnum.java 1.2 04/12/06
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
 * function loopEnum(n)
 */
public class loopEnum extends PnutsFunction {

	public loopEnum(){
		super("loopEnum");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			final int max = ((Integer)args[0]).intValue();

			class Enum implements Enumeration {
				int n = max;

				public boolean hasMoreElements(){
					return n > 0;
				}

				public Object nextElement(){
					n--;
					return null;
				}
			}
			return new Enum();
		
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function loopEnum(n)";
	}
}
