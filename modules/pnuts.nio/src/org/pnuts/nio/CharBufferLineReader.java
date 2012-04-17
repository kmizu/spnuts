/*
 * @(#)CharBufferLineReader.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.nio;

import java.io.*;
import java.nio.*;
import pnuts.lang.*;
import pnuts.lang.Package;
import org.pnuts.text.*;

class CharBufferLineReader extends AbstractLineReader {

	protected CharBuffer cbuf;
	protected LineHandler handler;

	final static int defaultBufferSize = 8192;

	/**
	 * Constructor 
	 *
	 * @param  cbuf A CharBuffer
	 * @param  sz   Buffer size
	 *
	 * @exception  IllegalArgumentException  If sz is <= 0
	 */
	public CharBufferLineReader(CharBuffer cbuf, int sz){
		super(sz);
		this.cbuf = cbuf;
	}

	/**
	 * Constructor 
	 *
	 * @param  cbuf A CharBuffer
	 */
	public CharBufferLineReader(CharBuffer cbuf){
		this(cbuf, defaultBufferSize);
	}

	/**
	 * Constructor 
	 */
	protected CharBufferLineReader(){
	}

	protected int fill(char[] c, int offset, int size) throws IOException {
		int rem = cbuf.remaining();
		if (rem <= 0){
			return -1;
		}
		if (size > rem){
			size = rem;
		}
		cbuf.get(c, offset, size);
		return size;
	}

	public void setLineHandler(LineHandler handler){
		this.handler = handler;
	}

	public LineHandler getLineHandler(){
		return this.handler;
	}

	protected void process(char[] c, int offset, int length){
		handler.process(c, offset, length);
	}
}
