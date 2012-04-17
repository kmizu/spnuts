/*
 * @(#)writeDouble.java 1.2 04/12/06
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
 * writeDouble(DataOutput dout, double value)
 * writeDouble(DataOutput dout, double[] src, int offset, int len)
 */
public class writeDouble extends PnutsFunction {

	public writeDouble(){
		super("writeDouble");
	}

	public boolean defined(int narg){
		return (narg == 2 || narg == 4);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 2){
				DataOutput dout = (DataOutput)args[0];
				double value = ((Double)args[1]).doubleValue();
				dout.writeDouble(value);
				return new Integer(1);
			} else if (nargs == 4){
				DataOutput dout = (DataOutput)args[0];
				double[] src = (double[])args[1];
				int offset = ((Integer)args[2]).intValue();
				int n = ((Integer)args[3]).intValue();
				for (int i = 0; i < n; i++){
					dout.writeDouble(src[offset + i]);
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
		return "function writeDouble(DataOutput dout, { double value | double[] src, int offset, int len })";
	}
}
