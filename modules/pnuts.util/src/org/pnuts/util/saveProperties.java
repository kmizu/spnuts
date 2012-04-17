/*
 * @(#)saveProperties.java 1.1 05/01/15
 *
 * Copyright (c) 2001-2005 Sun Microsystems, Inc. All Rights Reserved.
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
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.pnuts.lib.PathHelper;

/*
 * function loadProperties(input)
 */
public class saveProperties extends PnutsFunction {

	public saveProperties(){
		super("saveProperties");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Properties prop;
		Object dest;
		if (nargs == 2){
			prop = (Properties)args[0];
			dest = args[1];
		} else {
			undefined(args, context);
			return null;
		}
		OutputStream out = null;
		try {
			File destFile = null;
			if (dest instanceof String){
				destFile = PathHelper.getFile((String)dest, context);
				FileOutputStream fout = null;
				try {
					fout = new FileOutputStream(destFile);
					prop.store(fout, "");
				} finally {
					if (fout != null){
						fout.close();
					}
				}
			} else if (dest instanceof File){
				destFile = (File)dest;
				FileOutputStream fout = null;
				try {
					fout = new FileOutputStream(destFile);
					prop.store(fout, "");
				} finally {
					if (fout != null){
						fout.close();
					}
				}
			} else if (dest instanceof OutputStream){
				prop.store((OutputStream)dest, "");
			}
			return null;
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function saveProperties(properties, String|File|OutputStream)";
	}
}
