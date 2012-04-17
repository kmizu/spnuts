/*
 * getInitParameter.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import org.pnuts.net.URLEncoding;

/*
 * 
 */
public class getInitParameter extends PnutsFunction {

	static getInitParameter instance = new getInitParameter();

	static PnutsFunction getInstance(){
		return instance;
	}

	public getInitParameter(){
		super("getInitParameter");
	}

	public boolean defined(int narg){
		return narg == 1;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			Servlet servlet = (Servlet)context.get(PnutsServlet.SYMBOL_THIS);
			ServletConfig config = servlet.getServletConfig();
			return config.getInitParameter((String)args[0]);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
	    return "function getInitParameter( key )";
	}
}
