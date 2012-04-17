/*
 * @(#)reader.java 1.2 04/12/06
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
import pnuts.io.CharacterEncoding;
import org.pnuts.lib.PathHelper;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

/*
 * function reader(input {, encoding })
 */
public class reader extends PnutsFunction {

	public reader(){
		super("reader");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	private static Reader getReader(Object input, String encoding, Context context)
		throws IOException 
		{
			if (input instanceof Reader){
				return (Reader)input;
			} else if (input instanceof InputStream){
				return new BufferedReader(CharacterEncoding.getReader((InputStream)input, encoding, context));
			} else if (input instanceof File){
				return new BufferedReader(CharacterEncoding.getReader(new FileInputStream((File)input), encoding, context));
			} else if (input instanceof String){
				File file = PathHelper.getFile((String)input, context);
				return new BufferedReader(CharacterEncoding.getReader(new FileInputStream(file), encoding, context));
			} else if (input instanceof URL){
				if (encoding == null){
					return URLHelper.getReader((URL)input, context);
				} else {
					return new BufferedReader(new InputStreamReader(((URL)input).openStream(), encoding));
				}
			} else {
				throw new IllegalArgumentException(String.valueOf(input));
			}
		}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 1){
				return getReader(args[0], null, context);
			} else if (nargs == 2){
				return getReader(args[0], (String)args[1], context);
			} else {
				undefined(args, context);
				return null;
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function reader((Reader|InputStream|URL|String|File) {, encoding } )";
	}
}
