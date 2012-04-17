/*
 * @(#)readParameters.java 1.2 04/12/06
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
 * readParameters( { request { , encoding } } )
 */
public class readParameters extends PnutsFunction {

	static readParameters instance = new readParameters();

	static PnutsFunction getInstance(){
		return instance;
	}

	public readParameters(){
		super("readParameters");
	}

	static Map getParameterMap(HttpServletRequest request,
							   String encoding,
							   Context context)
		{
			String qs = request.getQueryString();
			Map map, map2;
			if (qs != null){
				try {
					map = URLEncoding.parseQueryString(qs, encoding);
				} catch (UnsupportedEncodingException e){
					throw new PnutsException(e, context);
				}
			} else {
				map = new HashMap();
			}
			String contentType = request.getContentType();
			if ("POST".equals(request.getMethod())){
				contentType = request.getContentType();
				if (contentType != null &&
					contentType.startsWith("multipart/form-data"))
				{
					Map t = (Map)context.get(PnutsServlet.SERVLET_MULTIPART_PARAM);
					map2 = new HashMap();
					for (Iterator it = t.keySet().iterator();
						 it.hasNext();)
					{
						String key = (String)it.next();
						try {
							map2.put(key,
									 new String[]{new String((byte[])t.get(key), encoding)});
						} catch (UnsupportedEncodingException e){
							throw new PnutsException(e, context);
						}
					}
				} else {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					byte[] buf = new byte[512];
					int n;
					try {
						InputStream in = request.getInputStream();
						while ((n = in.read(buf, 0, buf.length)) != -1){
							bout.write(buf, 0, n);
						}
						map2 = URLEncoding.parseQueryString(new String(bout.toByteArray()), encoding);
						in.close();
					} catch (IOException e){
						throw new PnutsException(e, context);
					}
				}
				for (Iterator it = map2.keySet().iterator();
					 it.hasNext();)
				{
					String key = (String)it.next();
					String[] v2 = (String[])map2.get(key);
					if (v2 != null){
						String[] v1 = (String[])map.get(key);
						if (v1 != null){
							String[] v3 = new String[v1.length + v2.length];
							System.arraycopy(v1, 0, v3, 0, v1.length);
							System.arraycopy(v2, 0, v3, v1.length, v2.length);
							map.put(key, v3);
						} else {
							map.put(key, v2);
						}
					}
				}
			}
			return map;
		}

	public boolean defined(int narg){
		return (narg == 0 || narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		HttpServletRequest request;
		String enc;

		if (nargs == 0){
			request = (HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
			enc = ServletEncoding.getDefaultInputEncoding(context);
		} else if (nargs == 1){
			request = (HttpServletRequest)args[0];
			enc = ServletEncoding.getDefaultInputEncoding(context);
		} else if (nargs == 2){
			request = (HttpServletRequest)args[0];
			enc = (String)args[1];
		} else {
			undefined(args, context);
			return null;
		}
		Map m = new ServletParameter(getParameterMap(request, enc, context));
		context.set(PnutsServlet.SERVLET_PARAM, m);
		return m;
	}

	public String toString(){
		return "function readParameters( { request { , encoding } } )";
	}
}
