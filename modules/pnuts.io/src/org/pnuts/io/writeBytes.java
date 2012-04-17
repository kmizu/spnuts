/*
 * @(#)writeBytes.java 1.2 04/12/06
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
 * writeBytes(OutputStream out, byte[] b)
 * writeBytes(OutputStream out, byte[] b, int offset, int len)
 */
public class writeBytes extends PnutsFunction {

	public writeBytes(){
		super("writeBytes");
	}
	public boolean defined(int nargs){
		return nargs == 2 || nargs == 4;
	}
	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 2 && nargs != 4){
			undefined(args, context);
			return null;
		}
		OutputStream out = (OutputStream)args[0];
		byte[] b = (byte[])args[1];

		int offset, size;
		if (nargs == 4){
			offset = ((Integer)args[2]).intValue();
			size = ((Integer)args[3]).intValue();
		} else {
			offset = 0;
			size = b.length;
		}
		try {
			out.write(b, offset, size);
			out.flush();
			return null;
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function writeBytes(OutputStream out, byte[] b {, int offset, int len})";
	}
}
