/*
 * @(#)CharacterEncoding.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.io;

import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import java.io.*;

public class CharacterEncoding {
	private final static String INPUT_CHARACTER_ENCODING = "pnuts$io$characterEncoding$in".intern();
	private final static String OUTPUT_CHARACTER_ENCODING = "pnuts$io$characterEncoding$out".intern();

	public static Reader getReader(InputStream in, Context context){
		return getReader(in, null, context);
	}

	public static Reader getReader(InputStream in, String enc, Context context){
		if (enc == null){
			enc = (String)context.get(INPUT_CHARACTER_ENCODING);
		}
		if (enc != null){
			try {
				return new InputStreamReader(in, enc);
			} catch (UnsupportedEncodingException e){
				throw new PnutsException(e, context);
			}
		} else {
			return new InputStreamReader(in);
		}
	}

	public static Writer getWriter(OutputStream out, Context context){
		return getWriter(out, null, context);
	}

	public static Writer getWriter(OutputStream out, String enc, Context context){
		if (enc == null){
			enc = (String)context.get(OUTPUT_CHARACTER_ENCODING);
		}
		if (enc != null){
			try {
				return new OutputStreamWriter(out, enc);
			} catch (UnsupportedEncodingException e){
				throw new PnutsException(e, context);
			}
		} else {
			return new OutputStreamWriter(out);
		}
	}

	public static void setCharacterEncoding(String in, String out, Context context){
		context.set(INPUT_CHARACTER_ENCODING, in);
		context.set(OUTPUT_CHARACTER_ENCODING, out);
	}

	public static String[] getCharacterEncoding(Context context){
		return new String[]{
			(String)context.get(INPUT_CHARACTER_ENCODING),
			(String)context.get(OUTPUT_CHARACTER_ENCODING)
		};
	}
}
