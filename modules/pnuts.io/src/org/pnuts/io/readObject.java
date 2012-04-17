/*
 * @(#)readObject.java 1.1 05/06/14
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.*;
import pnuts.io.PnutsObjectInputStream;
import org.pnuts.lib.PathHelper;
import java.io.*;

/*
 * readObject(fileOrStream)
 */
public class readObject extends PnutsFunction {

	public readObject(){
		super("readObject");
	}

	public boolean defined(int narg){
		return narg == 1;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 1){
			undefined(args, context);
			return null;
		}
		Object arg0 = args[0];
		PnutsObjectInputStream in;
		InputStream toClose = null;
		try {
			if (arg0 instanceof InputStream){
				in = new PnutsObjectInputStream((InputStream)arg0, context);
			} else if (arg0 instanceof File){
				toClose = new FileInputStream((File)arg0);
				in = new PnutsObjectInputStream(toClose, context);
			} else if (arg0 instanceof String){
				toClose = new FileInputStream(PathHelper.getFile((String)arg0, context));
				in = new PnutsObjectInputStream(toClose, context);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}

			return in.readObject();
		} catch (Exception ioe){
			throw new PnutsException(ioe, context);
		} finally {
			if (toClose != null){
				try {
					toClose.close();
				} catch (IOException e){
				}
			}
		}
	}

	public String toString(){
		return "function readObject(InputStream|String|File)";
	}
}
