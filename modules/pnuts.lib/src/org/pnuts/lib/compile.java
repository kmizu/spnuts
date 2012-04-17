/*
 * compile.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.zip.ZipOutputStream;
import java.net.URL;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;
import pnuts.compiler.ClassFileHandler;
import pnuts.compiler.FileWriterHandler;
import pnuts.compiler.ZipWriterHandler;
import pnuts.compiler.Compiler;
import pnuts.ext.CachedScript;

public class compile extends PnutsFunction {

	public compile(){
		super("compile");
	}

	public boolean defined(int nargs){
		return nargs >= 1 && nargs <= 3;
	}

       Object compileObject(Object arg, Context context){
		Compiler compiler = new Compiler();
		if (arg instanceof PnutsFunction){
		    return compiler.compile((PnutsFunction)arg, context);
		}
		Pnuts parsed = parse.parse(arg, context);
		if (!(arg instanceof Pnuts)){
		    parsed.setScriptSource(arg);
		}
		try {
		    return compiler.compile(parsed, context);
		} catch (Exception e){
		    return parsed;
		}
       }

	protected Object exec(Object args[], Context context){
		int nargs = args.length;
		if (nargs == 1){
			Object arg = args[0];
			return compileObject(arg, context);
		} else if (nargs == 2){
			Object arg = args[0];
			boolean reload = ((Boolean)args[1]).booleanValue();
			if (!reload ||
			    (arg instanceof InputStream) ||
			    (arg instanceof Reader) ||
			    (arg instanceof String))
			{
			    return compileObject(arg, context);
			}
			URL url;
			try {
			    if (arg instanceof URL){
				url = (URL)arg;
			    } else if (arg instanceof File){
				url = ((File)arg).toURL();
			    } else {
				throw new IllegalArgumentException(String.valueOf(arg));
			    }
			    return new CachedScript(url, context.getScriptEncoding(), context);
			} catch (Exception e){
			    throw new PnutsException(e, context);
			}

		} else if (nargs == 3){
			Object src = args[0];
			String name = (String)args[1];
			Object dest = args[2];
			Compiler compiler = new Compiler(name, false, true);
			ClassFileHandler handler;
			if (dest instanceof String){
				handler = new FileWriterHandler(new File((String)dest));
			} else if (dest instanceof File){
				handler = new FileWriterHandler((File)dest);
			} else if (dest instanceof ZipOutputStream){
				handler = new ZipWriterHandler((ZipOutputStream)dest);
			} else {
				throw new IllegalArgumentException(String.valueOf(dest));
			}
			Pnuts parsed = parse.parse(src, context);
			if (!(src instanceof Pnuts)){
				parsed.setScriptSource(src);
			}
			compiler.compile(parsed, handler);
		} else {
			undefined(args, context);
		}
		return null;
	}

	public String toString(){
		return "function compile((InputStream|Reader|String|File|URL|Pnuts) input {, reloadUpdatedScript }) or ((Pnuts|String)source, String className, (File|ZipOutputStream)output)";
	}
}
