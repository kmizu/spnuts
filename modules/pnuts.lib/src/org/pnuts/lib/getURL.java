/*
 * @(#)getURL.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import pnuts.lang.Runtime;
import pnuts.lang.*;

public class getURL extends PnutsFunction {

	public getURL(){
		super("getURL");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		URL base = null;
		if (nargs == 2){
			Object arg0 = args[0];
			if (arg0 instanceof URL){
				base = (URL)arg0;
			} else {
				base = (URL)exec(new Object[]{arg0}, context);
			}
			try {
				return new URL(base, (String)args[1]);
			} catch (MalformedURLException e){
				throw new PnutsException(e, context);
			}
		} else if (nargs == 1){
			Object arg = args[0];
			if (arg instanceof URL){
				return (URL)arg;
			} else if (arg instanceof File){
				try {
					return Runtime.fileToURL((File)arg);
				} catch (IOException e){
					throw new PnutsException(e, context);
				}
			} else if (arg instanceof String){
				try {
					return new URL((String)arg);
				} catch (MalformedURLException e){
					throw new PnutsException(e, context);
				}
			} else if (arg instanceof URI){
			       try {
				   return ((URI)arg).toURL();
			       } catch (MalformedURLException e){
				   throw new PnutsException(e, context);
			       }
			} else {
				throw new IllegalArgumentException(String.valueOf(arg));
			}
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function getURL({base ,} url)";
	}
}
