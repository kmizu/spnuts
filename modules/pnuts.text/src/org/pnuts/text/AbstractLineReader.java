/*
 * @(#)AbstractLineReader.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import java.io.*;

/**
 * Abstract base class of LineReader.
 */
public abstract class AbstractLineReader implements LineProcessor {

	protected static int defaultCharBufferSize = 8192;

	protected char[] cb;
	protected int size;
	protected int startChar;

	protected boolean stopped = false;

	/**
	 * Constructor
	 *
	 * @param  sz   Input-buffer size
	 * @exception  IllegalArgumentException  If sz is <= 0
	 */
	protected AbstractLineReader(int sz) {
		if (sz <= 0){
			throw new IllegalArgumentException();
		}
		this.cb = new char[sz];
	}

	/**
	 * Constructor
	 */
	protected AbstractLineReader() {
		this(defaultCharBufferSize);
	}

	/*
	 * Defines how to fetch data
	 */
	protected abstract int fill(char[] c, int offset, int size) throws IOException;

	/*
	 * Defines how to process a line
	 */
	protected void process(char[] c, int offset, int length){
	}

	boolean fillBuffer() throws IOException {
		int n = 0;
		int offset = size - startChar;
		if (offset < 0){
			return false;
		}
		do {
			if (startChar > 0 && startChar < cb.length){
				System.arraycopy(cb, startChar, cb, 0, offset);
			} else if (offset == cb.length){
				char[] newarray = new char[cb.length * 2];
				System.arraycopy(cb, startChar, newarray, 0, offset);
				cb = newarray;
			}
			n = fill(cb, offset, cb.length - offset);
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
			char c = 0;
			int i, e;
			int size = this.size;
		
			for (i = e = startChar + n; i < size; ) {
				c = cb[i++];
				n++;
				if (c == '\n'){
					eol = true;
					if (includeNewLine){
						e++;
					}
					break;
				} else if (c == '\r'){
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
								process(cb, startChar, e - startChar);
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
					process(cb, startChar, e - startChar);
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
					process(cb, startChar, e - startChar);
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
