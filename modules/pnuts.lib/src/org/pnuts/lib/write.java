/*
 * @(#)write.java 1.2 04/12/06
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.io.*;

public class write extends PnutsFunction {

	public write(){
		super("write");
	}

	public boolean defined(int narg){
		return narg == 1 || narg == 3;
	}

	public Object exec(Object[] args, Context context){
		PrintWriter writer = context.getWriter();
		OutputStream output = context.getOutputStream();

		if (output == null && writer == null){
			return null;
		}
		int nargs = args.length;
		if (nargs == 1){
			try {
				Object arg0 = args[0];
				if (arg0 instanceof Number){
					int ch = ((Number)arg0).intValue();
					if (output != null){
						output.write(ch);
					} else {
						writer.write(ch);
					}
				} else if (arg0 instanceof Character){
					char ch = ((Character)arg0).charValue();
					if (output != null){
						output.write(ch);
					} else {
						writer.write(ch);
					}
				} else if (arg0 instanceof byte[]){
					if (output != null){
						output.write((byte[])arg0);
					} else {
						writer.write(new String((byte[])arg0));
					}
				} else if (arg0 instanceof char[]){
					if (writer != null){
						char[] buf = (char[])arg0;
						writer.write(buf, 0, buf.length);
					}
				} else {
					throw new IllegalArgumentException();
				}
			} catch (IOException ioe){
				throw new PnutsException(ioe, context);
			}
		} else if (nargs == 3){
			try {
				Object arg0 = args[0];
				int offset = ((Integer)args[1]).intValue();
				int size = ((Integer)args[2]).intValue();
				if (arg0 instanceof byte[]){
					if (output != null){
						byte[] buf = (byte[])arg0;
						output.write(buf, offset, size);
					} else {
						writer.write(new String((byte[])arg0, offset, size));
					}
				} else if (arg0 instanceof char[]){
					if (writer != null){
						writer.write((char[])arg0, offset, size);
					}
				} else {
					throw new IllegalArgumentException();
				}
			} catch (IOException ioe){
				throw new PnutsException(ioe, context);
			}
		} else {
			undefined(args, context);
		}
		return null;
	}

	public String toString(){
		return "function write(c {, offset, size})";
	}
}
