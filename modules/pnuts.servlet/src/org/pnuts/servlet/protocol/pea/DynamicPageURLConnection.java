/*
 * @(#)DynamicPageURLConnection.java 1.2 04/12/06
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet.protocol.pea;

import pnuts.servlet.*;
import pnuts.lang.Context;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.*;

/*
 * Protocol handler for dynamic pages, that is used in debugging.
 *
 * pea:<spec-url>!charset=<charset>
 */
public class DynamicPageURLConnection extends URLConnection {
	private URL specURL;
    private Set scriptURLs;
	private URLConnection specURLConnection;
	private File dir;
	private String encoding;
	private boolean connected;
	private Context context;

	public DynamicPageURLConnection(URL url, Context context, Set scriptURLs) throws IOException {
		super(url);
		this.context = context;
		this.scriptURLs = scriptURLs;
		parseSpecs(url);
	}

	private void parseSpecs(URL url) throws MalformedURLException {
		String spec = url.getFile();
		int idx = spec.indexOf('!');
		if (idx > 0){
			this.specURL = new URL(spec.substring(0, idx));
			if (spec.length() > idx  + 1){
				if (spec.startsWith("charset=", idx + 1)){
					this.encoding = spec.substring(idx + 9);
				}
			}
		} else {
			this.specURL = new URL(spec);
		}
	}

	public int getContentLength(){
		return -1;
	}

	public String getContentType() {
		return "application/pnuts-dynamic-page";
	}	

	public synchronized void connect() throws IOException {
		if (connected){
			return;
		}
		this.specURLConnection = specURL.openConnection();
		specURLConnection.connect();
		connected = true;
	}

	public InputStream getInputStream() throws IOException {
		if (!connected){
			connect();
		}
		Reader reader;
		if (encoding == null){
			reader = new InputStreamReader(specURLConnection.getInputStream());
		} else {
			reader = new InputStreamReader(specURLConnection.getInputStream(), encoding);
		}
		StringWriter writer = new StringWriter();
		DynamicPage.convert(reader, writer, specURL, encoding, context, scriptURLs);
		if (encoding == null){
		    return new ByteArrayInputStream(writer.toString().getBytes());
		} else {
		    return new ByteArrayInputStream(writer.toString().getBytes(encoding));
		}
	}
}
