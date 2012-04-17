/*
 * @(#)AbstractLineInputStream.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.nio;

import org.pnuts.text.*;
import java.io.*;

/**
 * Abstract base class of LineInputStream.
 */
public abstract class AbstractLineInputStream implements LineProcessor {

	protected static int defaultBufferSize = 8192;

	protected byte[] bb;
	protected int size;
	protected int startChar;

	protected boolean stopped = false;

	/**
	 * Constructor
	 *
	 * @param  sz   Input-buffer size
	 * @exception  IllegalArgumentException  If sz is <= 0
	 */
	protected AbstractLineInputStream(int sz) {
		if (sz <= 0){
			throw new IllegalArgumentException();
		}
		this.bb = new byte[sz];
	}

	/**
	 * Constructor
	 */
	protected AbstractLineInputStream() {
		this(defaultBufferSize);
	}

	/*
	 * Defines how to fetch data
	 */
	protected abstract int fill(byte[] c, int offset, int size) throws IOException;

	/*
	 * Defines how to process a line
	 */
	protected void process(byte[] c, int offset, int length){
	}

	boolean fillBuffer() throws IOException {
		int n = 0;
		int offset = size - startChar;
		if (offset < 0){
			return false;
		}
		do {
			if (startChar > 0 && startChar < bb.length){
				System.arraycopy(bb, startChar, bb, 0, offset);
			} else if (offset == bb.length){
				byte[] newarray = new byte[bb.length * 2];
				System.arraycopy(bb, startChar, newarray, 0, offset);
				bb = newarray;
			}
			n = fill(bb, offset, bb.length - offset);
		} while (n == 0);
		if (n > 0) {
			this.size = n + offset;
		}
		return n > 0;
	}

	/**
	 * Reads one line.
	 */
	public synchronized boolean processLine(boolean includeNewLine) throws IOException {

		int n = 0;
		int startChar = this.startChar;

		if (this.size < 1){
			fillBuffer();
		}

		for(;;){
			boolean eol = false;
			boolean cr = false;
			byte b = 0;
			int i, e;
			int size = this.size;
		
			for (i = e = startChar + n; i < size; ) {
				b = bb[i++];
				n++;
				if (b == '\n'){
					eol = true;
					if (includeNewLine){
						e++;
					}
					break;
				} else if (b == '\r'){
					eol = true;
					if (cr){
						n--;
						break;
					}
					if (includeNewLine){
						e++;
					}
					cr = true;
					if (i == size){
						if (fillBuffer()){
							e -= startChar;
							this.startChar = startChar = 0;
							size = this.size;
							i = n;
						} else {
							if (i > startChar){
								process(bb, startChar, e - startChar);
							}
							return false;
						}
					}
				} else {
					if (cr){
						n--;
						break;
					} else {
						e++;
					}
					cr = false;
				}
			}
			if (eol) {
				if (e >= startChar){
					process(bb, startChar, e - startChar);
				}
				this.startChar += n;
				return true;
			}
			if (fillBuffer()){
				e -= startChar;
				this.startChar = startChar = 0;
				size = this.size;
				i = n;
			} else {
				if (e > startChar){
					process(bb, startChar, e - startChar);
				}
				return false;
			}
		}
	}

	/**
	 * If this method is called during the processAll() call, the processing
	 * will be interrupted.
	 */
	public void stop(){
		stopped = true;
	}

	/**
	 * Process all lines.
	 * The line string does not include the newline character at the end of lines.
	 *
	 * @return the number of lines processed
	 */
	public int processAll() throws IOException {
		return processAll(false);
	}


	/**
	 * Process all lines.
	 *
	 * @param includeNewLine if true newline code (\r|\n|\r\n) is appended.
	 * @return the number of lines processed
	 */
	public int processAll(boolean includeNewLine) throws IOException {
		int count = 0;
		while (!stopped && processLine(includeNewLine)){
			count++;
		}
		return count;
	}
}
