/*
 * @(#)writeText.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.*;
import java.io.*;

/*
 * function writeText(text, String|File|OutputStream|Writer {, encoding })
 */
public class writeText extends PnutsFunction {

	private final String WRITER_SYMBOL = "writer".intern();

	public writeText(){
		super("writeText");
	}

	public boolean defined(int narg){
		return (narg == 2 || narg == 3);
	}

	protected Object exec(Object args[], Context context){
		int narg = args.length;
		if (narg != 2 && narg != 3){
			undefined(args, context);
			return null;
		}
		String text = args[0].toString();
		Object output = args[1];
		Writer writer;
		try {
			if (output instanceof Writer){
				writer = (Writer)output;
				writer.write(text);
			} else if (output instanceof OutputStream){
				PnutsFunction writerFunc = (PnutsFunction)context.resolveSymbol(WRITER_SYMBOL);
				Object[] a = new Object[args.length - 1];
				System.arraycopy(args, 1, a, 0, a.length);
				writer = (Writer)writerFunc.call(a, context);
				writer.write(text);
			} else if ((output instanceof File) || (output instanceof String)){
				PnutsFunction writerFunc = (PnutsFunction)context.resolveSymbol(WRITER_SYMBOL);
				Object[] a = new Object[args.length - 1];
				System.arraycopy(args, 1, a, 0, a.length);
				writer = (Writer)writerFunc.call(a, context);
				writer.write(text);
				writer.close();
			} else {
				throw new IllegalArgumentException(output.toString());
			}
			return null;
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function writeText(String, OutputStream|Writer|String|File {, encoding} )";
	}
}
