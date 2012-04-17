/*
 * @(#)setThreadClassPath.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

/*
 * setThreadClassPath(["path1", "path2", ...])
 */
public class setThreadClassPath extends PnutsFunction {
	private final static String CWD = "cwd".intern();

	public setThreadClassPath() {
		super("setThreadClassPath");
	}

	public boolean defined(int nargs) {
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context) {
		if (args.length != 1) {
			undefined(args, context);
			return null;
		}
		String cwd = (String) context.get(CWD);
		try {
			Object[] arg = (Object[]) args[0];
			int len = arg.length;
			URL[] urls = new URL[len];
			for (int i = 0; i < len; i++) {
				File f = new File(cwd, (String) arg[i]);
				String path;
				if (f.isDirectory()) {
					path = f.getCanonicalPath() + "/";
				} else {
                                        path = f.getCanonicalPath();
                                }
				urls[i] = new URL("file", null, 0, path);
			}
			Thread.currentThread().setContextClassLoader(
					new URLClassLoader(urls));
		} catch (IOException e) {
			// ignore
		}
		return null;
	}
}