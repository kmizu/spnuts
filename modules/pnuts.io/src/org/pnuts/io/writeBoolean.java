/*
 * @(#)writeBoolean.java 1.2 04/12/06
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
 * writeBoolean(DataOutput dout, boolean value)
 * writeBoolean(DataOutput dout, boolean[] src, int offset, int len)
 */
public class writeBoolean extends PnutsFunction {

	public writeBoolean(){
		super("writeBoolean");
	}

	public boolean defined(int narg){
		return (narg == 2 || narg == 4);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 2){
				DataOutput dout = (DataOutput)args[0];
				boolean value = ((Boolean)args[1]).booleanValue();
				dout.writeBoolean(value);
				return new Integer(1);
			} else if (nargs == 4){
				DataOutput dout = (DataOutput)args[0];
				boolean[] src = (boolean[])args[1];
				int offset = ((Integer)args[2]).intValue();
				int size = ((Integer)args[3]).intValue();
		
				for (int i = 0; i < size; i++){
					dout.writeBoolean(src[offset + i]);
				}
				return new Integer(size);
			} else {
				undefined(args, context);
				return null;
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function writeBoolean(DataOutput dout , { boolean value |  boolean[] src, int offset, int len })";
	}
}
