/*
 * @(#)LogReader.java 1.2 04/12/06
 * 
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.FileWriter;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

class LogReader extends FilterReader {
	FileWriter writer;

	LogReader(Reader in, String path) throws IOException {
		super(in);
		this.writer = new FileWriter(path);

		java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					writer.flush();
					writer.close();
				} catch (IOException e) {
				}
			}
		});
	}

	public int read() throws IOException {
		int c = super.read();
		writer.write(c);
		writer.flush();
		return c;
	}

	public int read(char cbuf[], int off, int len) throws IOException {
		int n = super.read(cbuf, off, len);
		if (n > 0) {
			writer.write(cbuf, off, n);
			writer.flush();
		}
		return n;
	}

	public void close() throws IOException {
		super.close();
		writer.close();
	}
}