/*
 * @(#)MultipartInputStream.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import java.io.*;

/**
 * Handles multipart MIME messages defined in rfc1521.
 */
public class MultipartInputStream extends FilterInputStream {

	private byte[] boundary;
	private byte[] buffer;
	private boolean partEnd;
	private boolean fileEnd;

	protected MultipartInputStream(InputStream in) {
		super(in);
	}

	public MultipartInputStream(InputStream in, byte[] boundary) {
		super(in);
		if (!in.markSupported()){
			this.in = new BufferedInputStream(in, boundary.length + 4);
		}
		this.boundary = boundary;
		this.buffer = new byte[boundary.length];
		this.partEnd = false;
		this.fileEnd = false;
	}

	private final boolean readBoundaryBytes() throws IOException {
		int pos = 0;
		while (pos < buffer.length) {
			int got = in.read(buffer, pos, buffer.length - pos);
			if (got < 0) {
				return false;
			}
			pos += got;
		}
		return true;
	}

	protected boolean skipToBoundary() throws IOException {
		int ch = in.read();
	skip:
		while (ch != -1) {
			if (ch != '-') {
				ch = in.read();
				continue;
			}
			if ((ch = in.read()) != '-') {
				continue;
			}
			in.mark(boundary.length);
			if (!readBoundaryBytes()) {
				in.reset();
				ch = in.read();
				continue skip;
			}
			for (int i = 0; i < boundary.length; i++) {
				if (buffer[i] != boundary[i]) {
					in.reset();
					ch = in.read();
					continue skip;
				}
			}
			if ((ch = in.read()) == '\r') {
				ch = in.read();
			} 
			in.mark(3);
			if (in.read() == '-') {
				if (in.read() == '\r' && in.read() == '\n'){
					fileEnd = true;
					return false;
				}
			}
			in.reset();
			return true;
		}
		fileEnd = true;
		return false;
	}

	public int read() throws IOException {
		int ch;
		if (partEnd){
			return -1;
		}
		switch (ch = in.read()) {
		case '\r':
			in.mark(boundary.length + 3);
			int c1 = in.read();
			int c2 = in.read();
			int c3 = in.read();
			if ((c1 == '\n') && (c2 == '-') && (c3 == '-')) {
				if (!readBoundaryBytes()) {
					reset();
					return ch;
				}
				for (int i = 0; i < boundary.length; i++) {
					if (buffer[i] != boundary[i]) {
						in.reset();
						return ch;
					}
				}
				partEnd = true;
				if ((ch = in.read()) == '\r') {
					in.read();
				} else if (ch == '-') {
					if (in.read() == '-'){
						fileEnd = true;
					}
				} else {
					fileEnd = (ch == -1);
				}
				return -1;
			} else {
				in.reset();
				return ch;
			}
		case -1:
			fileEnd = true;
			return -1;
		default:
			return ch;
		}
	}

	public int read (byte b[], int off, int len) throws IOException {
		int got = 0;
		int ch;

		while ( got < len ) {
			if ((ch = read()) == -1){
				return (got == 0) ? -1 : got;
			}
			b[off + (got++)] = (byte)ch;
		}
		return got;
	}

	public long skip (long n) throws IOException {
		while ((--n >= 0) && (read() != -1)){
		}
		return n;
	}

	public boolean next() throws IOException {
		if (fileEnd) {
			return false;
		}
		if (!partEnd) { 
			return skipToBoundary();
		} else {
			partEnd = false;
			return true;
		}
	}
}
