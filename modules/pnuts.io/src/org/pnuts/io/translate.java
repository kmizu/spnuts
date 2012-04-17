/*
 * translate.java
 *
 * Copyright (c) 2005,2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.io;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class translate extends PnutsFunction {

	public translate(){
		super("translate");
	}

	public boolean defined(int narg){
		return (narg == 3);
	}
	
	static char[] toCharArray(Object obj){
		if (obj instanceof String){
			return ((String)obj).toCharArray();
		} else if (obj instanceof char[]){
			return (char[])obj;
		} else {
			throw new IllegalArgumentException(String.valueOf(obj));
		}
	}
	
	static byte[] toByteArray(Object obj){
		if (obj instanceof String){
			return ((String)obj).getBytes();
		} else if (obj instanceof byte[]){
			return (byte[])obj;
		} else {
			throw new IllegalArgumentException(String.valueOf(obj));
		}
	}
	
	protected Object exec(Object[] args, Context context){
		if (args.length != 3){
			undefined(args, context);
			return null;
		}
		Object arg0 = args[0];
		Object arg1 = args[1];
		Object arg2 = args[2];
		if (arg0 instanceof InputStream){
			byte[] b1 = toByteArray(arg1);
			byte[] b2 = toByteArray(arg2);
			return new TranslateInputStream((InputStream)arg0, b1, b2); 
		} else if (arg0 instanceof OutputStream){
			byte[] b1 = toByteArray(arg1);
			byte[] b2 = toByteArray(arg2);
			return new TranslateOutputStream((OutputStream)arg0, b1, b2);
		} else if (arg0 instanceof Reader){ 
			char[] c1 = toCharArray(arg1);
			char[] c2 = toCharArray(arg2);
			return new TranslateReader((Reader)arg0, c1, c2);
		} else if (arg0 instanceof Writer){
			char[] c1 = toCharArray(arg1);
			char[] c2 = toCharArray(arg2);
			return new TranslateWriter((Writer)arg0, c1, c2);
		} else if (arg0 instanceof byte[]){
			byte[] from = toByteArray(arg1);
			byte[] to = toByteArray(arg2);
			byte[] b = (byte[])arg0;
			byte[] table = new byte[256];
			for (int i = 0; i < 0xff; i++){
				table[i] = (byte)i;
			}
			for (int i = 0, len = from.length; i < len; i++){
				table[from[i] & 0xff] = to[i];
			}
			for (int i = 0, len = b.length; i < len; i++){
				b[i] = table[b[i]];
			}
			return b;
		} else if (arg0 instanceof char[]){
			char[] c = (char[])arg0;
			char[] from = toCharArray(arg1);
			char[] to = toCharArray(arg2);
			int l1 = from.length;
			int l2 = to.length;
			int l = l1 < l2 ? l1 : l2;
			Translation[] t = new Translation[l];
			for (int i = 0; i < l; i++){
			    t[i] = new Translation(from[i], to[i]);
			}
			Arrays.sort(t);
			char min = t[0].from;
			char max = t[t.length - 1].from;
			char[] table = new char[max - min + 1];
			for (int i = 0; i < table.length; i++){
			    table[i] = (char)(min + i);
			}
			for (int i = 0; i < t.length; i++){
				table[t[i].from - min] = t[i].to;
			}
			for (int i = 0; i < c.length; i++){
				char ch = c[i];
				if (ch >= min && ch <= max){
					c[i] = table[ch - min];
				} else {
				    c[i] = ch;
				}
			}
			return c;
		} else if (arg0 instanceof String){
			StringBuffer sbuf = new StringBuffer();
			String str = (String)arg0;
			char[] from = toCharArray(arg1);
			char[] to = toCharArray(arg2);
			int l1 = from.length;
			int l2 = to.length;
			int l = l1 < l2 ? l1 : l2;
			Translation[] t = new Translation[l];
			for (int i = 0; i < l; i++){
			    t[i] = new Translation(from[i], to[i]);
			}
			Arrays.sort(t);
			char min = t[0].from;
			char max = t[t.length - 1].from;
			char[] table = new char[max - min + 1];
			for (int i = 0; i < table.length; i++){
			    table[i] = (char)(min + i);
			}
			for (int i = 0; i < t.length; i++){
				table[t[i].from - min] = t[i].to;
			}
			for (int i = 0; i < str.length(); i++){
				char ch = str.charAt(i);
				if (ch >= min && ch <= max){
					sbuf.append(table[ch - min]);
				} else {
				    sbuf.append(ch);
				}
			}
			return sbuf.toString();
		} else {
			throw new IllegalArgumentException(String.valueOf(arg0));
		}
	}

	static class TranslateInputStream extends FilterInputStream {
		byte[] table = new byte[256];
		
		TranslateInputStream(InputStream in, byte[] from, byte[] to){
			super(in);
			for (int i = 0; i < 0xff; i++){
				table[i] = (byte)i;
			}
			for (int i = 0, len = from.length; i < len; i++){
				table[from[i] & 0xff] = to[i];
			}
		}
		
		public int read() throws IOException {
			int ch = super.read();
			if (ch == -1){
				return -1;
			} else {
				return table[ch & 0xff];
			}
		}

		public int read(byte[] buf, int offset, int len) throws IOException {
			int r = super.read(buf, offset, len);
			if (r != -1){
				for (int i = 0; i < len; i++){
					buf[i + offset] = table[buf[i + offset] & 0xff];
				}
			}
			return r;
		}	
	}
	
	static class TranslateOutputStream extends FilterOutputStream {
		byte[] table = new byte[256];
		
		TranslateOutputStream(OutputStream out, byte[] from, byte[] to){
			super(out);
			for (int i = 0; i < 0xff; i++){
				table[i] = (byte)i;
			}
			for (int i = 0, len = from.length; i < len; i++){
				table[from[i] & 0xff] = to[i];
			}
		}
		
		public void write(int ch) throws IOException {
			super.write((int)table[ch & 0xff]);
		}
		
		public void write(byte[] b, int offset, int len) throws IOException {
			for (int i = 0; i < len; i++){
				write(b[offset + i]);
			}
		}
			
	}	

	static class TranslateReader extends FilterReader {
		char[] table;
		int min;
		int max;
		
		TranslateReader(Reader in, char[] from, char[] to){
			super(in);
			if (from.length > 0){
				char[] tmp = (char[])from.clone();
				Arrays.sort(tmp);
				int min = tmp[0];
				int max = tmp[tmp.length - 1];
				this.table = new char[max - min + 1];
				for (int i = min; i <= max; i++){
					table[i] = (char)i;
				}
				for (int i = 0; i < from.length; i++){
					table[from[i] - min] = to[i];
				}
			}
		}
		
		public int read() throws IOException {
			int ch = super.read();
			if (table == null || ch < min || ch > max){
				return ch;
			} else {
				return table[ch - min];
			}
		}

		public int read(char[] buf, int offset, int len) throws IOException {
			int r = super.read(buf, offset, len);
			if (table != null && r != -1){
				for (int i = 0; i < len; i++){
					char ch = buf[i + offset];
					if (ch != -1 && ch >= min && ch <= max){
						buf[i + offset] = table[ch - min];
					}
				}
			}
			return r;
		}	
	}	
	
	static class TranslateWriter extends FilterWriter {
		char[] table;
		int min;
		int max;
		
		TranslateWriter(Writer out, char[] from, char[] to){
			super(out);
			if (from.length > 0){
				int min = from[0];
				int max = from[0];
				char[] tmp = (char[])from.clone();
				Arrays.sort(tmp);
				min = tmp[0];
				max = tmp[tmp.length - 1];
				this.table = new char[max - min + 1];
				for (int i = min; i <= max; i++){
					table[i] = (char)i;
				}
				for (int i = 0; i < from.length; i++){
					table[from[i] - min] = to[i];
				}
			}
		}

		public void write(int ch) throws IOException {
			if (table != null){
				super.write((int)table[ch - min]);
			} else {
				super.write(ch);
			}
		}
		
		public void write(char[] b, int offset, int len) throws IOException {
			for (int i = 0; i < len; i++){
				write(b[offset + i]);
			}
		}
			
	}	

    static class Translation implements Comparable {
	char from;
	char to;
	Translation(char from, char to){
	    this.from = from;
	    this.to = to;
	}
	public int compareTo(Object obj){
	    Translation t = (Translation)obj;
	    if (from < t.from){
		return -1;
	    } else if (from > t.from){
		return 1;
	    } else {
		return 0;
	    }
	}
	
    }

	public String toString(){
		return "function translate((InputStream|OutputStream|Reader|Writer|String|char[]|byte[]), from[], to[])";
	}
}
