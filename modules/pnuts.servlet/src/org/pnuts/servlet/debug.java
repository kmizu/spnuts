/*
 * @(#)debug.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.Pnuts;
import pnuts.lang.Context;
import pnuts.lang.Executable;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import pnuts.tools.VisualDebugger;
import java.io.FileNotFoundException;

/*
 * debug(script)
 */
public class debug extends PnutsFunction {

	public debug(){
		super("debug");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		String script;
		if (nargs == 1){
			Context c = new VisualDebugger().createContext(context);
			c.set(PnutsServlet.SERVLET_COMPILER, null);
			Object arg0 = args[0];
			if (arg0 instanceof String){
				try {
					return Pnuts.load((String)arg0, c);
				} catch (FileNotFoundException e){
					throw new PnutsException(e, context);
				}
			} else if (arg0 instanceof Executable){
				return ((Executable)arg0).run(c);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function debug((String|Executable) script)";
	}
}
