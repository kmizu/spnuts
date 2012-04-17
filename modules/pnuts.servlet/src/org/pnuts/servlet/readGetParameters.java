/*
 * @(#)readGetParameters.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import org.pnuts.net.URLEncoding;

/*
 * readGetParameters(request {, encoding})
 */
public class readGetParameters extends PnutsFunction {

	public readGetParameters(){
		super("readGetParameters");
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
		HttpServletRequest request = (HttpServletRequest)args[0];
		String enc;
		if (nargs == 1){
			enc = ServletEncoding.getDefaultInputEncoding(context);
		} else {
			enc = (String)args[1];
		}
		String qs = request.getQueryString();
		Map map;
		try {
			if (qs != null){
				map = URLEncoding.parseQueryString(qs, enc);
			} else {
				map = new Hashtable();
			}
			return new ServletParameter(map);
		} catch (UnsupportedEncodingException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function readGetParameters(request {, encoding})";
	}
}
