/*
 * @(#)readText.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.io.CharacterEncoding;
import pnuts.lang.*;
import org.pnuts.lib.PathHelper;
import java.io.*;
import java.net.URL;

/*
 * function readText(Reader|InputStream|String|File|URL {, encoding })
 */
public class readText extends PnutsFunction {

	public readText(){
		super("readText");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object args[], Context context){
		int narg = args.length;
		if (narg != 1 && narg != 2){
			undefined(args, context);
			return null;
		}
		Reader reader;
		StringWriter sw = new StringWriter();
		char[] buf;
		int n;
		Object input = args[0];
		try {
			if (input instanceof Reader){
				return TextReader.getText((Reader)input, false);
			} else if (input instanceof InputStream){
				reader = CharacterEncoding.getReader((InputStream)input, context);
				return TextReader.getText(reader, false);
			} else if (input instanceof String){
				File f = new File((String)input);
				if (args.length == 2){
					return TextReader.getText(f, (String)args[1], context);
				} else {
					InputStream in = new FileInputStream(PathHelper.getFile((String)input, context));
					reader = CharacterEncoding.getReader(in, context);
					return TextReader.getText(reader, (int)(f.length() / 2), true);
				}
			} else if (input instanceof File){
				File f = (File)input;
				if (args.length == 2){
					return TextReader.getText(f, (String)args[1], context);
				} else {
					InputStream in = new FileInputStream(f);
					reader = CharacterEncoding.getReader(in, context);
					return TextReader.getText(reader, (int)(f.length() / 2), true);
				}
			} else if (input instanceof URL){
				URL url = (URL)input;
				if (args.length == 2){
					return TextReader.getText(new InputStreamReader(url.openStream(), (String)args[1]), true);
				} else {
					reader = URLHelper.getReader((URL)input, context);
					return TextReader.getText(reader, true);
				}
			} else {
				throw new IllegalArgumentException(String.valueOf(input));
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function readText(InputStream|Reader|String|File|URL {, encoding })";
	}
}
