/*
 * @(#)URLClassLoader.java 1.2 04/12/06
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

/**
 * This class is a subset of java.net.URLClassLoader.
 *
 * @see pnuts.lib.ClassPath
 */
public class URLClassLoader extends ClassLoader {
	private ClassPath ucp;
	private URL[] urls;
	private ClassLoader parent;

	public URLClassLoader(URL[] urls){
		this(urls, null, null);
	}

	public URLClassLoader(URL[] urls, URLStreamHandlerFactory factory){
		this(urls, null, factory);
	}

	public URLClassLoader(URL[] urls, ClassLoader parent){
		this(urls, parent, null);
	}

	public URLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory){
		this.urls = urls;
		this.ucp = new ClassPath(urls, factory);
		this.parent = parent;
	}

	protected synchronized Class loadClass(String name, boolean resolve)
		throws ClassNotFoundException
		{
			Class c = findLoadedClass(name);
			if (c == null) {
				try {
					if (parent != null && resolve){
						c = parent.loadClass(name);
					} else {
						c = findSystemClass(name);
					}
				} catch (ClassNotFoundException e) {
					c = findClass(name);
				}
			}
			if (resolve) {
				resolveClass(c);
			}
			return c;
		}

	protected Class findClass(String name) throws ClassNotFoundException {
		String path = name.replace('.', '/').concat(".class");
		Resource res = ucp.getResource(path);
		if (res != null) {
			try {
				return defineClass(name, res);
			} catch (IOException e) {
				throw new ClassNotFoundException(name);
			}
		} else {
			throw new ClassNotFoundException(name);
		}
	}

	private Class defineClass(String name, Resource res) throws IOException {
		byte[] b = res.getBytes();
		return defineClass(name, b, 0, b.length);
	}

	public URL getResource(String name) {
		URL url = getSystemResource(name);
		if (url == null) {
			url = findResource(name);
		}
		return url;
	}

	public URL findResource(String name){
		Resource res = ucp.getResource(name);
		return res != null ? res.getURL() : null;
	}

	public InputStream getResourceAsStream(String name) {
		URL url = getResource(name);
		try {
			return url != null ? url.openStream() : null;
		} catch (IOException e) {
			return null;
		}
	}

	public URL[] getURLs() {
		return ucp.getURLs();
	}

	private static final String protocolPathProp = "java.protocol.handler.pkgs";

	static {
		Properties p = new Properties(System.getProperties());
		String s = p.getProperty(protocolPathProp);
		if (s != null){
			p.put(protocolPathProp, s + "|pnuts.lib");
		} else {
			p.put(protocolPathProp, "pnuts.lib");
		}
		System.setProperties(p);
	}
}
