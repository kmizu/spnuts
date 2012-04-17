/*
 * @(#)setClassPath.java 1.1 05/05/19
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.io.File;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;

public class setClassPath extends PnutsFunction {
	public setClassPath(){
		super("setClassPath");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}


	static void setURLs(URL[] urls, Context context){
		URLClassLoader ucl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		context.setClassLoader(ucl);
		Thread.currentThread().setContextClassLoader(ucl);
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		Object arg = args[0];
		if (arg == null){

			context.setClassLoader(null);
			Thread.currentThread().setContextClassLoader(null);
		} else {
			Enumeration e = context.getConfiguration().toEnumeration(arg);
			if (e != null){
				ArrayList list = new ArrayList();
				try {
					while (e.hasMoreElements()){
						Object elem = e.nextElement();
						if (elem instanceof String){
							File file = PathHelper.getFile((String)elem, context);
							list.add(file.toURL());
						} else if (elem instanceof File){
							list.add(((File)elem).toURL());
						} else if (elem instanceof URL){
							list.add(elem);
						} else {
							throw new IllegalArgumentException(String.valueOf(elem));
						}
					}
				} catch (MalformedURLException mue){
					throw new PnutsException(mue, context);
				}
				URL[] urls = new URL[list.size()];
				list.toArray(urls);
				setURLs(urls, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg));
			}
		}
		return null;
	}
	
	public String toString(){
		return "function setClassPath(urlsOrPaths)";
	}
}
