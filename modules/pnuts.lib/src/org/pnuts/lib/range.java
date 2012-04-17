/*
 * @(#)range.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Generator;

public class range extends PnutsFunction {
	public range(){
		super("range");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, final Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		int i0, i1;
		Object arg0 = args[0];
		if (arg0 instanceof Number){
			i0 = ((Number)arg0).intValue();
		} else {
			throw new IllegalArgumentException(String.valueOf(arg0));
		}
		Object arg1 = args[1];
		if (arg1 instanceof Number){
			i1 = ((Number)arg1).intValue();
		} else {
			throw new IllegalArgumentException(String.valueOf(arg1));
		}
		class G extends Generator {
			int start;
			int end;

			G(int start, int end){
				this.start = start;
				this.end = end;
			}

			public Object apply(PnutsFunction closure, Context ctx){
				int s = this.start;
				int e = this.end;
				Object[] args = new Object[1];
				if (s > e){
					for (int i = s; i >= e; i--){
						args[0] = new Integer(i);
						closure.call(args, context);
					}
				} else {
					for (int i = s; i <= e; i++){
						args[0] = new Integer(i);
						closure.call(args, context);
					}
				}
				return null;
			}
		}
		return new G(i0, i1);
	}

	public String toString(){
		return "function range(start, end)";
	}
}
