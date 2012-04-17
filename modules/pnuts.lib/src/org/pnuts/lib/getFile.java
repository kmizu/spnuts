/*
 * @(#)getFile.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;

public class getFile extends PnutsFunction {

	public getFile(){
		super("getFile");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			File file = null;
			Object arg = args[0];
			if (arg instanceof File){
				file = (File)arg;
			} else if (arg instanceof String){
				file = PathHelper.getFile((String)arg, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg));
			}
			if (file.getPath() != ""){
				try {
					return new File(file.getCanonicalPath());
				} catch (IOException e){
					throw new PnutsException(e, context);
				}
			} else {
				return new File("");
			}
		} else if (nargs == 2){
			Object arg0 = args[0];
			if (arg0 instanceof String){
				return new File((String)args[0], (String)args[1]);
			} else if (arg0 instanceof File){
				return new File((File)args[0], (String)args[1]);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
		} else {
			undefined(args, context);
			return null;
		}
	}
	public String toString(){
		return "getFile(String|File {, String })";
	}
}
