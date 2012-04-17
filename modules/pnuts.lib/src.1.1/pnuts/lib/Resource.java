/*
 * @(#)Resource.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib;

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;

abstract class Resource {
	public abstract URL getURL();
	public abstract InputStream getInputStream() throws IOException;
	public abstract int getContentLength() throws IOException;

	public byte[] getBytes() throws IOException {
		byte[] b;
		InputStream in = getInputStream();
		int len = getContentLength();
		try {
			if (len != -1) {
				b = new byte[len];
				while (len > 0) {
					int n = in.read(b, b.length - len, len);
					if (n == -1) {
						throw new IOException("unexpected EOF");
					}
					len -= n;
				}
			} else {
				b = new byte[1024];
				int total = 0;
				while ((len = in.read(b, total, b.length - total)) != -1) {
					total += len;
					if (total >= b.length) {
						byte[] tmp = new byte[total * 2];
						System.arraycopy(b, 0, tmp, 0, total);
						b = tmp;
					}
				}
				if (total != b.length) {
					byte[] tmp = new byte[total];
					System.arraycopy(b, 0, tmp, 0, total);
					b = tmp;
				}
			}
		} finally {
			in.close();
		}
		return b;
	}
}
