/*
 * @(#)writeObject.java 1.1 05/06/14
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.*;
import org.pnuts.lib.PathHelper;
import java.io.*;

/*
 * writeObject(object, fileOrStream)
 */
public class writeObject extends PnutsFunction {

	public writeObject(){
		super("writeObject");
	}

	public boolean defined(int narg){
		return narg == 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 2){
			undefined(args, context);
			return null;
		}
		Object arg0 = args[0];
		Object arg1 = args[1];
		ObjectOutputStream out;
		OutputStream toClose = null;
		try {
			if (arg1 instanceof OutputStream){
				out = new ObjectOutputStream((OutputStream)arg1);
			} else if (arg1 instanceof File){
				toClose = new FileOutputStream((File)arg1);
				out = new ObjectOutputStream(toClose);
			} else if (arg1 instanceof String){
				toClose = new FileOutputStream(PathHelper.getFile((String)arg1, context));
				out = new ObjectOutputStream(toClose);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg1));
			}
			out.writeObject(arg0);
			return null;
		} catch (IOException ioe){
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
		return "function writeObject(obj, OutputStream|String|File)";
	}
}
