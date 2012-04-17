/*
 * @(#)classGenerator.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lib.ScriptPackage;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.compiler.*;
import org.pnuts.lang.ClassFileLoader;
import java.io.File;
import java.util.zip.ZipOutputStream;

public class classGenerator extends PnutsFunction {

	public classGenerator(){
		super("classGenerator");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 0;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Object arg;
		ClassFileHandler handler = null;
		ClassLoader loader = null;

		if (nargs == 0){
			arg = Thread.currentThread().getContextClassLoader();
		} else if (nargs == 1){
			arg = args[0];
		} else {
			undefined(args, context);
			return null;
		}
		if (arg instanceof ClassLoader){
			ClassLoader classLoader = (ClassLoader)arg;
			if (classLoader instanceof ClassFileLoader){
				loader = classLoader;
			} else {
				loader = new ClassFileLoader((ClassLoader)arg);
			}
			handler = (ClassFileHandler)loader;
		} else if (arg instanceof String){
			handler = new FileWriterHandler(PathHelper.getFile((String)arg, context));
		} else if (arg instanceof File){
			handler = new FileWriterHandler((File)arg);
		} else if (arg instanceof ZipOutputStream){
			handler = new ZipWriterHandler((ZipOutputStream)arg);
		} else {
			throw new IllegalArgumentException(String.valueOf(arg));
		}
		ScriptPackage pkg = new ScriptPackage();
		pkg.set("subclass".intern(), new subclass(handler));
		pkg.set("interface".intern(), new _interface(handler));
		pkg.set("beanclass".intern(), new beanclass(handler));
		pkg.set("getClassLoader".intern(), new getClassLoader(loader));
		return pkg;
	}

	static class getClassLoader extends PnutsFunction {
		ClassLoader loader;

		getClassLoader(ClassLoader loader){
			super("getClassLoader");
			this.loader = loader;
		}

		public boolean defined(int nargs){
			return nargs == 0;
		}

		protected Object exec(Object[] args, Context context){
			return loader;
		}
	}

	public String toString(){
		return "function classGenerator( { (ClassLoader | ZipOutputStream | File | String) } )";
	}
}
