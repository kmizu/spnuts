/*
 * @(#)URLHelper.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.lang.Context;
import pnuts.io.CharacterEncoding;
import java.net.*;
import java.io.*;

public class URLHelper {

	public static Reader getReader(URL url, Context context) throws IOException {
		URLConnection con = url.openConnection();
		String contentType = con.getContentType();
		String encoding = null;
		if (contentType != null){
			int semi = contentType.indexOf(';');
			if (semi >= 0) {
				int loc = contentType.indexOf("charset=", semi);
				if (loc >= 0) {
					encoding = contentType.substring(loc + 8).replace('"', ' ').trim();
				}
			}
		}
		return CharacterEncoding.getReader(con.getInputStream(), encoding, context);
	}
}
