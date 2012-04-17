/*
 * @(#)textGrab.java 1.3 05/01/14
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import pnuts.lang.*;
import java.io.*;

/*
 * function textGrab(function)
 */
public class textGrab extends PnutsFunction {

	public textGrab(){
		super("textGrab");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object args[], Context context){
		int narg = args.length;
		if (narg != 1){
			undefined(args, context);
			return null;
		}
		final PnutsFunction func = (PnutsFunction)args[0];
		return new PnutsFunction(){
				public boolean defined(int nargs){
					return true;
				}
				protected Object exec(Object[] args, Context context){
					Context c = (Context)context.clone();
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					c.setOutputStream(bout);
					func.call(args, c);
					PrintWriter pw = c.getWriter();
					if (pw != null){
						pw.flush();
					}
					return bout.toString();
				}
			};
	}

	public String toString(){
		return "function textGrab(func)";
	}
}
