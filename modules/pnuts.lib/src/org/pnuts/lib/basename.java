/*
 * basename.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

public class basename extends PnutsFunction {

	public basename(){
		super("basename");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			String name = null;
			Object arg = args[0];
			if (arg instanceof File){
				name = ((File)arg).getName();
			} else if (arg instanceof String){
				name = new File((String)arg).getName();
			} else {
				throw new IllegalArgumentException(String.valueOf(arg));
			}
			int idx = name.lastIndexOf('.');
			if (idx > 0){
			    return name.substring(0, idx);
			} else {
			    return name;
			}
		} else if (nargs == 2){
			Object arg0 = args[0];
			Object arg1 = args[1];
			String name;
			if (arg0 instanceof String){
			    name = new File((String)arg0).getName();
			} else if (arg0 instanceof File){
			    name = ((File)arg0).getName();
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			String suffix = (String)arg1;
			if (name.endsWith(suffix)){
			    return name.substring(0, name.length() - suffix.length());
			} else {
			    return name;
			}
		} else {
			undefined(args, context);
			return null;
		}
	}
	public String toString(){
		return "function basename(filename {, suffix })";
	}
}
