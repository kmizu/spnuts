/*
 * @(#)dataInput.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import java.io.InputStream;
import java.io.DataInputStream;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class dataInput extends PnutsFunction {

	public dataInput(){
		super("dataInput");
	}

	public boolean defined(int narg){
		return (narg == 1);
	}

	protected Object exec(Object[] args, Context context){
		if (args.length == 1){
			return new DataInputStream((InputStream)args[0]);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function dataInput(InputStream)";
	}
}
