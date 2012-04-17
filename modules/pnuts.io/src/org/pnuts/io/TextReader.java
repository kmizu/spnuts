/*
 * @(#)TextReader.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.io.CharacterEncoding;
import pnuts.lang.*;
import java.io.*;
import java.net.URL;

/*
 * function readText(Reader|InputStream|String|File|URL {, encoding })
 */
class TextReader {

	protected static Object getText(File file, String encoding, Context context)
		throws IOException
		{
			long size = file.length();
			if (size > Integer.MAX_VALUE){
				throw new RuntimeException("too large");
			} else {
				return getText(new BufferedReader(CharacterEncoding.getReader(new FileInputStream(file), encoding, context)),
							   (int)size / 2,
							   true);
			}
		}

	protected static Object getText(Reader reader, boolean needToClose)
		throws IOException
		{
			return getText(reader, -1, needToClose);
		}

	protected static Object getText(Reader reader, int hint, boolean needToClose)
		throws IOException
		{
			int n;
			int isize = 512;

			if (hint > 0){
				isize = hint;
			}
			char[] buf = new char[8192];
			StringWriter sw = new StringWriter(isize);
			try {
				while ((n = reader.read(buf, 0, buf.length)) != -1){
					sw.write(buf, 0, n);
				}
				return sw.toString();
			} finally {
				if (needToClose){
					reader.close();
				}
			}
	}
}
