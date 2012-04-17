/*
 * @(#)URLEncoding.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.net;

import java.util.*;
import java.io.*;

/**
 * A set of utility methods which are related to character encoding.
 * 
 * @version	1.1
 */
public class URLEncoding {
	private final static boolean DEBUG = false;
	private static BitSet dontNeedEncoding;
	private final static int caseDiff = ('a' - 'A');

	static {
		dontNeedEncoding = new BitSet(256);
		int i;
		for (i = 'a'; i <= 'z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = 'A'; i <= 'Z'; i++) {
			dontNeedEncoding.set(i);
		}
		for (i = '0'; i <= '9'; i++) {
			dontNeedEncoding.set(i);
		}
		dontNeedEncoding.set(' '); /* encoding a space to a + is done in the encode() method */
		dontNeedEncoding.set('-');
		dontNeedEncoding.set('_');
		dontNeedEncoding.set('.');
		dontNeedEncoding.set('*');
	}

	protected URLEncoding() {}

	/**
	 * Translates a string into <code>x-www-form-urlencoded</code> format.
	 *
	 * @param s   The input data to be encoded
	 * @param enc The character encoding
	 */
	public static String encode(String s, String enc)
		throws UnsupportedEncodingException
		{
			int maxBytesPerChar = 10;
			StringBuffer out = new StringBuffer();
			ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);
			OutputStreamWriter writer = new OutputStreamWriter(buf, enc);

			for (int i = 0; i < s.length(); i++) {
				int c = (int)s.charAt(i);
				if (dontNeedEncoding.get(c)) {
					if (c == ' ') {
						c = '+';
					}
					out.append((char)c);
				} else {
					// convert to external encoding before hex conversion
					try {
						writer.write(c);
						writer.flush();
					} catch(IOException e) {
						buf.reset();
						continue;
					}
					byte[] ba = buf.toByteArray();
					for (int j = 0; j < ba.length; j++) {
						out.append('%');
						char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
						// converting to use uppercase letter as part of
						// the hex value if ch is a letter.
						if (Character.isLetter(ch)) {
							ch -= caseDiff;
						}
						out.append(ch);
						ch = Character.forDigit(ba[j] & 0xF, 16);
						if (Character.isLetter(ch)) {
							ch -= caseDiff;
						}
						out.append(ch);
					}
					buf.reset();
				}
			}

			return out.toString();
		}

	/**
	 * Decodes a <code>x-www-form-urlencoded</code> to a String.
	 *
	 * @param s The encoded data to be decoded
	 * @param enc The character encoding
	 */
	public static String decode(String s, String enc)
		throws UnsupportedEncodingException
		{
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for (int i = 0; i < s.length(); i++){
				char c = s.charAt(i);
				switch (c) {
				case '+':
					bout.write((int)' ');
					break;
				case '%':
					try {
						bout.write(Integer.parseInt(s.substring(i+1,i+3), 16));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(s.substring(i+1,i+3));
					}
					i += 2;
					break;
				default:
					bout.write((int)c);
					break;
				}
			}
			return new String(bout.toByteArray(), enc);
		}

	/**
	 * Parses a QUERY_STRING passed from the client to the server and build a Hashtable
	 * with key-value pairs.
	 *
	 * @param s "QUERY_STRING"
	 * @param enc The character encoding
	 *
	 * @see javax.servlet.http.HttpUtils#parseQueryString(java.lang.String)
	 */
	public static Map parseQueryString(String s, String enc)
		throws UnsupportedEncodingException
		{
			if (DEBUG){
				System.out.println("parseQueryString(" + s + ", " + enc + ")");
			}
			String valArray[] = null;
	
			if (s == null) {
				throw new IllegalArgumentException();
			}
			Hashtable ht = new Hashtable();
			StringBuffer sb = new StringBuffer();
			StringTokenizer st = new StringTokenizer(s, "&");
			while (st.hasMoreTokens()) {
				String pair = (String)st.nextToken();
				int pos = pair.indexOf('=');
				if (pos == -1) {
					continue;
				}
				String key = decode(pair.substring(0, pos), enc);
				String val = decode(pair.substring(pos+1, pair.length()), enc);
				if (DEBUG){
					System.out.println("key = " + key + ", value = " + val);
				}

				if (ht.containsKey(key)) {
					String oldVals[] = (String []) ht.get(key);
					valArray = new String[oldVals.length + 1];
					for (int i = 0; i < oldVals.length; i++) 
						valArray[i] = oldVals[i];
					valArray[oldVals.length] = val;
				} else {
					valArray = new String[1];
					valArray[0] = val;
				}
				ht.put(key, valArray);
			}
			return ht;
		}

	/**
	 * Makes a QUERY STRING from java.util.Map.
	 *
	 * @param table a java.util.Map object
	 * @param enc the character encoding
	 *
	 * @return the resulting query string.
	 */
	public static String makeQueryString(Map table, String enc)
		throws UnsupportedEncodingException
		{
			boolean first = true;
			StringBuffer buf = new StringBuffer();
			for (Iterator it = table.keySet().iterator(); it.hasNext(); ){
				String key = (String)it.next();
				Object value = table.get(key);
				if (value instanceof Object[]){
					Object[] avalue = (Object[])value;
					for (int i = 0; i < avalue.length; i++){
						String svalue = String.valueOf(avalue[i]);
						if (svalue == null) continue;
						if (first){
							first = false;
						} else {
							buf.append("&");
						}
						if (value != null){
							buf.append(encode(key, enc) + "=" + encode(svalue, enc));
						}
					}
				} else if (value instanceof String){
					String svalue = (String)value;
					if (first){
						first = false;
					} else {
						buf.append("&");
					}
					if (value != null){
						buf.append(encode(key, enc) + "=" + encode(svalue, enc));
					}
				}
			}
			return buf.toString();
		}
}
