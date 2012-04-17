/*
 * @(#)parse.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.IOException;
import java.net.URL;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import pnuts.lang.ParseException;

public class parse extends PnutsFunction {

	public parse(){
		super("parse");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	static Pnuts parse(Object arg, Context context){
		if (arg instanceof Pnuts){
			return (Pnuts)arg;
		} else {
			try {
				if (arg instanceof String){
					return Pnuts.parse((String)arg);
				} else if (arg instanceof URL){
					Pnuts parsed;
					InputStream in = null;
					URL url = (URL)arg;
					try {
						in = url.openStream();
						return Pnuts.parse(Runtime.getScriptReader(in, context), url, context);
					} catch (IOException ioe1){
						throw new PnutsException(ioe1, context);
					} finally {
						if (in != null){
							try {
								in.close();
							} catch (IOException ioe){}
						}
					}
				} else if (arg instanceof File){
					Pnuts parsed;
					File file = (File)arg;
					InputStream in = null;
					try {
						in = new FileInputStream(file);
						return Pnuts.parse(Runtime.getScriptReader(in, context), file.toURL(), context);
					} catch (IOException ioe1){
						throw new PnutsException(ioe1, context);
					} finally {
						if (in != null){
							try {
								in.close();
							} catch (IOException ioe){}
						}
					}
				} else if (arg instanceof InputStream){
					return Pnuts.parse(Runtime.getScriptReader((InputStream)arg, context));
				} else if (arg instanceof Reader){
					return Pnuts.parse((Reader)arg);
				} else {
					throw new IllegalArgumentException(String.valueOf(arg));
				}
			} catch (IOException ioe){
				throw new PnutsException(ioe, context);
			} catch (ParseException e){
				throw new PnutsException(e, context);
			}
		}
	}

	protected Object exec(Object args[], Context context){
		if (args.length == 1){
			return parse(args[0], context);
		} else {
			undefined(args, context);
			return null;
		}
	}
	public String toString(){
		return "function parse(String|InputStream|Reader|File|URL)";
	}
}
