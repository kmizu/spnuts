/*
 * @(#)NodeUtil.java 1.2 05/06/27
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.io.*;
import java.nio.*;
import java.util.*;
import pnuts.lang.SimpleNode;
import pnuts.lang.Runtime;
import pnuts.lang.ParseException;
import pnuts.lang.PnutsParserTreeConstants;

public class NodeUtil {
	static String ENCODING = "UTF-8";

	public static void writeNode(ObjectOutputStream o, SimpleNode node) throws IOException {
		ByteArray ba = new ByteArray();
		save(node, ba);
		byte[] bytes = ba.getByteArray();
		int count = ba.size();
		o.writeInt(count);
		o.write(bytes, 0, count);
		o.flush();
	}

	public static SimpleNode readNode(ObjectInputStream in) throws IOException,ParseException {
		int len = in.readInt();
		byte[] buf = new byte[len];
		in.readFully(buf);
		return parseNode(new ByteArrayInputStream(buf));
	}

	static SimpleNode parseNode(InputStream in) throws IOException, ParseException {
		return parseNode(new BufferedReader(new InputStreamReader(in, ENCODING)));
	}

	public static SimpleNode parseNode(String str) {
		try {
			return parseNode(new StringReader(str));
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	static SimpleNode parseNode(Reader in) throws IOException,ParseException {
		int id = 0;
		int line = 0;
		int column = 0;
		String image = null;
		String info = null;
		Character c = null;
		ArrayList nodes = new ArrayList();
		int ch = in.read();
		int state = 0;
		StringBuffer sb = new StringBuffer();
		loop:
		while (ch != -1){
			switch (state){
			case 0:
				if (ch == '('){
					state = 1;
					break;
				} else if (ch == ')'){
					return null;
				} else {
					throw new RuntimeException(String.valueOf(ch));
				}
			case 1:    // id
				if (ch == ','){
					id = Integer.parseInt(sb.toString());
					sb.setLength(0);
					if (id == PnutsParserTreeConstants.JJTSTRINGNODE){
	 		 	 		int len = readInt(in);
						for (int i = 0; i < len; i++){
							sb.append((char)in.read());
						}
						if (len >= 0){
						    image = sb.toString();
						}
						sb.setLength(0);
						in.read();
						state = 3;
					} else {
						state = 2;
					}
				} else {
					sb.append((char)ch);
				}
				break;
			case 2:    // image
				if (ch == ','){
					image = sb.toString();
					sb.setLength(0);
					state = 3;
				} else {
					sb.append((char)ch);
				}
				break;
			case 3:    // line
				if (ch == ','){
					line = Integer.parseInt(sb.toString());
					sb.setLength(0);
					state = 4;
				} else {
					sb.append((char)ch);
				}
				break;				
			case 4:    // column
				if (ch == ','){
					column = Integer.parseInt(sb.toString());
					sb.setLength(0);
					state = 5;
				} else {
					sb.append((char)ch);
				}
				break;
			case 5:   // info
				if (id == PnutsParserTreeConstants.JJTCHARACTERNODE){
					c = new Character((char)ch);
					in.read();
					state = 6;
					break;
				} else {
					if (ch == ','){
						String str = sb.toString();
						if (!str.startsWith("{")){
							info = str;
						}
						sb.setLength(0);
						state = 6;
					} else {
						sb.append((char)ch);
						break;
					}
				}
			case 6:   // children
				if (ch == ')'){
					break loop;
				} else {
					SimpleNode n = parseNode(in);
					if (n != null){
						nodes.add(n);
					} else {
						break loop;
					}
				}
				break;
			}
			ch = in.read();
		}
		SimpleNode n = new SimpleNode(id);
		if (image != null){
		    n.str = image.intern();
		}
		if (id == PnutsParserTreeConstants.JJTINTEGERNODE){
			n.info = Runtime.parseInt(image);
		} else if (id == PnutsParserTreeConstants.JJTFLOATINGNODE){
			n.info = Runtime.parseFloat(image);
		} else if (id == PnutsParserTreeConstants.JJTCHARACTERNODE){
			n.info = c;
		} else if (id == PnutsParserTreeConstants.JJTBEANPROPERTYDEF){
			n.info = info;
		}
		n.beginLine = line;
		n.beginColumn = column;
		int size = nodes.size();
		for (int i = size; i > 0; i--){
			SimpleNode cn = (SimpleNode)nodes.get(i - 1);
			cn.jjtSetParent(n);
			n.jjtAddChild(cn, i - 1);
		}
		return n;
	}

	public static String saveNode(SimpleNode node, StringBuffer sb){
		try {
			sb.setLength(0);
			ByteArray ba = new ByteArray();
			save(node, ba);
			byte[] bytes = ba.getByteArray();
			int count = ba.size();
			return byteArrayToString(bytes, count, sb);
		} catch (IOException e){
			e.printStackTrace();
			return null;
		}
	}

	public static String saveNode(SimpleNode node){
		return saveNode(node, new StringBuffer());
	}

	static void writeDigits(OutputStream os, int d) throws IOException {
		writeString(os, Integer.toString(d));
	}

	static void writeInt(OutputStream out, int v) throws IOException {
	        out.write((v >>> 24) & 0xFF);
		out.write((v >>> 16) & 0xFF);
		out.write((v >>>  8) & 0xFF);
		out.write((v >>>  0) & 0xFF);
	}

	static int readInt(Reader in) throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();

		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	
	static void writeString(OutputStream os, String s) throws IOException {
		
		int n = s.length();
		
		for (int i = 0; i < n; i++){
			char c = s.charAt(i);

			if (c < 0x80){
				os.write(c);
			} else if (c < 0x800) {
				os.write(0xc0 + (c >> 6));
				os.write(0x80 + (c & 0x3f));
			} else {
				os.write(0xe0 + (c >> 12));
				os.write(0x80 + ((c >> 6) & 0x3f));
				os.write(0x80 + (c & 0x3f));
			}
		}
	}
	/*
	 * (id,image,beginLine,endLine,{literal:offset},...
	 * (id,image,beginLine,endLine,char,...
	 */
	static void save(SimpleNode node, OutputStream w) throws IOException {
		w.write('(');
		writeDigits(w, node.id);
		w.write(',');
		if (node.str != null){
			if (node.id == PnutsParserTreeConstants.JJTSTRINGNODE){
				writeInt(w, node.str.length());
				writeString(w, node.str);
			} else if (node.id != PnutsParserTreeConstants.JJTCHARACTERNODE){
				writeString(w, node.str);
			}
		} else {
			if (node.id == PnutsParserTreeConstants.JJTSTRINGNODE){
				writeInt(w, -1);
			}
		}
		w.write(',');
		writeDigits(w, node.beginLine);
		w.write(',');
		writeDigits(w, node.beginColumn);
		w.write(',');
		if (node.info instanceof String){
			writeString(w, (String)node.info);
		} else {
			if (node.id == PnutsParserTreeConstants.JJTINTEGERNODE ||
			    node.id == PnutsParserTreeConstants.JJTFLOATINGNODE)
			{
				Object[] info = (Object[])node.info;
				Number n = (Number)info[0];
				int[] offset = (int[])info[1];
				w.write('{');
				writeString(w, n.toString());
				w.write(':');
				if (offset != null){
					writeDigits(w, offset[0]);
				}
				w.write('}');
			} else if (node.id == PnutsParserTreeConstants.JJTCHARACTERNODE){
				Character ch = (Character)node.info;
		 		writeString(w, String.valueOf(ch.charValue()));
			}
		}
		w.write(',');
		int nchildren = node.jjtGetNumChildren();
		if (nchildren > 0){
			save(node.jjtGetChild(0), w);
		}
		for (int i = 1; i < nchildren; i++){
			w.write(',');
			save(node.jjtGetChild(i), w);
		}
		w.write(')');
	}


	public static SimpleNode loadNode(String str) {
		byte[] b = stringToByteArray(str);
		/**
		b = ZipUtil.unzipByteArray(b);
		**/
		InputStream in = new ByteArrayInputStream(b);
		try {
			Reader r = new BufferedReader(new InputStreamReader(in, ENCODING));
			return parseNode(r);
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}


	static String byteArrayToString(byte[] array, int len, StringBuffer sbuf) {
		int i = 0;
		if ((len % 2) == 0) {
			sbuf.append('\0');
		} else {
			sbuf.append('\uffff');
		}
		for (; i < len / 2; i++) {
			sbuf.append((char) ((array[i * 2] << 8) | (array[i * 2 + 1] & 0xff)));
		}
		if (i * 2 < len) {
			sbuf.append((char) (array[i * 2] << 8));
		}
		return sbuf.toString();
	}

	public static String byteArrayToString(byte[] array, int len) {
		StringBuffer sbuf = new StringBuffer(len / 2 + 1);
		int i = 0;
		if ((len % 2) == 0) {
			sbuf.append('\0');
		} else {
			sbuf.append('\uffff');
		}
		for (; i < len / 2; i++) {
			sbuf.append((char) ((array[i * 2] << 8) | (array[i * 2 + 1] & 0xff)));
		}
		if (i * 2 < len) {
			sbuf.append((char) (array[i * 2] << 8));
		}
		return sbuf.toString();
	}

	public static byte[] stringToByteArray(String s) {
		return charArrayToByteArray(s.toCharArray());
	}

	static byte[] charArrayToByteArray(char[] c) {
		int i = 1;
		int j = 0;
		int len = c.length;
		if (c[0] == '\0') { // even length
			byte[] bytes = new byte[(len - 1) * 2];
			for (; i < len; i++) {
				char ch = c[i];
				bytes[j++] = (byte) ((ch >> 8) & 255);
				bytes[j++] = (byte) (ch & 255);
			}
			return bytes;
		} else { // odd length
			byte[] bytes = new byte[(len - 1) * 2 - 1];
			for (; i < len - 1; i++) {
				char ch = c[i];
				bytes[j++] = (byte) ((ch >> 8) & 255);
				bytes[j++] = (byte) (ch & 255);
			}
			bytes[j++] = (byte) ((c[i] >> 8) % 255);
			return bytes;
		}
	}


	public static String unparseNode(SimpleNode node){
		StringBuffer sbuf = new StringBuffer();
		node.accept(new org.pnuts.lang.UnparseVisitor(sbuf), null);
		return sbuf.toString();
	}

	public static void setPackage(String pkg, SimpleNode ss){
		SimpleNode el = new SimpleNode(PnutsParserTreeConstants.JJTEXPRESSIONLIST);
		ss.jjtAddChild(el, ss.jjtGetNumChildren());
		el.jjtSetParent(ss);

		SimpleNode an = new SimpleNode(PnutsParserTreeConstants.JJTAPPLICATIONNODE);
		el.jjtAddChild(an, 0);
		an.jjtSetParent(el);
  
		SimpleNode in = new SimpleNode(PnutsParserTreeConstants.JJTIDNODE);
		in.str = "package".intern();

		an.jjtAddChild(in, 0);
		in.jjtSetParent(an);

		SimpleNode le = new SimpleNode(PnutsParserTreeConstants.JJTLISTELEMENTS);
		an.jjtAddChild(le, 1);

		SimpleNode sn;
		if (pkg != null){
			sn = new SimpleNode(PnutsParserTreeConstants.JJTSTRINGNODE);
			sn.str = pkg;
		} else {
			sn = new SimpleNode(PnutsParserTreeConstants.JJTNULLNODE);
		}
		le.jjtAddChild(sn, 0);
		sn.jjtSetParent(le);
	}

	public static void addImportNode(String def, SimpleNode ss){
		SimpleNode el = new SimpleNode(PnutsParserTreeConstants.JJTEXPRESSIONLIST);
		ss.jjtAddChild(el, ss.jjtGetNumChildren());
		el.jjtSetParent(ss);

		SimpleNode im = new SimpleNode(PnutsParserTreeConstants.JJTIMPORT);
		el.jjtAddChild(im, 0);
		im.jjtSetParent(el);

		SimpleNode sn = new SimpleNode(PnutsParserTreeConstants.JJTSTRINGNODE);
		im.jjtAddChild(sn, 0);
		sn.jjtSetParent(im);
		sn.str = def;
	}

	public static void addFunction(SimpleNode fnode, SimpleNode ss){
		SimpleNode el = new SimpleNode(PnutsParserTreeConstants.JJTEXPRESSIONLIST);
		ss.jjtAddChild(el, ss.jjtGetNumChildren());
		el.jjtSetParent(ss);

		el.jjtAddChild(fnode, 0);
		fnode.jjtSetParent(el);
	}


	static class ByteArray extends OutputStream {

		protected byte buf[];

		protected int count;

		public ByteArray() {
			this(1024);
		}

		public ByteArray(int size) {
			if (size < 0) {
				throw new IllegalArgumentException("Negative initial size: " + size);
			}
			buf = new byte[size];
		}

		public void write(int b) {
			int newcount = count + 1;
			if (newcount > buf.length) {
				byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			buf[count] = (byte)b;
			count = newcount;
		}

		public void write(byte b[], int off, int len) {
			if ((off < 0) || (off > b.length) || (len < 0) ||
			    ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}
			int newcount = count + len;
			if (newcount > buf.length) {
				byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			System.arraycopy(b, off, buf, count, len);
			count = newcount;
		}

		public void writeTo(OutputStream out) throws IOException {
			out.write(buf, 0, count);
		}

		public void reset() {
			count = 0;
		}

		public byte toByteArray()[] {
			byte newbuf[] = new byte[count];
			System.arraycopy(buf, 0, newbuf, 0, count);
			return newbuf;
		}

		public int size() {
			return count;
		}

		public String toString() {
			return new String(buf, 0, count);
		}

		public String toString(String enc) throws UnsupportedEncodingException {
			return new String(buf, 0, count, enc);
		}

		public void close() throws IOException {
		}

		public byte[] getByteArray(){
			return buf;
		}
	}
}
