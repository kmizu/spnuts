/*
 * @(#)addCookie.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.pnuts.net.URLEncoding;

/*
 * addCookie(name, value {, maxAge })
 */
public class addCookie extends PnutsFunction {

	public addCookie(){
		super("addCookie");
	}

	public boolean defined(int narg){
		return (narg == 2 || narg == 3);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		HttpServletResponse response =
			(HttpServletResponse)context.get(PnutsServlet.SERVLET_RESPONSE);
		try {
			if (nargs == 2 || nargs == 3){
				String name = (String)args[0];
				String value = (String)args[1];
				String encoding = "UTF8";
				Cookie cookie = new Cookie(URLEncoding.encode(name, encoding),
										   URLEncoding.encode(value, encoding));
				if (nargs == 3){
					int maxAge = ((Integer)args[2]).intValue();
					cookie.setMaxAge(maxAge);
				}
				response.addCookie(cookie);
			} else {
				undefined(args, context);
				return null;
			}
			return null;
		} catch (UnsupportedEncodingException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function addCookie(name, value {, maxAge })";
	}
}
