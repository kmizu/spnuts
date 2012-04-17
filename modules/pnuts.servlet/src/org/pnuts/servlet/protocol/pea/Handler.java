/*
 * @(#)Handler.java 1.2 04/12/06
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet.protocol.pea;

import java.io.*;
import java.net.*;
import java.util.*;
import pnuts.lang.Context;

public class Handler extends URLStreamHandler {
	private Context context;
       private Set scriptURLs;

	public Handler(Context context, Set scriptURLs){
		this.context = context;
		this.scriptURLs = scriptURLs;
	}
	protected URLConnection openConnection(URL url) throws IOException {
		return new DynamicPageURLConnection(url, context, scriptURLs);
	}
}
