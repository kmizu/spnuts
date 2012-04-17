/*
 * @(#)CharBufferReader.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.nio;

import java.io.*;
import java.nio.*;

public class CharBufferReader extends Reader {

	private CharBuffer cbuf;
	private int length;
	private int next = 0;
	private int mark = 0;

	public CharBufferReader(CharBuffer cbuf){
		this.cbuf = cbuf;
		this.length = cbuf.length();
	}

	private void ensureOpen() throws IOException {
		if (cbuf == null){
			throw new IOException("Stream closed");
		}
	}

	public int read() throws IOException {
		synchronized (lock) {
			ensureOpen();
			if (next >= length){
				return -1;
			}
			return cbuf.get(next++);
		}
	}

	public int read(char ca[], int off, int len) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if ((off < 0) || (off > ca.length) || (len < 0) ||
				((off + len) > ca.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return 0;
			}
			if (next >= length)
				return -1;
			int n = Math.min(length - next, len);
			cbuf.get(ca, off, n);
			next += n;
			return n;
		}
	}

	public long skip(long ns) throws IOException {
		synchronized (lock) {
			ensureOpen();
			if (next >= length){
				return 0;
			}
			long n = Math.min(length - next, ns);
			next += n;
			return n;
		}
	}

	public boolean ready() throws IOException {
		synchronized (lock) {
			ensureOpen();
			return true;
		}
	}
	
	public boolean markSupported() {
		return true;
	}

	public void mark(int readAheadLimit) throws IOException {
		if (readAheadLimit < 0){
			throw new IllegalArgumentException("Read-ahead limit < 0");
		}
		synchronized (lock) {
			ensureOpen();
			mark = next;
		}
	}

	public void reset() throws IOException {
		synchronized (lock) {
			ensureOpen();
			next = mark;
		}
	}

	public void close() {
		this.cbuf = null;
	}
}
