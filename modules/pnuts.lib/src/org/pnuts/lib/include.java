/*
 * @(#)include.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import java.net.URL;
import java.io.Reader;
import java.io.InputStream;
import java.io.FileNotFoundException;

/**
 * include() function
 */
public class include extends PnutsFunction {

	/**
	 * Constructor
	 */
	public include(){
		super("include");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object args[], Context context) {
		try {
			if (args.length != 1){
				undefined(args, context);
				return null;
			}
			Object arg = args[0];
			if (arg instanceof String){
				return Pnuts.load((String)arg, context);
			} else if (arg instanceof URL){
				return Pnuts.load((URL)arg, context);
			} else if (arg instanceof InputStream){
				return Pnuts.load((InputStream)arg, context);
			} else if (arg instanceof Reader){
				return Pnuts.load((Reader)arg, context);
			} else {
				throw new IllegalArgumentException(arg.toString());
			}
		} catch (FileNotFoundException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function include((String|URL|InputStream|Reader) script)";
	}
}
