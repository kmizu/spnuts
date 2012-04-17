/*
 * @(#)ByteBufferLineInputStream.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.nio;

import org.pnuts.text.*;
import java.io.*;
import java.nio.*;

public class ByteBufferLineInputStream extends AbstractLineInputStream {

	protected ByteBuffer bbuf;
	protected LineHandler handler;
	protected boolean needToClose;

	final static int defaultBufferSize = 8192;

	public ByteBufferLineInputStream(ByteBuffer bbuf, LineHandler handler, boolean needToClose){
		this(bbuf, defaultBufferSize, handler, needToClose);
	}
	/**
	 * Constructor 
	 *
	 * @param  cbuf A ByteBuffer
	 * @param  sz   Buffer size
	 *
	 * @exception  IllegalArgumentException  If sz is <= 0
	 */
	public ByteBufferLineInputStream(ByteBuffer bbuf, int sz, LineHandler handler, boolean needToClose){
		super(sz);
		this.bbuf = bbuf;
		this.handler = handler;
		this.needToClose = needToClose;
	}

	/**
	 * Constructor 
	 */
	protected ByteBufferLineInputStream(){
	}

	protected int fill(byte[] b, int offset, int size) throws IOException {
		int rem = bbuf.remaining();
		if (rem <= 0){
			return -1;
		}
		if (size > rem){
			size = rem;
		}
		bbuf.get(b, offset, size);
		return size;
	}

	protected void process(byte[] b, int offset, int length){
		handler.process(b, offset, length);
	}
}
