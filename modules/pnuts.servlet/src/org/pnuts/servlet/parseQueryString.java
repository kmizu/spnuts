/*
 * @(#)parseQueryString.java 1.2 04/12/06
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
import org.pnuts.net.URLEncoding;

/*
 * parseQueryString(str {, encoding})
 */
public class parseQueryString extends PnutsFunction {

	public parseQueryString(){
		super("parseQueryString");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		String str;
		String encoding;
		if (nargs == 1){
			str = (String)args[0];
			encoding = ServletEncoding.getDefaultInputEncoding(context);
		} else if (nargs == 2){
			str = (String)args[0];
			encoding = (String)args[1];
		} else {
			undefined(args, context);
			return null;
		}
		try {
			return new ServletParameter(URLEncoding.parseQueryString(str, encoding));
		} catch (UnsupportedEncodingException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function parseQueryString(str {, encoding })";
	}
}
