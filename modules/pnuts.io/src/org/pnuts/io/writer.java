/*
 * @(#)writer.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import org.pnuts.lib.PathHelper;
import pnuts.io.CharacterEncoding;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.File;

/*
 * function writer(output {, encoding })
 */
public class writer extends PnutsFunction {

	public writer(){
		super("writer");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	static PrintWriter getWriter(Object output, String enc, Context context)
		throws IOException 
		{
			if (output instanceof PrintWriter){
				return (PrintWriter)output;
			} else if (output instanceof Writer){
				return new PrintWriter((Writer)output);
			} else if (output instanceof OutputStream){
				return new PrintWriter(new BufferedWriter(CharacterEncoding.getWriter((OutputStream)output, enc, context)));
			} else if (output instanceof File){
				File file = (File)output;
				PathHelper.ensureBaseDirectory(file);
				return new PrintWriter(new BufferedWriter(CharacterEncoding.getWriter(new FileOutputStream(file), enc, context)));
			} else if (output instanceof String){
				File file = PathHelper.getFile((String)output, context);
				PathHelper.ensureBaseDirectory(file);
				return new PrintWriter(new BufferedWriter(CharacterEncoding.getWriter(new FileOutputStream(file), enc, context)));
			} else {
				throw new IllegalArgumentException(String.valueOf(output));
			}
		}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 1){
				return getWriter(args[0], null, context);
			} else if (nargs == 2){
				return getWriter(args[0], (String)args[1], context);
			} else {
				undefined(args, context);
				return null;
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function writer((Writer|OutputStream|String|File) {, encoding } )";
	}
}
