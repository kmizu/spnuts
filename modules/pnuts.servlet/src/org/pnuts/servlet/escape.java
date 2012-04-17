/*
 * escape.java
 *
 * Copyright (c) 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.lang.*;

public class escape extends PnutsFunction {

	public escape(){
		super("escape");
	}

	public boolean defined(int narg){
		return narg == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
		}
		Object arg = args[0];
		if (arg == null){
			return "";
		}
		String input = String.valueOf(arg);
		char[] a = null;
		int len = input.length();
		StringBuffer sbuf = null;
		int pos = 0;
		for (int i = 0; i < len; i++){
			if (a == null){
				char c = input.charAt(i);
				switch (c){
				case '<':
					a = input.toCharArray();
					sbuf = new StringBuffer(len * 10 / 9);
					sbuf.append(a, 0, i);
					sbuf.append("&lt;");
					pos = i + 1;
					break;
				case '>':
					a = input.toCharArray();
					sbuf = new StringBuffer(len * 10 / 9);
					sbuf.append(a, 0, i);
					sbuf.append("&gt;");
					pos = i + 1;
					break;
				case '&':
					a = input.toCharArray();
					sbuf = new StringBuffer(len * 10 / 9);
					sbuf.append(a, 0, i);
					sbuf.append("&amp;");
					pos = i + 1;
					break;
				case '"':
					a = input.toCharArray();
					sbuf = new StringBuffer(len * 10 / 9);
					sbuf.append(a, 0, i);
					sbuf.append("&quot;");
					pos = i + 1;
					break;
				case '\'':
					a = input.toCharArray();
					sbuf = new StringBuffer(len * 10 / 9);
					sbuf.append(a, 0, i);
					sbuf.append("&#39;");
					pos = i + 1;
					break;
				}
			} else {
				switch (a[i]){
				case '<':
					sbuf.append(a, pos, i - pos);
					sbuf.append("&lt;");
					pos = i + 1;
					break;
				case '>':
					sbuf.append(a, pos, i - pos);
					sbuf.append("&gt;");
					pos = i + 1;
					break;
				case '&':
					sbuf.append(a, pos, i - pos);
					sbuf.append("&amp;");
					pos = i + 1;
					break;
				case '"':
					sbuf.append(a, pos, i - pos);
					sbuf.append("&quot;");
					pos = i + 1;
					break;
				case '\'':
					sbuf.append(a, pos, i - pos);
					sbuf.append("&#39;");
					pos = i + 1;
					break;
				}
			}
		}
		if (a == null){
			return input;
		} else {
			if (pos > 0 && pos < len){
				sbuf.append(a, pos, len - pos);
			}
			return sbuf.toString();
		}
	}
}
