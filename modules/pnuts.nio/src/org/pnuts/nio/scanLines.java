/*
 * @(#)scanLines.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.nio;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.net.*;
import pnuts.lang.*;
import pnuts.io.CharacterEncoding;
import org.pnuts.text.*;
import org.pnuts.lib.PathHelper;
import org.pnuts.io.URLHelper;

/*
 * function scanLines((inputStream|reader|file|fileName|url), (func(line) | collection) {, includeNewLine })
 */
public class scanLines extends PnutsFunction {

	public scanLines(){
		super("scanLines");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2 || narg == 3);
	}

	static boolean requiresDoubleByte(Context context){
		String enc = CharacterEncoding.getCharacterEncoding(context)[0];
	
		return !(enc != null && "ascii".equals(enc.toLowerCase()));
	}

	static LineProcessor getLineProcessor(Object arg, LineHandler handler, Context context)
		throws IOException
		{
			AbstractLineReader lineReader;

			if (arg instanceof InputStream){
				if (requiresDoubleByte(context)){
					return new LineReader(CharacterEncoding.getReader((InputStream)arg, context), handler, false);
				} else {
					return new LineInputStream((InputStream)arg, handler, false);
				}
			} else if (arg instanceof Reader){
				return new LineReader((Reader)arg, handler, false);
			} else if (arg instanceof File){
				if (requiresDoubleByte(context)){
					return new LineReader(CharacterEncoding.getReader(new FileInputStream((File)arg), context), handler, true);
				} else {
					return new LineInputStream(new FileInputStream((File)arg), handler, true);
				}
			} else if (arg instanceof String){
				if (requiresDoubleByte(context)){
					return new LineReader(CharacterEncoding.getReader(new FileInputStream(PathHelper.getFile((String)arg, context)), context), handler, true);
				} else {
					return new LineInputStream(new FileInputStream(PathHelper.getFile((String)arg, context)), handler, true);
				}
			} else if (arg instanceof URL){
				return new LineReader(URLHelper.getReader((URL)arg, context), handler, true);
			} else if (arg instanceof ByteBuffer){
				return new ByteBufferLineInputStream((ByteBuffer)arg, handler, false);
			} else if (arg instanceof CharBuffer){
				lineReader = new CharBufferLineReader((CharBuffer)arg);
			} else if (arg instanceof CharSequence){
				lineReader = new CharBufferLineReader(CharBuffer.wrap((CharSequence)arg));
			} else {
				throw new IllegalArgumentException();
			}
			return lineReader;
		}

	static LineHandler getLineHandler(Object arg, Context context){
		if (arg instanceof PnutsFunction){
			return new CallbackLineHandler((PnutsFunction)arg, context);
		} else if (arg instanceof Collection){
			return new CollectionLineHandler((Collection)arg);
		} else if (arg == null){
			return new LineHandler(){
					public void process(char[] c, int offset, int length){}
					public void process(byte[] b, int offset, int length){}
				};
		} else {
			throw new IllegalArgumentException();
		}
	}

	protected Object exec(final Object args[], final Context context){
		boolean includeNewLine = false;
		final Object arg0 = args[0];
		LineProcessor lineReader = null;
		switch (args.length){
		case 3:
			includeNewLine = ((Boolean)args[2]).booleanValue();
		case 2:
			try {
				LineHandler handler = getLineHandler(args[1], context);
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
							lineReader =
								getLineProcessor(arg0,
												 getLineHandler(new PnutsFunction(){
														 protected Object exec(Object[] args, Context c){
															 closure.call(args, context);
															 return null;
														 }
													 }, context),
												 context);
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
		return "function scanLines((inputStream|reader|file|fileName|url) {, (func(line) | collection) {, includeNewLine }})";
	}
}
