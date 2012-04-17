/*
 * @(#)ByteBufferInputStream.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.nio;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;

public class ByteBufferInputStream extends InputStream {
	private ByteBuffer buffer;
	private int mark;
	private boolean eof;

	public ByteBufferInputStream(ByteBuffer buf){
		this.buffer = buf;
	}

	public int read() throws IOException {
		if (eof){
			throw new EOFException();
		}
		try {
			return (int)buffer.get();
		} catch (BufferUnderflowException e){
			eof = true;
			return -1;
		}
	}

	public int read(byte[] b, int offset, int size) throws IOException {
		if (eof){
			throw new EOFException();
		}
		int oldPosition = buffer.position();
		int limit = buffer.limit();
		if (limit == oldPosition){
			eof = true;
			return -1;
		}
		if (size > limit - oldPosition){
			size = limit - oldPosition;
		}
		try {
			buffer.get(b, offset, size);
			return buffer.position() - oldPosition;
		} catch (IndexOutOfBoundsException e1){
			throw new IOException(e1.getMessage());
		} catch (BufferUnderflowException e2){
			throw new IOException(e2.getMessage());
		}
	}

	public long skip(long n) throws IOException {
		if (eof){
			throw new EOFException();
		}
		int oldPosition = buffer.position();
		int limit = buffer.limit();
		long newPosition = oldPosition + n;
		if (newPosition > limit){
			newPosition = limit;
		}
		buffer.position((int)newPosition);
		return newPosition - oldPosition;
	}

	public int available() throws IOException {
		return buffer.remaining();
	}

	public void close() throws IOException {
	}

	public void mark(int readlimit) {
		mark = buffer.position();
	}

	public void reset() throws IOException {
		buffer.position(mark);
	}

	public boolean markSupported() {
		return true;
	}
}
