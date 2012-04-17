/*
 * sendPostRequest.java
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;
import org.pnuts.net.*;

/*
 * sendPostRequest(url, map, encoding {, handler(urlConnection), { requestProperties } })
 */
public class sendPostRequest extends PnutsFunction {

	public sendPostRequest(){
		super("sendPostRequest");
	}

	public boolean defined(int narg){
		return (narg == 3 || narg == 4 || narg == 5);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		URL url;
		Map param;
		String encoding;
		PnutsFunction handler = null;
		Map hdrs = null;

		if (nargs == 3){
			url = (URL)args[0];
			param = (Map)args[1];
			encoding = (String)args[2];
		} else if (nargs == 4){
			url = (URL)args[0];
			param = (Map)args[1];
			encoding = (String)args[2];
			handler = (PnutsFunction)args[3];
		} else if (nargs == 5){
			url = (URL)args[0];
			param = (Map)args[1];
			encoding = (String)args[2];
			handler = (PnutsFunction)args[3];
			hdrs = (Map)args[4];
		} else {
			undefined(args, context);
			return null;
		}
		post(url, hdrs, param, encoding, handler, context);
		return null;
	}

	static void post(URL url,
					Map hdrs,
					 Map param,
					 String encoding,
					 PnutsFunction handler,
					 Context context)
		{
			try {
				URLConnection con = url.openConnection();
				if (hdrs != null){
					for (Iterator it = hdrs.entrySet().iterator(); it.hasNext();){
						Map.Entry entry = (Map.Entry)it.next();
						con.setRequestProperty((String)entry.getKey(), (String)entry.getValue());
					}
				}
				con.setDoOutput(true);
				con.setUseCaches(false);
				con.setAllowUserInteraction(false);
				con.setRequestProperty("content-type", "application/x-www-form-urlencoded");

				String message = URLEncoding.makeQueryString(param, encoding);
				DataOutputStream out = new DataOutputStream(con.getOutputStream());
				out.writeBytes(message);
				out.close();
				if (handler != null){
					handler.call(new Object[]{con}, context);
				} else {
					InputStream in = con.getInputStream();
					byte[] buf = new byte[512];
					while (in.read(buf) != -1){}
				} 
			} catch (IOException e){
				throw new PnutsException(e, context);
			}
		}

	public String toString(){
		return "function sendPostRequest(url, map, encoding {, handler(urlConnection) { , requestProperties }})";
	}
}
