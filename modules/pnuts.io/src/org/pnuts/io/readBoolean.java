/*
 * @(#)readBoolean.java 1.2 04/12/06
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
 * readBoolean(DataInput)
 * readBoolean(DataInput, boolean[] data, int offset, int size)
 */
public class readBoolean extends PnutsFunction {

	public readBoolean(){
		super("readBoolean");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 4);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 1){
				DataInput din = (DataInput)args[0];
				return new Boolean(din.readBoolean());
			} else if (nargs == 4){
				DataInput din = (DataInput)args[0];
				boolean[] dest = (boolean[])args[1];
				int offset = ((Integer)args[2]).intValue();
				int size = ((Integer)args[3]).intValue();
				for (int i = 0; i < size; i++){
					dest[offset + i] = din.readBoolean();
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
		return "function readBoolean(DataInput {, boolean[] data, int offset, int size })";
	}
}
