/*
 * @(#)ResponseWriter.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import pnuts.lang.*;
import java.io.*;
import javax.servlet.*;

class ResponseWriter extends Writer {

	private ServletResponse response;
	private Context context;
	private Writer writer;
	private ByteArrayOutputStream bout;
	private boolean buffering;

	public ResponseWriter(ServletResponse response, Context context, boolean buffering){
		this.response = response;
		this.context = context;
		this.buffering = buffering;
	}

	private Writer getWriter() throws IOException {
		Writer w = (Writer)context.get(PnutsServlet.SERVLET_WRITER);
		if (w == null){
			if (buffering){
				this.bout = new ByteArrayOutputStream();
				w = new BufferedWriter(new OutputStreamWriter(this.bout, response.getCharacterEncoding()));
			} else {
				w = response.getWriter();
			}
			context.set(PnutsServlet.SERVLET_WRITER, w);
		}
		return w;
	}

	public void write(int c) throws IOException {
		if (writer == null){
			writer = getWriter();
		}
		writer.write(c);
	}

	public void write(char cbuf[]) throws IOException {
		if (writer == null){
			writer = getWriter();
		}
		writer.write(cbuf);
	}

	public void write(char cbuf[], int off, int len) throws IOException {
		if (writer == null){
			writer = getWriter();
		}
		writer.write(cbuf, off, len);
	}

	public void write(String str) throws IOException {
		if (writer == null){
			writer = getWriter();
		}
		writer.write(str);
	}

	public void write(String str, int off, int len) throws IOException {
		if (writer == null){
			writer = getWriter();
		}
		writer.write(str, off, len);
	}

	public void flush() throws IOException {
		if (writer != null){
			writer.flush();
		}
	}

	public void close() throws IOException {
		if (writer != null){
			writer.close();
		}
	}

	public void flushBuffer() throws IOException {
		if (buffering && bout != null){
			int size = bout.size();
			if (size > 0){
				response.setContentLength(bout.size());
				OutputStream out = response.getOutputStream();
				bout.writeTo(out);
				bout.reset();
			}
		}
	}
}
