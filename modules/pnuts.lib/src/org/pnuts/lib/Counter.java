/*
 * @(#)Counter.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.io.*;
import java.util.*;

class Counter {

	public Number count(Object a, Context context){
		if (a instanceof Enumeration){
			Enumeration e = (Enumeration)a;
			int c = 0;
			while (e.hasMoreElements()){
				c++;
				e.nextElement();
			}
			return new Integer(c);
		} else if (a instanceof Iterator){
			Iterator it = (Iterator)a;
			int c = 0;
			while (it.hasNext()){
				c++;
				it.next();
			}
			return new Integer(c);
		} else if (a instanceof Generator){
			Generator g = (Generator)a;
			class F extends PnutsFunction {
				int count = 0;
				protected Object exec(Object[] args, Context context){
					count++;
					return null;
				}
			}
			F f = new F();
			g.apply(f, context);
			return new Integer(f.count);
		} else if (a instanceof InputStream){
			byte[] buf = new byte[4096];
			int n = 0;
			int c = 0;
			InputStream in = (InputStream)a;
			try {
				while ((n = in.read(buf)) != -1){
					c += n;
				}
			} catch (IOException e){
				throw new PnutsException(e, context);
			}
			return new Integer(c);
		} else if (a instanceof Reader){
			char[] buf = new char[4096];
			int n = 0;
			int c = 0;
			Reader in = (Reader)a;
			try {
				while ((n = in.read(buf)) != -1){
					c += n;
				}
			} catch(IOException e){
				throw new PnutsException(e, context);
			}
			return new Integer(c);
		} else {
			return size(a, context);
		}
	}

	public Number size(Object a, Context context){
		if (a == null){
			return new Integer(0);
		} else if (a instanceof String){
			return new Integer(((String)a).length());
		} else if (a instanceof StringBuffer){
			return new Integer(((StringBuffer)a).length());
		} else if (a instanceof Object[]){
			return new Integer(((Object[])a).length);
		} else if (a instanceof File){
			return new Long(((File)a).length());
		} else if (a instanceof Collection){
			return new Integer(((Collection)a).size());
		} else if (a instanceof Map){
			return new Integer(((Map)a).size());
		} else if (Runtime.isArray(a)){
			return new Integer(Runtime.getArrayLength(a));
		} else {
			throw new IllegalArgumentException(String.valueOf(a));
		}
	}
}
