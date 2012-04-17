/*
 * @(#)Base64Encoder.java 1.1 05/01/20
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

public class Base64Encoder {
	private static final int BUFFER_SIZE = 4096;
	private static final char[] table = {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
		'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/', '='
	};

	public Base64Encoder() {
	}

	private final int get1(byte buf[], int offset) {
		return (buf[offset] & 0xfc) >> 2;
	}

	private final int get2(byte buf[], int offset) {
		return ((buf[offset] & 0x3) << 4) | ((buf[offset + 1] & 0xf0) >>> 4);
	}

	private final int get3(byte buf[], int offset) {
		return ((buf[offset + 1] & 0x0f) << 2) | ((buf[offset + 2] & 0xc0) >>> 6);
	}

	private static final int get4(byte buf[], int offset) {
		return buf[offset + 2] & 0x3f;
	}

	public void encode(InputStream in, OutputStream out) throws IOException {
		byte buffer[] = new byte[BUFFER_SIZE];
		int nread = -1;
		int offset = 0;
		int count = 0;
		while ((nread = in.read(buffer, offset, BUFFER_SIZE - offset)) > 0) {
			if ((nread + offset) >= 3) {
				nread += offset;
				offset = 0;
				while (offset + 3 <= nread) {
					int c1 = get1(buffer, offset);
					int c2 = get2(buffer, offset);
					int c3 = get3(buffer, offset);
					int c4 = get4(buffer, offset);
					switch (count) {
						case 73 :
							out.write(table[c1]);
							out.write(table[c2]);
							out.write(table[c3]);
							out.write('\n');
							out.write(table[c4]);
							count = 1;
							break;
						case 74 :
							out.write(table[c1]);
							out.write(table[c2]);
							out.write('\n');
							out.write(table[c3]);
							out.write(table[c4]);
							count = 2;
							break;
						case 75 :
							out.write(table[c1]);
							out.write('\n');
							out.write(table[c2]);
							out.write(table[c3]);
							out.write(table[c4]);
							count = 3;
							break;
						case 76 :
							out.write('\n');
							out.write(table[c1]);
							out.write(table[c2]);
							out.write(table[c3]);
							out.write(table[c4]);
							count = 4;
							break;
						default :
							out.write(table[c1]);
							out.write(table[c2]);
							out.write(table[c3]);
							out.write(table[c4]);
							count += 4;
							break;
					}
					offset += 3;
				}
				for (int i = 0; i < 3; i++){
					buffer[i] = (i < nread - offset) ? buffer[offset + i] : ((byte) 0);
				}
				offset = nread - offset;
			} else {
				offset += nread;
			}
		}
		switch (offset) {
			case 1 :
				out.write(table[get1(buffer, 0)]);
				out.write(table[get2(buffer, 0)]);
				out.write('=');
				out.write('=');
				break;
			case 2 :
				out.write(table[get1(buffer, 0)]);
				out.write(table[get2(buffer, 0)]);
				out.write(table[get3(buffer, 0)]);
				out.write('=');
		}
		return;
	}

	public byte[] encode(byte[] bytes) throws IOException {
		InputStream in = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		encode(in, out);
		return out.toByteArray();
	}

	public String encode(String input) throws IOException {
		int len = input.length();
		byte bytes[] = new byte[len];
		for (int i = 0; i < len; i++){
			bytes[i] = (byte)input.charAt(i);
		}
		InputStream in = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		encode(in, out);
		return out.toString();
	}
}
