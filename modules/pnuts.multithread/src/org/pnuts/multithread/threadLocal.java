/*
 * @(#)threadLocal.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Property;
import pnuts.lang.Package;

public class threadLocal extends PnutsFunction {

	final static ThreadLocalPackage instance = new ThreadLocalPackage();

	public threadLocal(){
		super("threadLocal");
	}

	public boolean defined(int nargs){
		return nargs == 0;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 0){
			undefined(args, context);
			return null;
		}
		return instance;
	}

	static class ThreadLocalPackage extends ThreadLocal implements Property {

		protected Object initialValue() {
			return new Package(null, null);
		}

		public Object get(String symbol, Context context){
			return ((Package)super.get()).get(symbol);
		}

		public void set(String symbol, Object value, Context context){
			((Package)super.get()).set(symbol, value);
		}
	}

	public String toString(){
		return "function threadLocal()";
	}
}
