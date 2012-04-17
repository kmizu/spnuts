/*
 * @(#)openURL.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;

public class openURL extends PnutsFunction {

	public openURL(){
		super("openURL");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	static URL getURL(Object input) throws IOException {
		if (input instanceof URL){
			return (URL)input;
		} else if (input instanceof File){
			return Runtime.fileToURL((File)input);
		} else if (input instanceof String){
			return new URL((String)input);
		} else {
			throw new IllegalArgumentException(String.valueOf(input));
		}
	}

	protected Object exec(Object[] args, Context context){
		if (args.length == 1){
			try {
				return getURL(args[0]).openStream();
			} catch (IOException e){
				throw new PnutsException(e, context);
			}
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function openURL(URL|String|File)";
	}
}
