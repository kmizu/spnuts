/*
 * requestPath.java
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
import pnuts.lang.Package;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

/*
 * function requestPath()
 */
public class requestPath extends PnutsFunction {
	public requestPath(){
		super("requestPath");
	}

	public boolean defined(int nargs){
		return (nargs == 0);
	}

	protected Object exec(Object args[], Context context){
		if (args.length != 0){
			undefined(args, context);
			return null;
		}
		HttpServletRequest request = (HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
//		String pathInfo = request.getPathInfo();
		String uri = request.getRequestURI().substring(request.getContextPath().length());
		ArrayList requestPaths = new ArrayList();
		if (uri != null){
		    String s = uri;
		    if (s.startsWith("/")){
			s = s.substring(1);
		    }
		    if (s.length() > 0){
			String[] tokens = s.split("/");
			for (int i = 0; i < tokens.length; i++){
			    requestPaths.add(tokens[i]);
			}
		    }
		}
		return requestPaths;
	}

	public String toString(){
		return "function requestPath()";
	}
}
