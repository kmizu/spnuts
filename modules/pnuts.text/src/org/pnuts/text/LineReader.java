/*
 * @(#)LineReader.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import java.io.*;

/**
 * This class is used to read lines from a character stream.
 * Unlike BufferedReader.readLine(), this class allows you to
 * process lines without instantiating a String object for each
 * line.
 */
public class LineReader extends AbstractLineReader {

	protected LineHandler handler;
	protected Reader input;
	protected boolean needToClose;

	public LineReader(Reader input, int sz, LineHandler handler, boolean needToClose){
		super(sz);
		this.input = input;
		this.handler = handler;
		this.needToClose = needToClose;
	}

	public LineReader(Reader input, LineHandler handler, boolean needToClose){
		this(input, defaultCharBufferSize, handler, needToClose);
	}

	/**
	 * Fills the buffer.
	 * This method is called when LineReader needs more data.
	 */
	protected int fill(char[] c, int offset, int size) throws IOException {
		return input.read(c, offset, size);
	}

	/**
	 * Process a line.
	 *
	 * @param c the char buffer that contains the current line.
	 * @param offset the offset of the buffer
	 * @param length the length of the current line
	 */
	protected void process(char[] c, int offset, int length){
		handler.process(c, offset, length);
	}

	/**
	 * Process all lines.
	 *
	 * @param includeNewLine if true newline code (\r|\n|\r\n) is appended.
	 * @return the number of lines processed
	 */
	public int processAll(boolean includeNewLine) throws IOException {
		int count = 0;
		try {
			while (!stopped && processLine(includeNewLine)){
				count++;
			}
			return count;
		} finally {
			if (needToClose){
				input.close();
			}
		}
	}
}
