/*
 * @(#)MimeInputStream.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import java.io.*;
import java.util.*;

/**
 * A class to parse MIME headers.
 */
public class MimeInputStream extends FilterInputStream {

	protected Hashtable headers;

	public MimeInputStream(InputStream in) throws IOException {
		super(in);
		readHeaders();
	}

	protected void readHeaders() throws IOException {
		headers = new Hashtable();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		int ch = read();
		while (ch > 0){
			if (ch == '\r'){
				ch = read();
				if (ch == '\n'){
					byte[] b = bout.toByteArray();
					if (b.length == 0){
						return;
					}
					int len = b.length;
					for (int i = 0; i < len; i++){
						if (b[i] == ':'){
							String key = new String(b, 0, i, "8859_1");
							i++;
							while (i < len){
								if (b[i] == ' ' || b[i] == '\t'){
									i++;
								} else {
									break;
								}
							}
							String value = new String(b, i, len - i, "8859_1");
							headers.put(key.toLowerCase(), value);
							bout.reset();
							break;
						}
					}
					ch = read();
				} else {
					throw new IOException();
				}
			} else {
				bout.write(ch);
				ch = read();
			}
		}
	}

	public Enumeration getHeaders(){
		return headers.keys();
	}

	public String getHeader(String name){
		return (String)headers.get(name);
	}
}
