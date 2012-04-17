/*
 * @(#)ByteBuffer.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.IOException;
import java.io.OutputStream;

public class ByteBuffer {
	byte[] buffer;

	int size = 0;

	public ByteBuffer() {
		this(64);
	}

	public ByteBuffer(int size) {
		buffer = new byte[size];
	}

	public void add(byte b) {
		if (size + 1 > buffer.length) {
			byte[] newBuf = new byte[(size + 1) * 2];
			System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
			buffer = newBuf;
		}
		buffer[size++] = b;
	}

	public void add(short s) {
		set(s, size);
	}

	public void set(short s, int offset) {
		if (offset + 2 > buffer.length) {
			byte[] newBuf = new byte[(offset + 2) * 2];
			System.arraycopy(buffer, 0, newBuf, 0, size);
			buffer = newBuf;
		}
		buffer[offset] = (byte) (s >> 8);
		buffer[offset + 1] = (byte) s;
		if (offset + 2 > size) {
			size = offset + 2;
		}
	}

	public void add(int s) {
		set(s, size);
	}

	public void set(int s, int offset) {
		if (offset + 4 > buffer.length) {
			byte[] newBuf = new byte[(offset + 4) * 2];
			System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
			buffer = newBuf;
		}
		buffer[offset] = (byte) (s >> 24);
		buffer[offset + 1] = (byte) (s >> 16);
		buffer[offset + 2] = (byte) (s >> 8);
		buffer[offset + 3] = (byte) s;
		if (offset + 4 > size) {
			size = offset + 4;
		}
	}

	public void add(long l) {
		if (size + 8 > buffer.length) {
			byte[] newBuf = new byte[(size + 8) * 2];
			System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
			buffer = newBuf;
		}
		buffer[size++] = (byte) (l >> 56);
		buffer[size++] = (byte) (l >> 48);
		buffer[size++] = (byte) (l >> 40);
		buffer[size++] = (byte) (l >> 32);
		buffer[size++] = (byte) (l >> 24);
		buffer[size++] = (byte) (l >> 16);
		buffer[size++] = (byte) (l >> 8);
		buffer[size++] = (byte) l;
	}

	public void add(byte[] b, int offset, int len) {
		add(b, offset, len, size);
	}

	public void add(byte[] b, int offset, int len, int dst_offset) {
		if (dst_offset + len > buffer.length) {
			byte[] newBuf = new byte[(dst_offset + 1) * 2 + len];
			System.arraycopy(buffer, 0, newBuf, 0, size);
			buffer = newBuf;
		}
		System.arraycopy(b, offset, buffer, dst_offset, len);
		if (dst_offset + len > size) {
			size = dst_offset + len;
		}
	}

	public void prepend(ByteBuffer buf) {
		int len = buf.size();
		/***/
		if (len + size > buffer.length) {
			byte[] newBuf = new byte[len * 2 + size];
			System.arraycopy(buffer, 0, newBuf, len, size);
			System.arraycopy(buf.buffer, 0, newBuf, 0, len);
			buffer = newBuf;
		} else {
			System.arraycopy(buffer, 0, buffer, len, size);
			System.arraycopy(buf.buffer, 0, buffer, 0, len);
		}
		/***/
		size += len;
	}

	public void append(ByteBuffer buf) {
		add(buf.buffer, 0, buf.size());
	}

	public void set(byte b, int offset) {
		buffer[offset] = b;
	}

	public void copyTo(int offset, byte[] dest, int off, int len) {
		System.arraycopy(buffer, offset, dest, off, len);
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int size() {
		return size;
	}

	public void writeTo(OutputStream out) throws IOException {
		out.write(buffer, 0, size);
	}
}
