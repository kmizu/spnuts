/*
 * getFile.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.*;
import java.io.*;

/*
 * getFile( { { parent, } name } )
 */
public class getFile extends PnutsFunction {

	private final static String SERVLET_FILE = "pnuts.servlet.file".intern();

	public getFile(){
		super("getFile");
	}

	public boolean defined(int narg){
		return (narg == 0 || narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			return context.get(SERVLET_FILE);
		} else if (nargs == 1){
			File f = (File)context.get(SERVLET_FILE);
			if (f != null){
				f = f.getParentFile();
			}
			Object arg = args[0];
			if (arg instanceof String){
				String name = (String)args[0];
				if (f != null){
					return new File(f, name);
				} else {
					return new File(name);
				}
			} else if (arg instanceof File){
				return arg;
			} else {
				throw new IllegalArgumentException();
			}
		} else if (nargs == 2){
			Object arg = args[0];
			File parent;
			if (arg instanceof String){
				File f = (File)context.get(SERVLET_FILE);
				if (f != null){
					f = f.getParentFile();
				}
				parent = new File(f, (String)arg);
			} else if (arg instanceof File){
				parent = (File)arg;
			} else {
				throw new IllegalArgumentException();
			}
			Object name = args[1];
			if (name instanceof String){
				return new File(parent, (String)name);
			} else {
				throw new IllegalArgumentException();
			}
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function getFile( { { parent, } name } )";
	}
}
