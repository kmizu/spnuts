/*
 * @(#)forward.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.*;
import java.net.URL;
import javax.servlet.*;
import javax.servlet.http.*;

/*
 * function forward(path)
 */
public class forward extends PnutsFunction {

	public forward(){
		super("forward");
	}

	public boolean defined(int nargs){
		return (nargs == 1);
	}

	protected Object exec(Object args[], Context context){
		int nargs = args.length;
		if (nargs == 1){
			Object arg = args[0];
			ServletRequest request =
				(ServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
			if (request == null){
				throw new IllegalStateException();
			}
			ServletResponse response =
				(ServletResponse)context.get(PnutsServlet.SERVLET_RESPONSE);
			if (response == null){
				throw new IllegalStateException();
			}
			try {
				if (arg instanceof String){
					request.getRequestDispatcher((String)arg).forward(request, response);
				} else {
					throw new IllegalArgumentException();
				} 
			} catch (Exception e){
				throw new PnutsException(e, context);
			}
			return null;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function forward(path)";
	}
}
