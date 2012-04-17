/*
 * @(#)convertDynamicPage.java 1.2 04/12/06
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.io.*;

public class convertDynamicPage extends PnutsFunction {

	static final String CWD = "cwd".intern();
	PnutsFunction pipe;
	PnutsFunction getFile;

	public convertDynamicPage(){
		super("convertDynamicPage");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		File ifile = null;
		File ofile = null;
		Reader reader = null;
		Writer writer = null;
		int nargs = args.length;
		if (nargs > 2 || nargs == 0){
			undefined(args, context);
			return null;
		}
		try {
			if (nargs >= 1){
				Object arg0 = args[0];
				if (arg0 instanceof String){
					if (getFile == null){
						getFile = (PnutsFunction)context.resolveSymbol("getFile");
					}
					if (getFile == null){
						throw new PnutsException("getFile is not defined", context);
					} 
					ifile = (File)getFile.call(new Object[]{arg0}, context);
					reader = Runtime.getScriptReader(new FileInputStream(ifile), context);
				} else if (arg0 instanceof File){
					ifile = (File)arg0;
					reader = Runtime.getScriptReader(new FileInputStream(ifile), context);
				} else if (arg0 instanceof Reader){
					reader = (Reader)arg0;
				} else if (arg0 instanceof InputStream){
					reader = Runtime.getScriptReader((InputStream)arg0, context);
				} else {
					throw new IllegalArgumentException(String.valueOf(arg0));
				}
			}
			if (nargs >= 2){
				Object arg1 = args[1];
				if (arg1 instanceof String){
					if (getFile == null){
						getFile = (PnutsFunction)context.resolveSymbol("getFile");
					}
					if (getFile == null){
						throw new PnutsException("getFile is not defined", context);
					}
					ofile = (File)getFile.call(new Object[]{arg1}, context);
					writer = new FileWriter(ofile);
				} else if (arg1 instanceof File){
					ofile = (File)arg1;
					writer = new FileWriter(ofile);
				} else if (arg1 instanceof Writer){
					writer = (Writer)arg1;
				} else if (arg1 instanceof OutputStream){
					String enc = context.getScriptEncoding();
					if (enc != null){
						writer = new OutputStreamWriter((OutputStream)arg1, enc);
					} else {
						writer = new OutputStreamWriter((OutputStream)arg1);
					}
				} else {
					throw new IllegalArgumentException(String.valueOf(arg1));
				}
			}
			File dir;
			if (ifile != null){
				dir = ifile.getAbsoluteFile().getParentFile();
			} else {
				String cwd = (String)context.get(CWD);
				if (cwd == null){
					context.set(CWD, cwd = System.getProperty("user.dir"));
				}
				dir = new File(cwd);
			}
			if (writer != null){
				try {
					try {
						DynamicPage.convert(reader, writer, dir.toURL(), context.getScriptEncoding(), context);
					} finally {
						if (ifile != null){
							reader.close();
						}
					}
				} finally {
					if (ofile != null){
						writer.close();
					}
				}
				return null;
			} else {
				if (pipe == null){
					pipe = (PnutsFunction)context.resolveSymbol("pipe");
				}
				if (pipe == null){
					throw new PnutsException("pipe is not defined", context);
				}
				return pipe.call(new Object[]{this, reader}, context);
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function convertDynamicPage(input {, output } )";
	}
}
