/*
 * @(#)writeUTF.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.*;
import java.io.*;

/*
 * writeUTF(DataOutput dout, String str)
 */
public class writeUTF extends PnutsFunction {

	public writeUTF(){
		super("writeUTF");
	}

	public boolean defined(int narg){
		return (narg == 2);
	}

	public Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 2){
				DataOutput dout = (DataOutput)args[0];
				String str = (String)args[1];
				dout.writeUTF(str);
			} else {
				undefined(args, context);
			}
			return null;
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function writeUTF(DataOutput dout, String str)";
	}
}
