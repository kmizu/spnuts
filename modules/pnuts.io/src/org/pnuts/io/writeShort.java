/*
 * @(#)writeShort.java 1.2 04/12/06
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
 * writeShort(DataOutput dout, short value)
 * writeShort(DataOutput dout, short[] src, int offset, int len)
 */
public class writeShort extends PnutsFunction {

	public writeShort(){
		super("writeShort");
	}

	public boolean defined(int narg){
		return (narg == 2 || narg == 4);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 2){
				DataOutput dout = (DataOutput)args[0];
				short value = ((Short)args[1]).shortValue();
				dout.writeShort(value);
				return new Integer(1);
			} else if (nargs == 4){
				DataOutput dout = (DataOutput)args[0];
				short[] src = (short[])args[1];
				int offset = ((Integer)args[2]).intValue();
				int n = ((Integer)args[3]).intValue();
				for (int i = 0; i < n; i++){
					dout.writeShort(src[offset + i]);
				}
				return new Integer(n);
			} else {
				undefined(args, context);
				return null;
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function writeShort(DataOutput dout, {short value | short[] src, int offset, int len })";
	}
}
