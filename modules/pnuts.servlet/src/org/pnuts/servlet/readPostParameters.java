/*
 * @(#)readPostParameters.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import org.pnuts.net.URLEncoding;

/*
 * readPostParameters(request {, encoding})
 */
public class readPostParameters extends PnutsFunction {

	public readPostParameters(){
		super("readPostParameters");
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
		String contentType = request.getContentType();
		if (contentType != null &&
			contentType.startsWith("multipart/form-data"))
		{
			throw new RuntimeException("not yet implemented");
		} else {
			try {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				byte[] buf = new byte[512];
				int n;
				InputStream in = request.getInputStream();
				while ((n = in.read(buf, 0, buf.length)) != -1){
					bout.write(buf, 0, n);
				}
				in.close();
				String qs = new String(bout.toByteArray());
				Map map = URLEncoding.parseQueryString(qs, enc);
				return new ServletParameter(map);
			} catch (IOException e){
				throw new PnutsException(e, context);
			}
		}
	}
	
	public String toString(){
		return "function readPostParameters(request {, encoding })";
	}
}
