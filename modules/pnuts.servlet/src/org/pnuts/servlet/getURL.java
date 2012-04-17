/*
 * @(#)getURL.java 1.2 04/12/06
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
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

/*
 * getURL( { { base, } url } )
 */
public class getURL extends PnutsFunction {

	public getURL(){
		super("getURL");
	}

	public boolean defined(int nargs){
		return (nargs == 0 || nargs == 1 || nargs == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
	
		if (nargs == 2){
			Object arg = args[0];
			URL base;
			if (arg instanceof URL){
				base = (URL)arg;
			} else {
				base = (URL)exec(new Object[]{arg}, context);
			}
			try {
				return new URL(base, (String)args[1]);
			} catch (MalformedURLException e){
				throw new PnutsException(e, context);
			}
		} else if (nargs == 1){
			Object arg = args[0];
			if (arg instanceof URL){
				return arg;
			} else if (arg instanceof File){
				try {
					return ((File)arg).toURL();
				} catch (MalformedURLException e){
					throw new PnutsException(e, context);
				}
			} else if (arg instanceof String){
				try {
					return new URL((String)arg);
				} catch (MalformedURLException e1){
					String file = (String)arg;
					HttpServletRequest request =
						(HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
					String uri = request.getRequestURI();
					int idx = uri.lastIndexOf('/');
					if (idx > 0){
						uri = uri.substring(0, idx + 1);
					} else {
						uri = "/";
					}
					try {
						URL base = new URL(request.getScheme() +
										   "://" +
										   request.getServerName() +
										   ":" +
										   request.getServerPort() +
										   uri);
						return new URL(base, file);
					} catch (MalformedURLException e2){
						throw new PnutsException(e2, context);
					}
				}
			} else {
				throw new IllegalArgumentException();
			}
		} else if (nargs == 0){
			try {
				HttpServletRequest request =
					(HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
				String uri = request.getRequestURI();
				return new URL(request.getScheme() +
							   "://" +
							   request.getServerName() +
							   ":" +
							   request.getServerPort() +
							   uri);
			} catch (MalformedURLException e){
				throw new PnutsException(e, context);
			}
		} else {
			undefined(args, context);
			return null;
		}
	}
}
