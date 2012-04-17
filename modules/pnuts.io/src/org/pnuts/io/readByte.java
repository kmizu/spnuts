/*
 * @(#)readByte.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.*;
import java.io.*;

/**
 * readByte(DataInput)
 * readByte(DataInput, byte[] data, int offset, int size)
 */
public class readByte extends PnutsFunction {

	public readByte(){
		super("readByte");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 4);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 1){
				DataInput din = (DataInput)args[0];
				return new Byte(din.readByte());
			} else if (nargs == 4){
				DataInput din = (DataInput)args[0];
				byte[] dest = (byte[])args[1];
				int offset = ((Integer)args[2]).intValue();
				int size = ((Integer)args[3]).intValue();
				for (int i = 0; i < size; i++){
					dest[offset + i] = din.readByte();
				}
				return null;
			} else {
				undefined(args, context);
				return null;
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function readByte(DataInput {, byte[] data, int offset, int size})";
	}
}
