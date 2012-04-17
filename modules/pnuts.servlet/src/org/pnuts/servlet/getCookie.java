/*
 * @(#)getCookie.java 1.2 04/12/06
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
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.pnuts.net.URLEncoding;

/*
 * getCookie(name)
 */
public class getCookie extends PnutsFunction {

	public getCookie(){
		super("getCookie");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 1){
			undefined(args, context);
			return null;
		}
		String name = (String)args[0];
		Hashtable tab =
			(Hashtable)context.get(PnutsServlet.SERVLET_COOKIE);
		if (tab == null){
			HttpServletRequest request =
				(HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
			String encoding = "UTF8";
			Cookie[] cookies = request.getCookies();
			if (cookies == null){
				return null;
			}
			tab = new Hashtable();
			for (int i = 0; i < cookies.length; i++){
				try {
					tab.put(URLEncoding.decode(cookies[i].getName(),
											   encoding),
							URLEncoding.decode(cookies[i].getValue(),
											   encoding));
				} catch (UnsupportedEncodingException e){
					throw new PnutsException(e, context);
				}
			}
			context.set(PnutsServlet.SERVLET_COOKIE, tab);
		}
		return tab.get(name);
	}

	public String toString(){
		return "function getCookie(name)";
	}
}
