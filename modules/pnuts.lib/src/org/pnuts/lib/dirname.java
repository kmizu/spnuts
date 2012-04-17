/*
 * dirname.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.io.File;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class dirname extends PnutsFunction {
       private final static char FILE_SEPARATOR = '/';
       private final static boolean useBackslash = ("\\".equals(getProperty("file.separator")));

	public dirname(){
		super("dirname");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			String name = null;
			Object arg = args[0];
			if (arg instanceof File){
				name = ((File)arg).getName();
			} else if (arg instanceof String){
				name = (String)arg;
			} else {
				throw new IllegalArgumentException(String.valueOf(arg));
			}
			int idx = name.length();
			idx--;
			char ch = name.charAt(idx);
			if (ch == FILE_SEPARATOR || (useBackslash && ch == '\\')){
			    idx--;
			}
			while (idx >= 0){
			    ch = name.charAt(idx);
			    if (ch == FILE_SEPARATOR || (useBackslash && ch == '\\')){
				return name.substring(0, idx);
			    }
			    idx--;
			}
			return name;
		} else {
			undefined(args, context);
			return null;
		}
	}
	public String toString(){
		return "function dirname(filename)";
	}
}
