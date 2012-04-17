/*
 * readDynamicPage.java
 *
 * Copyright (c) 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;
import pnuts.compiler.Compiler;
import java.io.*;
import java.net.*;
import org.pnuts.servlet.protocol.pea.Handler;

public class readDynamicPage extends PnutsFunction {

	private final static String SERVLET_COMPILER = "pnuts.servlet.compiler".intern();

	public readDynamicPage(){
		super("readDynamicPage");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2 || nargs == 3;
	}

	static URL getDynamicPageURL(URL url, String encoding, Context context){
		try {
			String specURL;
			if (encoding == null){
				specURL = "pea:" + url.toExternalForm();
			} else {
				specURL = "pea:" + url.toExternalForm() + "!charset=" + encoding;
			}
			return new URL(null, specURL, new Handler(context, null));
		} catch (MalformedURLException mue){
			mue.printStackTrace();
			return null;
		}
	}

	static Reader getReader(InputStream in, String enc, Context context)
		throws UnsupportedEncodingException 
		{
			if (enc != null){
				return new InputStreamReader(in, enc);
			} else {
				return Runtime.getScriptReader(in, context);
			}
		}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0 || nargs > 3){
			undefined(args, context);
		}
		Object arg = args[0];
		String encoding;
		if (nargs >= 2){
			encoding = (String)args[1];
			if (encoding == null){
			    encoding = context.getScriptEncoding();
			}
		} else {
			encoding = context.getScriptEncoding();
		}

                boolean reloadUpdatedScripts = true;
                if (nargs >= 3){
                    reloadUpdatedScripts = ((Boolean)args[2]).booleanValue();
                }
		Pnuts parsed = null;
		URL url = null;
		Reader reader = null;
	
		final Compiler compiler = (Compiler)context.get(SERVLET_COMPILER);
		URL baseloc = null;
		try {
			if (arg instanceof InputStream){
				reader = getReader((InputStream)arg, encoding, context);
			} else if (arg instanceof Reader){
				reader = (Reader)arg;
			} else if (arg instanceof URL){
				url = (URL)arg;
				baseloc = url;
			} else if (arg instanceof File){
				File file = (File)arg;
				url = file.toURL();
				baseloc = file.getAbsoluteFile().getParentFile().toURL();
			} else if (arg instanceof String){
				url = Pnuts.getResource((String)arg, context);
				baseloc = url;
			} else {
				throw new IllegalArgumentException(String.valueOf(arg));
			}
			if (url != null){
			    return new DynamicPage(url, encoding, context, reloadUpdatedScripts){
					protected Compiler getCompiler(){
					    return compiler;
					}
				    };			    
			} else {
			    StringWriter sw = new StringWriter();
			    DynamicPage.convert(reader, sw, baseloc, encoding, context, null);
			    parsed = Pnuts.parse(new StringReader(sw.toString()), null, context);
			    if (compiler != null){
				parsed = compiler.compile(parsed, context);
			    }
			    return parsed;
			}
		} catch (Throwable t){
			throw new PnutsException(t, context);
		}
	}

	public String toString(){
		return "function readDynamicPage((String|InputStream|Reader|File|URL) input {, encoding {, reload_updated_scripts}})";
	}
}
