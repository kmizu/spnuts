/*
 * @(#)read.java 1.3 05/06/14
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import pnuts.io.CharacterEncoding;
import pnuts.lang.*;
import java.io.*;
import java.net.*;
import org.pnuts.lib.PathHelper;

public class read extends PnutsFunction {

	private final static Object DEFAULT_STREAM = new Object();

	public read(){
		super("read");
	}

	public boolean defined(int narg){
		return (narg == 1 || narg == 2);
	}

	protected Object exec(Object[] args, Context context){
		Object a1 = null;
		Object a2 = null;
		int nargs = args.length;
		if (nargs == 1){
			a1 = args[0];
			a2 = DEFAULT_STREAM;
		} else if (nargs == 2){
			a1 = args[0];
			a2 = args[1];
		} else {
			undefined(args, context);
		}
		int nread = 0;
		InputStream in = null;
		OutputStream out = null;
		Reader reader = null;
		Writer writer = null;
		InputStream inputStreamToClose = null;
		OutputStream outputStreamToClose = null;
		boolean a1isInputStream = false;
		boolean a1isReader = false;
		try {
			if (a1 instanceof InputStream){
				a1isInputStream = true;
				in = (InputStream)a1;
			} else if (a1 instanceof File){
				a1isInputStream = true;
				in = inputStreamToClose = new FileInputStream((File)a1);
			} else if (a1 instanceof String){
				a1isInputStream = true;
				in = new FileInputStream(PathHelper.getFile((String)a1, context));;
				inputStreamToClose = in;
			} else if (a1 instanceof URL){
				a1isInputStream = true;
				in = ((URL)a1).openStream();
				inputStreamToClose = in;
			} else if (a1 instanceof Reader){
				a1isReader = true;
				reader = (Reader)a1;
			} else {
				throw new IllegalArgumentException(String.valueOf(a1));
			}
			boolean a2isOutputStream = false;
			boolean a2isWriter = false;
			boolean a2isNull = false;
			if (a2 instanceof OutputStream){
				a2isOutputStream = true;
				out = (OutputStream)a2;
			} else if (a2 instanceof File){
				a2isOutputStream = true;
				out = outputStreamToClose = new FileOutputStream((File)a2);
			} else if (a2 instanceof String){
				a2isOutputStream = true;
				out = new FileOutputStream(PathHelper.getFile((String)a2, context));
				outputStreamToClose = out;
			} else if (a2 instanceof Writer){
				a2isWriter = true;
				writer = (Writer)a2;
			} else if (a2 == null){
				a2isNull = true;
			} else if (a2 == DEFAULT_STREAM){
			} else {
				throw new IllegalArgumentException(String.valueOf(a2));
			}
	
			if (a1isInputStream){
				if (a2 == DEFAULT_STREAM){
					a2 = context.getOutputStream();
					out = (OutputStream)a2;
					a2isOutputStream = true;
				}
				if (out != null && a2isOutputStream){
					byte[] buf = new byte[8192];
					int n;
					while ((n = in.read(buf, 0, buf.length)) >= 0){
						out.write(buf, 0, n);
						nread += n;
					}
					out.flush();
				} else if (a2isWriter){
					CountingInputStream cin = new CountingInputStream(in);
					char[] buf = new char[8192];
					reader = CharacterEncoding.getReader(cin, context);
					int n;
					while ((n = reader.read(buf, 0, buf.length)) >= 0){
						writer.write(buf, 0, n);
					}
					nread += cin.count();
					writer.flush();
				} else if (a2 == null){
					byte[] buf = new byte[8192];
					int n;
					while ((n = in.read(buf, 0, buf.length)) >= 0){
						nread += n;
					}
				} else {
					throw new IllegalArgumentException();
				}
			} else if (a1isReader){
				if (a2 == DEFAULT_STREAM){
					a2 = context.getWriter();
					a2isWriter = true;
				}
				char[] buf = new char[8192];
				int n;
				if (a2isOutputStream){
					writer = CharacterEncoding.getWriter((OutputStream)a2, context);
					while ((n = reader.read(buf, 0, buf.length)) >= 0){
						writer.write(buf, 0, n);
						nread += n;
					}
					writer.flush();
				} else if (a2isWriter){
					writer = (Writer)a2;
					while ((n = reader.read(buf, 0, buf.length)) >= 0){
						writer.write(buf, 0, n);
						nread += n;
					}
					writer.flush();
				} else if (a2 == null){
					while ((n = reader.read(buf, 0, buf.length)) >= 0){
						nread += n;
					}
				} else {
					throw new IllegalArgumentException();
				}
			} else {
				throw new IllegalArgumentException();
			}
			return new Integer(nread);
		} catch (IOException e){
			throw new PnutsException(e, context);
		} finally {
			if (inputStreamToClose != null){
				try {
					inputStreamToClose.close();
				} catch (IOException e){
				}
			}
			if (outputStreamToClose != null){
				try {
					outputStreamToClose.close();
				} catch (IOException e){
				}
			}
		}
	}

	static class CountingInputStream extends FilterInputStream {
		int nread = 0;
		public CountingInputStream(InputStream in){
			super(in);
		}
		public int read() throws IOException {
			int c = in.read();
			if (c != -1){
				nread++;
			}
			return c;
		}
		public int read(byte[] buf, int offset, int len) throws IOException {
			int n = in.read(buf, offset, len);
			if (n >= 0){
				nread += n;
			}
			return n;
		}
		public int count(){
			return nread;
		}
	}

	public String toString(){
		return "function read(input, output)";
	}
}
