/*
 * @(#)Handler.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib.jar;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.io.IOException;

/**
 * a URLStreamHandler for JarResourceConnection
 */
public class Handler extends URLStreamHandler {
	public URLConnection openConnection(URL u) throws IOException {
		return new JarResourceConnection(u);
	}
}
