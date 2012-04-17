/*
 * @(#)loadProperties.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import java.net.URL;
import java.util.Properties;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/*
 * function loadProperties(input)
 */
public class loadProperties extends PnutsFunction {

	public loadProperties(){
		super("loadProperties");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Properties prop;
		if (nargs == 1){
			prop = new Properties();
		} else if (nargs == 2){
			prop = (Properties)args[1];
		} else {
			undefined(args, context);
			return null;
		}
		Object arg0 = args[0];
		InputStream in = null;
		try {
			if (arg0 instanceof String){
				URL url = Pnuts.getResource((String)args[0], context);
				if (url == null){
					throw new FileNotFoundException((String)args[0]);
				}
				in = url.openStream();
				try {
					prop.load(in);
				} finally {
					in.close();
				}
			} else if (arg0 instanceof File){
				in = new FileInputStream((File)arg0);
				try {
					prop.load(in);
				} finally {
					in.close();
				}
			} else if (arg0 instanceof InputStream){
				prop.load((InputStream)arg0);
			} else if (arg0 instanceof URL){
				in = ((URL)arg0).openStream();
				try {
					prop.load(in);
				} finally {
					in.close();
				}
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			return prop;
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function loadProperties(String|File|InputStream|URL)";
	}
}
