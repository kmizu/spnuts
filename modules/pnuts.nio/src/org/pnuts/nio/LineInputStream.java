/*
 * @(#)LineInputStream.java 1.2 04/12/06
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
 * This class is used to read lines from a character stream.
 */
public class LineInputStream extends AbstractLineInputStream {

	protected LineHandler handler;
	protected InputStream input;
	protected boolean needToClose;

	public LineInputStream(InputStream input, LineHandler handler, boolean needToClose){
		this(input, defaultBufferSize, handler, needToClose);
	}

	public LineInputStream(InputStream input, int sz, LineHandler handler, boolean needToClose){
		super(sz);
		this.input = input;
		this.handler = handler;
		this.needToClose = needToClose;
	}

	/**
	 * Fills the buffer.
	 * This method is called when LineInputStream  needs more data.
	 */
	protected int fill(byte[] b, int offset, int size) throws IOException {
		return input.read(b, offset, size);
	}

	/**
	 * Process a line.
	 *
	 * @param c the char buffer that contains the current line.
	 * @param offset the offset of the buffer
	 * @param length the length of the current line
	 */
	protected void process(byte[] b, int offset, int length){
		handler.process(b, offset, length);
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
