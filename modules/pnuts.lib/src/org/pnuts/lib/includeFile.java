/*
 * @(#)includeFile.java 1.3 05/06/07
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
import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;

/**
 * Implements include() function and includeFile() function.
 */
public class includeFile extends PnutsFunction {

	/**
	 * Constructor
	 */
	public includeFile(){
		super("includeFile");
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
				File file = PathHelper.getFile((String)arg, context);
				return Pnuts.loadFile(file.getPath(), context);
			} else if (arg instanceof File){
				return Pnuts.loadFile(((File)arg).getPath(), context);
			} 
			throw new IllegalArgumentException(String.valueOf(arg));


		} catch (FileNotFoundException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function includeFile(String fileName)";
	}
}
