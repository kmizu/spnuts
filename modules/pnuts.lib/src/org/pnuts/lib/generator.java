/*
 * @(#)generator.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.util.Enumeration;

public class generator extends PnutsFunction {

	public generator(){
		super("generator");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object[] args, final Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		} else {
			Object a0 = args[0];
			if (a0 instanceof Generator){
				return a0;
			} else {
				final Enumeration en = Runtime.toEnumeration(a0, context);
				if (en != null){
					return new Generator(){
							public Object apply(PnutsFunction closure, Context c){
								Object[] a = new Object[1];
								while (en.hasMoreElements()){
									a[0] = en.nextElement();
									closure.call(a, context);
								}
								return null;
							}
						};
				} else {
					throw new IllegalArgumentException(String.valueOf(a0));
				}
			}
		}
	}

	public String toString(){
		return "function generator(elements)";
	}
}
