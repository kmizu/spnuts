/*
 * @(#)ClassPath.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib;

import java.io.*;
import java.net.*;
import java.util.*;

class ClassPath {

	private URL[] urls;
	private Vector loaders = new Vector();
	private Hashtable lmap = new Hashtable();	// {URL, loader}
	private URLStreamHandler jarHandler;

	public ClassPath(URL[] urls) {
		this(urls, null);
	}

	public ClassPath(URL[] urls, URLStreamHandlerFactory factory) {
		this.urls = urls;
		if (factory != null) {
			jarHandler = factory.createURLStreamHandler("jar");
		}
	}

	public URL[] getURLs(){
		return urls;
	}

	public Resource getResource(String name) {
		for (int i = 0; i < urls.length; i++){
			Loader loader = (Loader)lmap.get(urls[i]);
			try {
				if (loader == null){
					lmap.put(urls[i], loader = getLoader(urls[i]));
				}
				Resource res = loader.getResource(name);
				if (res != null) {
					return res;
				}
			} catch (IOException e){
				/* ignore */
			}
		}
		return null;
	}

	/*
	 * Returns the Loader for the specified base URL.
	 */
	private Loader getLoader(URL url) throws IOException {
		String file = url.getFile();
		if (file != null && file.endsWith("/")) {
			if ("file".equals(url.getProtocol())) {
				return new FileLoader(url);
			} else {
				throw new RuntimeException("not supported");
			}
		} else {
			return new JarLoader(url/*, jarHandler*/);
		}
	}
}
