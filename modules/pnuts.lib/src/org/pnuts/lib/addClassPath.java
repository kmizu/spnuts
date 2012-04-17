/*
 * @(#)addClassPath.java 1.1 05/05/19
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.io.File;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;

public class addClassPath extends PnutsFunction {
	public addClassPath(){
		super("addClassPath");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		Object path = args[0];
		URL url;
		try {
			if (path instanceof String){
				File file = PathHelper.getFile((String)path, context);
				url = file.getAbsoluteFile().toURL();
			} else if (path instanceof File){
				url = ((File)path).getAbsoluteFile().toURL();
			} else if (path instanceof URL){
				url = (URL)path;
			} else {
				throw new IllegalArgumentException(String.valueOf(path));
			}
		} catch (MalformedURLException mue){
			throw new PnutsException(mue, context);
		}
		URL[] urls = getClassPath.getURLs(context);
		ArrayList newList = new ArrayList();
		newList.add(url);
		if (urls != null){
			for (int i = 0; i < urls.length; i++){
				if (!urls[i].equals(url)){
					newList.add(urls[i]);
				}
			}
		}
		URL[] newURLs = (URL[])newList.toArray(new URL[newList.size()]);
		setClassPath.setURLs(newURLs, context);
		return null;
	}
	
	public String toString(){
		return "function addClassPath(pathOrURL)";
	}
}
