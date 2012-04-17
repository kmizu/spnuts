/*
 * open.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import org.pnuts.lib.PathHelper;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;

/*
 * function open(input {, mode })
 */
public class open extends PnutsFunction {

	public open(){
		super("open");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	static File getFile(Object input, Context context){
		File file;
		if (input instanceof String){
			file = PathHelper.getFile((String)input, context);
		} else if (input instanceof File){
			file = (File)input;
		} else {
			throw new IllegalArgumentException(String.valueOf(input));
		}
		if (file.getPath().length() == 0){
			throw new PnutsException("pnuts.io.errors", "empty.fileName", new Object[]{}, context);
		}
		return file;
	}
	
	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 1){
				Object arg0 = args[0];
				if (arg0 instanceof InputStream){
					return arg0;
				} else if (arg0 instanceof URL){
					return ((URL)arg0).openStream();
				} else if (arg0 instanceof byte[]){
					return new ByteArrayInputStream((byte[])arg0);
				} else {
					File file = getFile(args[0], context);
					return new BufferedInputStream(new FileInputStream(file));
				}
			} else if (nargs == 2){
				Object input = args[0];
				Object mode = args[1];
				if ("r".equals(mode) || "R".equals(mode)){
					if (input instanceof InputStream){
						return input;
					} else if (input instanceof URL){
						return ((URL)input).openStream();
					} else if (input instanceof byte[]){
						return new ByteArrayInputStream((byte[])input);
					} else {
						File file = getFile(input, context);
						return new BufferedInputStream(new FileInputStream(file));
					}
				} else if ("w".equals(mode) || "W".equals(mode)){
					if (input instanceof OutputStream){
						return input;
					} else if (input instanceof URL){
						URLConnection con = ((URL)input).openConnection();
						con.setDoOutput(true);
						return con.getOutputStream();
					} else if (input instanceof byte[]){
						return new ByteArrayOutputStream();
					} else {
						File file = getFile(input, context);
						PathHelper.ensureBaseDirectory(file);
						return new BufferedOutputStream(new FileOutputStream(file));
					}
				} else if ("a".equals(mode) || "A".equals(mode)){
					if (input instanceof URL){
						throw new IllegalArgumentException(String.valueOf(input));
					} else if (input instanceof byte[]){
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						byte[] bytes = (byte[])input;
						bout.write(bytes);
						return bout;
					} else {
						File file = getFile(input, context);
						PathHelper.ensureBaseDirectory(file);
						return new BufferedOutputStream(new FileOutputStream(file.toString(), true));
					}
				} else {
					throw new IllegalArgumentException(String.valueOf(mode));
				}
			} else {
				undefined(args, context);
				return null;
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function open((String|File|URL|byte[]) {, (\"r\"|\"w\"|\"a\") } )";
	}
}
