/*
 * @(#)readMultipartRequest.java 1.2 04/12/06
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

/*
 * readMultipartRequest( { request, } handler(mime, name, filename, ctype))
 */
public class readMultipartRequest extends PnutsFunction {

	public readMultipartRequest(){
		super("readMultipartRequest");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		ServletRequest request;
		PnutsFunction handler;
		if (nargs == 1){
			request = (ServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
			handler = (PnutsFunction)args[0];
		} else if (nargs == 2){
			request = (ServletRequest)args[0];
			handler = (PnutsFunction)args[1];
		} else {
			undefined(args, context);
			return null;
		}
		String type = request.getContentType();
		String name = null;
		int idx = type.lastIndexOf("boundary=");
		if (idx < 0) {
			return null;
		}
		byte[] boundary = type.substring(idx + 9).getBytes();

		try {
			MultipartInputStream multi =
				new MultipartInputStream(request.getInputStream(), boundary);
			while (multi.next()){
				MimeInputStream mime = new MimeInputStream(multi);
				String cdisp = mime.getHeader("content-disposition");
				String ctype = mime.getHeader("content-type");

				String filename = null;
				StringTokenizer st = new StringTokenizer(cdisp, ";");
				while (st.hasMoreTokens()){
					String token = st.nextToken();
					idx = token.indexOf('=');
					if (idx > 0){
						String key = token.substring(0, idx).trim();
						if ("filename".equals(key)){
							String value = token.substring(idx + 1).trim();
							filename = value;
							int len = value.length();
							if (len > 1 && value.charAt(0) == '"' && value.charAt(len - 1) == '"'){
								filename = value.substring(1, len - 1);
							}
						} else if ("name".equals(key)){
							String value = token.substring(idx + 1).trim();
							name = value;
							int len = value.length();
							if (len > 1 && value.charAt(0) == '"' && value.charAt(len - 1) == '"'){
								name = value.substring(1, len - 1);
							}
						}
					}
				}
				if (filename != null){
					handler.call(new Object[]{mime, name, filename, ctype}, context);
				} else {
					Hashtable param = (Hashtable)context.get(PnutsServlet.SERVLET_MULTIPART_PARAM);
					if (param == null){
						param = new Hashtable();
						context.set(PnutsServlet.SERVLET_MULTIPART_PARAM, param);
					}
					ByteArrayOutputStream ba = new ByteArrayOutputStream();
					byte[] buf = new byte[512];
					int n;
					while ((n = mime.read(buf)) != -1){
						ba.write(buf, 0, n);
					}
					Vector val = (Vector)param.get(name);
					byte[] barray = ba.toByteArray();
					if (val == null){
						param.put(name, new Object[]{barray});
					} else {
						val.addElement(barray);
					}
				}
			}
			return null;
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function readMultipartRequest( { request, } handler(mime, name, filename, ctype))";
	}
}
