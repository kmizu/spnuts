/*
 * @(#)sendRedirect.java 1.2 04/12/06
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
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

/*
 * function sendRedirect(url)
 */
public class sendRedirect extends PnutsFunction {

	public sendRedirect(){
		super("sendRedirect");
	}

	public boolean defined(int nargs){
		return (nargs == 1);
	}

	protected Object exec(Object args[], Context context){
		int nargs = args.length;
		if (nargs == 1){
			Object arg = args[0];
			HttpServletResponse response =
				(HttpServletResponse)context.get(PnutsServlet.SERVLET_RESPONSE);
			if (response == null){
				throw new IllegalStateException();
			}
			try {
				if (arg instanceof String){
					response.sendRedirect((String)arg);
				} else if (arg instanceof URL){
					response.sendRedirect(String.valueOf(arg));
				} else {
					throw new IllegalArgumentException(String.valueOf(arg));
				} 
			} catch (IOException e){
				throw new PnutsException(e, context);
			} catch (IllegalStateException e){
				PnutsException pe = new PnutsException("Headers have already been sent.", context);
				pe.initCause(e);
				throw pe;
			}
			return null;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function sendRedirect(url)";
	}
}
