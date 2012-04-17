/*
 * @(#)ServletEncoding.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import pnuts.servlet.*;
import pnuts.lang.Context;

class ServletEncoding {

	/**
	 * Gets the default encoding for input.
	 * This function returns request.getCharacterEncoding() value if it is non-null,
	 * otherwise response.getCharacterEncoding() value, if it is non-null. 
	 * If both are null, it returns "UTF8".
	 *
	 * @param context the context
	 * @return the encoding name
	 */
	public static String getDefaultInputEncoding(Context context){
		HttpServletRequest request =
			(HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
		if (request == null){
			throw new IllegalStateException();
		}
		String encoding = request.getCharacterEncoding();
		if (encoding == null){
			ServletResponse response =
				(ServletResponse)context.get(PnutsServlet.SERVLET_RESPONSE);
			encoding = response.getCharacterEncoding();
		}
		if (encoding == null){
			encoding = "UTF-8";
		}
		return encoding;
	}

	/**
	 * Gets the default encoding for output.
	 * This function returns response.getCharacterEncoding() if it is non-null, otherwise "UTF8".
	 *
	 * @param context the context
	 * @return the encoding name
	 */
	public static String getDefaultOutputEncoding(Context context){
		ServletResponse response =
			(ServletResponse)context.get(PnutsServlet.SERVLET_RESPONSE);
		if (response == null){
			throw new IllegalStateException();
		}
		String encoding = response.getCharacterEncoding();
		if (encoding == null){
			encoding = "UTF-8";
		}
		return encoding;
	}
}
