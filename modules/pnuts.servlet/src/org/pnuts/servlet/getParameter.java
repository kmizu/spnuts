/*
 * @(#)getParameter.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;

/*
 * getParameter(name {, encoding})
 */
public class getParameter extends PnutsFunction {

	public getParameter(){
		super("getParameter");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 1 && nargs != 2){
			undefined(args, context);
			return null;
		}
		String param = (String)args[0];
		Map map = (Map)context.get(PnutsServlet.SERVLET_PARAM);
		if (map == null){
			HttpServletRequest request =
				(HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
			String enc;
			if (nargs == 1){
				enc = ServletEncoding.getDefaultInputEncoding(context);
			} else {
				enc = (String)args[1];
			}
			map = (Map)readParameters.getInstance().call(new Object[]{request, enc}, context);
		}
		return map.get(param);
	}

	public String toString(){
		return "function getParameter(name {, encoding})";
	}
}
