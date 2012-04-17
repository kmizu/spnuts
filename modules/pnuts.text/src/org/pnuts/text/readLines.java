/*
 * @(#)readLines.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import pnuts.lang.*;
import org.pnuts.lib.PathHelper;
import org.pnuts.io.URLHelper;
import pnuts.io.CharacterEncoding;
import java.io.*;
import java.util.*;
import java.net.URL;

/**
 * An implementation of readLines().
 */
public class readLines extends PnutsFunction {

	public readLines(){
		super("readLines");
	}

	public boolean defined(int narg){
		return (narg >= 1 && narg <= 3);
	}

	static LineProcessor getLineProcessor(Object arg, LineHandler handler, Context context)
		throws IOException
		{
			if (arg instanceof InputStream){
				return new LineReader(CharacterEncoding.getReader((InputStream)arg, context), handler, false);
			} else if (arg instanceof Reader){
				return new LineReader((Reader)arg, handler, false);
			} else if (arg instanceof File){
				return new LineReader(CharacterEncoding.getReader(new FileInputStream((File)arg), context), handler, true);
			} else if (arg instanceof String){
				return new LineReader(CharacterEncoding.getReader(new FileInputStream(PathHelper.getFile((String)arg, context)), context), handler, true);
			} else if (arg instanceof URL){
				return new LineReader(URLHelper.getReader((URL)arg, context), handler, true);
			} else {
				throw new IllegalArgumentException();
			}
		}

	static LineHandler getLineHandler(Object arg, Context context){
		if (arg instanceof PnutsFunction){
			return new CallbackLineHandler((PnutsFunction)arg, context);
		} else if (arg instanceof Collection){
			return new CollectionLineHandler((Collection)arg);
		} else if (arg == null){
			return new LineHandler(){
					public void process(char[] c, int offset, int length){}
				};
		} else {
			throw new IllegalArgumentException();
		}
	}

	protected Object exec(Object args[], Context context){
		boolean includeNewLine = false;
		final Object arg0 = args[0];
		LineProcessor lineReader = null;
		switch (args.length){
		case 3:
			includeNewLine = ((Boolean)args[2]).booleanValue();
		case 2:
			try {
				LineHandler handler = getLineHandler(args[1], context);
				if (arg0 instanceof String || arg0 instanceof File || arg0 instanceof URL){
				}
				lineReader = getLineProcessor(arg0, handler, context);
				return new Integer(lineReader.processAll(includeNewLine));
			} catch (IOException e){
				throw new PnutsException(e, context);
			}
		case 1:
			final boolean newline = includeNewLine;
			return new Generator() {
					public Object apply(final PnutsFunction closure, final Context context){
						LineProcessor lineReader = null;
						try {
							lineReader = getLineProcessor(arg0, getLineHandler(closure, context), context);
							lineReader.processAll(newline);
						} catch (IOException e){
							throw new PnutsException(e, context);
						}
						return null;
					}
				};
		default:
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function readLines((inputStream|reader|file|fileName|url) {, (func(line) | collection) {, includeNewLine }} )";
	}
}
