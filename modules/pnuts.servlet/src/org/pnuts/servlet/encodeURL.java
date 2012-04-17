/*
 * @(#)encodeURL.java 1.2 04/12/06
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
 * encodeURL(str {, encoding})
 */
public class encodeURL extends PnutsFunction {

	public encodeURL(){
		super("encodeURL");
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
			encoding = ServletEncoding.getDefaultOutputEncoding(context);
		} else if (nargs == 2){
			str = (String)args[0];
			encoding = (String)args[1];
		} else {
			undefined(args, context);
			return null;
		}
		try {
			if (str == null){
				return "";
			} else {
				return URLEncoding.encode(str, encoding);
			}
		} catch (UnsupportedEncodingException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function encodeURL(str {, encoding})";
	}
}
