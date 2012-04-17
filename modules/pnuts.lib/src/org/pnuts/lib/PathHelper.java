/*
 * @(#)PathHelper.java 1.3 05/06/07
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import java.io.File;

/**
 * Provides helper methods to handle files
 */
public class PathHelper {

	static final String CWD = "cwd".intern();

	static boolean isRelative(File file){
		return !file.isAbsolute() && file.getPath().charAt(0) != File.separatorChar;
	}

	/**
	 * Gets a java.io.File object
	 *
	 * @param name the file name
	 * @param context the context in which the file name is resolved.
	 *  The context-local variable 'cwd' is used as the current directory if the
	 *  specified file name is not an absolute path.
	 * @return the java.io.File object that represents the file.
	 */
	public static File getFile(String name, Context context){
		File file = new File(name);
		if (isRelative(file)){
			String cwd = (String)context.get(CWD);
			if (cwd == null){
				cwd = System.getProperty("user.dir");
				context.set(CWD, cwd);
			}
			return new File(cwd, name);
		} else {
			return file;
		}
	}

	/**
	 * Sets the current directory
	 *
	 * @param dir the directory
	 * @param context the context
	 */
	public static void setCurrentDirectory(File dir, Context context){
		context.set(CWD, dir.getPath());
	}

	/**
	 * Ensure that the parent directories exist.
	 *
	 * @param file a java.io.File object
	 * @return true if all parent directories are already/successfully created.
	 */
	public static boolean ensureBaseDirectory(File file){
		File dir = new File(file.getParent());
		if (!dir.exists()){
			return dir.mkdirs();
		} else {
			return true;
		}
	}
}
