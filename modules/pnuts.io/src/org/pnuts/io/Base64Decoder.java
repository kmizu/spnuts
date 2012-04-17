/*
 * @(#)Base64Decoder.java 1.1 05/01/20
 *
 * Copyright (c) 2004,2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Base64Decoder {
	private static final int BUFFER_SIZE = 4096;
	private static byte[] table = new byte[255];

	static {
		for (int i = 0; i < 255; i++) {
			table[i] = (byte) -1;
		}
		for (int i = 'Z'; i >= 'A'; i--) {
			table[i] = (byte) (i - 'A');
		}
		for (int i = 'z'; i >= 'a'; i--) {
			table[i] = (byte) (i - 'a' + 26);
		}
		for (int i = '9'; i >= '0'; i--) {
			table[i] = (byte) (i - '0' + 52);
		}
		table['='] = 65;
		table['+'] = 62;
		table['/'] = 63;
	}

	public Base64Decoder() {
	}

	private final int get1(byte buf[], int offset) {
		return ((buf[offset] & 0x3f) << 2) | ((buf[offset + 1] & 0x30) >>> 4);
	}

	private final int get2(byte buf[], int offset) {
		return ((buf[offset + 1] & 0x0f) << 4) | ((buf[offset + 2] & 0x3c) >>> 2);
	}

	private final int get3(byte buf[], int offset) {
		return ((buf[offset + 2] & 0x03) << 6) | (buf[offset + 3] & 0x3f);
	}

	public void decode(InputStream in, OutputStream out) throws IOException {
		byte buffer[] = new byte[BUFFER_SIZE];
		byte chunk[] = new byte[4];
		int nread = -1;
		int ready = 0;

	fill :
		while ((nread = in.read(buffer)) > 0) {
			int skiped = 0;
			while (skiped < nread) {
				while (ready < 4) {
					if (skiped >= nread){
						continue fill;
					}
					int ch = table[buffer[skiped++]];
					if (ch >= 0){
						chunk[ready++] = (byte) ch;
					}
				}
				if (chunk[2] == 65) {
					out.write(get1(chunk, 0));
					return;
				} else if (chunk[3] == 65) {
					out.write(get1(chunk, 0));
					out.write(get2(chunk, 0));
					return;
				} else {
					out.write(get1(chunk, 0));
					out.write(get2(chunk, 0));
					out.write(get3(chunk, 0));
				}
				ready = 0;
			}
		}
		if (ready != 0){
			throw new IOException("Invalid length.");
		}
		out.flush();
	}

	public String decode(String input) throws IOException {
		int len = input.length();
		byte bytes[] = new byte[len];
		for (int i = 0; i < len; i++){
			bytes[i] = (byte)input.charAt(i);
		}
		InputStream in = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		decode(in, bout);
		return bout.toString();
	}

	public byte[] decode(byte[] bytes) throws IOException {
		InputStream in = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		decode(in, bout);
		return bout.toByteArray();
	}
}
