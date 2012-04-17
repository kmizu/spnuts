/*
 * @(#)Console.java 1.4 05/05/16
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * General purpose Console
 */
public class Console {
	private final static int DEFAULT_LINE_BUFFER_SIZE = 40;
	private ConsoleBuffer out;
	private PipedReader in;
	private PipedWriter pipe;
	protected ConsoleUI ui;

	/**
	 * Constructor
	 */
	public Console() {
		out = new ConsoleBuffer(DEFAULT_LINE_BUFFER_SIZE);
		pipe = new PipedWriter();
		in = new PipedReader();
		try {
			pipe.connect(in);
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}


	/**
	 * Sets the UI object of this console
	 */
	public void setConsoleUI(ConsoleUI ui){
		this.ui = ui;
	}

	/**
	 * Gets the UI object of this console
	 */
	public ConsoleUI getConsoleUI(){
		return ui;
	}
        
	/**
	 * Gets the Reader from this console
	 */	
	public Reader getReader() {
		return in;
	}
	
	/**
	 * Gets the OutputStream from this console
	 */	
	public Writer getWriter() {
		return out;
	}

	/**
	 * Sends the specified string to the scripting engine
	 */
	public void enter(String str) throws IOException {
		char[] carray = str.toCharArray();
		enter(carray, 0, carray.length);
	}

	/**
	 * Sends the specified characters to the scripting engine
	 */
	public void enter(char[] cbuf, int offset, int size) throws IOException {
		pipe.write(cbuf, offset, size);
		pipe.write("\n");
		pipe.flush();
	}

	class ConsoleBuffer extends Writer {
		private StringBuffer buf = new StringBuffer();
		private int lineBufferSize;
		private int pendingLines = 0;

		ConsoleBuffer(){
			this(0);
		}

		ConsoleBuffer(int lineBufferSize){
			this.lineBufferSize = lineBufferSize;
		}
		
		public synchronized void write(int ch) {
			buf.append((char)ch);
			if (lineBufferSize > 0 && ch == '\n') {
				if (++pendingLines > lineBufferSize){
					flushBuffer();
					pendingLines = 0;
				}
			}
		}
		
		public synchronized void write(char[] data, int off, int len) {
			for (int i = off; i < len; i++) {
				buf.append(data[i]);
				if (lineBufferSize > 0 && data[i] == '\n') {
					if (++pendingLines > lineBufferSize){
						flushBuffer();
						pendingLines = 0;
					}
				}
			}
		}

		public synchronized void flush() throws IOException {
			if (buf.length() > 0) {
				flushBuffer();
			}
		}
	
		public void close() throws IOException {
			flush();
		}

		public int size(){
			return buf.length();
		}
	
		private void flushBuffer() {
			final String str = buf.toString();
			buf.setLength(0);
			if (ui != null){
			    ui.append(str);
			}
		}
	}
}
