/*
 * @(#)JarResourceConnection.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib.jar;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.Method;

/**
 * URLConnection for resource in a JAR file
 * The URL scheme is same as jdk1.2's.
 */
class JarResourceConnection extends URLConnection {
	private String name;	// name of the resource
	private URL jarFileURL;
	private URLConnection jarFileURLConnection;
	private InputStream in;
	private ZipFile jarFile;
	private ZipEntry jarEntry;
	private boolean connected = false;
	private String contentType = null;

	protected JarResourceConnection (URL url)
		throws MalformedURLException, IOException
		{
			super(url);
			String file = url.getFile();
			if (file.charAt(0) == '/'){
				file = file.substring(1);
			}
			int idx = file.indexOf("!/");
			if (idx < 0){
				throw new MalformedURLException();
			}
			String sub = file.substring(0, idx);
			name = file.substring(idx + 2);

			jarFileURL = new URL(sub);
			try {
				jarFile = new ZipFile(jarFileURL.getFile());
			} catch (IOException e){
				// do nothing
			}
		}

	public void connect() throws IOException {
		if (!connected){
			jarFileURLConnection = jarFileURL.openConnection();
			if (jarFile != null){
				jarEntry = jarFile.getEntry(name);
				in = jarFile.getInputStream(jarEntry);
			} else {
				ZipEntry e = null;
				ZipInputStream zin = new ZipInputStream(jarFileURLConnection.getInputStream());
				while ((e = zin.getNextEntry()) != null){
					if (e.getName().equals(name)){
						jarEntry = e;
						in = zin;
						break;
					}
				}
			}
			connected = true;
		}
	}

	public String getContentType() {
		if (contentType == null) {
			if (name == null) {
				contentType = "x-java/jar";
			} else {
				try {
					connect();
					InputStream in = jarFile.getInputStream(jarEntry);
					contentType = guessContentTypeFromStream(new BufferedInputStream(in));
					in.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
			if (contentType == null) {
				contentType = guessContentTypeFromName(name);
			}
		}
		return contentType;
	}

	public InputStream getInputStream() throws IOException {
		connect();
		if (jarEntry == null){
			throw new FileNotFoundException();
		}
		return in;
	}

	public int getContentLength() {
		int result = -1;
		try {
			connect();
			if (jarEntry == null) {
				result = jarFileURLConnection.getContentLength();
			} else {
				result = (int)jarEntry.getSize();
			}
		} catch (IOException e) {
		}
		return result;
	}
}
