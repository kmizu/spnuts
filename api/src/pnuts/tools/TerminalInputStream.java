/*
 * @(#)TerminalInputStream.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream of Terminal. To avoid blocking it runs background. When the Enter
 * key is pressed or EOF is read the current line is sent to the InputStream.
 * 
 * @version 1.1
 * @author Toyokazu Tomatsu
 */
class TerminalInputStream extends FilterInputStream implements Runnable {
	private Thread runner;
	private byte result[];
	private int reslen;
	private boolean EOF;
	private IOException IOError;
	private boolean enterred = false;

	TerminalInputStream(InputStream in, int bufsize) {
		super(in);
		result = new byte[bufsize];
		reslen = 0;
		EOF = false;
		IOError = null;
		runner = new Thread(this, "Terminal");
		runner.setPriority(2);
		runner.setDaemon(true);
		runner.start();
	}

	TerminalInputStream(InputStream in) {
		this(in, 1024);
	}

	public void setPriority(int prio) {
		runner.setPriority(prio);
	}

	public synchronized int read() throws IOException {
		while (reslen == 0 || !enterred) {
			try {
				if (EOF) {
					return -1;
				}
				if (IOError != null) {
					throw IOError;
				}
				wait();
			} catch (InterruptedException e) {
			}
		}
		return (int) getChar();
	}

	public synchronized int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	public synchronized int read(byte b[], int off, int len) throws IOException {
		while (reslen == 0 || !enterred) {
			try {
				if (EOF)
					return -1;
				if (IOError != null)
					throw IOError;
				wait();
			} catch (InterruptedException e) {
			}
		}
		int sizeread = Math.min(reslen, len);
		byte c[] = getChars(sizeread);
		System.arraycopy(c, 0, b, off, sizeread);
		return sizeread;
	}

	public synchronized long skip(long n) throws IOException {
		int sizeskip = Math.min(reslen, (int) n);
		if (sizeskip > 0) {
			getChars(sizeskip);
		}
		return ((long) sizeskip);
	}

	public synchronized int available() throws IOException {
		return reslen;
	}

	public synchronized void close() throws IOException {
		reslen = 0;
		in.close();
		EOF = true;
		notifyAll();
	}

	public synchronized void reset() throws IOException {
		super.reset();
		reslen = 0;
		enterred = false;
	}

	public boolean markSupported() {
		return false;
	}

	public void run() {
		try {
			while (true) {
				int c = in.read();
				synchronized (this) {
					if ((c == -1) || EOF) {
						EOF = true;
						enterred = true;
						notifyAll();
						return;
					} else {
						putChar((byte) c);
					}
				}
			}
		} catch (IOException e) {
			synchronized (this) {
				IOError = e;
			}
			return;
		} finally {
			synchronized (this) {
				notifyAll();
			}
		}
	}

	private synchronized void putChar(byte c) {
		if (c == 10 || c == 13) {
			enterred = true;
		}
		if (reslen >= result.length) {
			byte[] result2 = new byte[result.length * 2];
			System.arraycopy(result, 0, result2, 0, result.length);
			result = result2;
		}
		result[reslen++] = c;
		if (enterred) {
			notifyAll();
		}
	}

	private synchronized byte getChar() {
		byte c = result[0];
		if (--reslen > 0) {
			System.arraycopy(result, 1, result, 0, reslen);
		} else {
			enterred = false;
		}
		return c;
	}

	private synchronized byte[] getChars(int chars) {
		byte c[] = new byte[chars];
		System.arraycopy(result, 0, c, 0, chars);
		reslen -= chars;
		if (reslen > 0) {
			System.arraycopy(result, chars, result, 0, reslen);
		} else {
			enterred = false;
		}
		return c;
	}
}