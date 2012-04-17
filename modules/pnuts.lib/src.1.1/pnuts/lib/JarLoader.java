/*
 * @(#)JarLoader.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

class JarLoader implements Loader {
	private final static boolean DEBUG = false;
	private ZipFile jar;
	private URL csu;
	private URL base;

	JarLoader(URL url) throws IOException {
		base = new URL("jar", "", -1, url + "!/");
		jar = getJarFile(url);
		csu = url;
	}

	private ZipFile getJarFile(URL url) throws IOException {
		if ("file".equals(url.getProtocol())) {
			String path = url.getFile().replace('/', File.separatorChar);
			File file = new File(path);
			if (!file.exists()) {
				throw new FileNotFoundException(path);
			}
			return new ZipFile(path);
		}
		return null;
	}

	public Resource getResource(final String name) {
		if (jar != null){
			ZipEntry entry = jar.getEntry(name);

			if (entry != null) {
				final URL url;
				final ZipEntry ze = entry;
				try {
					url = new URL(base, name);
				} catch (MalformedURLException e) {
					return null;
				} catch (IOException e) {
					return null;
				}
				return new Resource() {
						public URL getURL(){
							return url;
						}
						public InputStream getInputStream() throws IOException {
							return jar.getInputStream(ze);
						}
						public int getContentLength(){
							return (int)ze.getSize();
						}
					};
			}
			return null;
		} else {
			try {
				if (DEBUG){
					System.out.println("name = " + name);
				}
				final ZipInputStream zin = new ZipInputStream(csu.openStream());
				ZipEntry entry = null;
				while ((entry = zin.getNextEntry()) != null){
					if (DEBUG){
						System.out.println(entry.getName());
					}
					if (name.equals(entry.getName())){
						final URL url;
						final ZipEntry ze = entry;
						try {
							url = new URL(base, name);
						} catch (MalformedURLException e) {
							if (DEBUG){
								e.printStackTrace();
							}
							return null;
						} catch (IOException e) {
							if (DEBUG){
								e.printStackTrace();
							}
							return null;
						}
						return new Resource() {
								public URL getURL(){
									return url;
								}
								public InputStream getInputStream() throws IOException {
									return zin;
								}
								public int getContentLength(){
									return -1;
								}
							};
					}
				}
			} catch (IOException e){ 
				if (DEBUG){
					e.printStackTrace();
				}
			}
			return null;
		}
	}
}

