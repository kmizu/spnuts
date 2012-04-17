/*
 * @(#)readChar.java 1.2 04/12/06
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
 * void readChar(DataInput)
 * void readChar(DataInput, char[] data, int offset, int size)
 */
public class readChar extends PnutsFunction {

	public readChar(){
		super("readChar");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 4);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 1){
				DataInput din = (DataInput)args[0];
				return new Character(din.readChar());
			} else if (nargs == 4){
				DataInput din = (DataInput)args[0];
				char[] dest = (char[])args[1];
				int offset = ((Integer)args[2]).intValue();
				int size = ((Integer)args[3]).intValue();
				for (int i = 0; i < size; i++){
					dest[offset + i] = din.readChar();
				}
				return null;
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
		undefined(args, context);
		return null;
	}

	public String toString(){
		return "function readChar(DataInput {, char[] data, int offset, int size} )";
	}
}
