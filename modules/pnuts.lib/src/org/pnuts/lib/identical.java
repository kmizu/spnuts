/*
 * @(#)identical.java 1.2 04/12/06
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

public class identical extends PnutsFunction {

	public identical(){
		super("identical");
	}

	public boolean defined(int narg){
		return (narg == 2);
	}

	protected Object exec(Object[] args, final Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		} else {
			if (args[0] == args[1]){
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		}
	}

	public String toString(){
		return "function identical(object1, object2)";
	}
}
