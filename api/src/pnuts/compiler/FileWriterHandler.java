/*
 * @(#)FileWriterHandler.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * This class is a concrete class of ClassFileHandler. When this is passed to
 * Compiler.compile(..., ClassFileHandler), compiled class files are saved in
 * the directory specified with the constructor.
 */
public class FileWriterHandler implements ClassFileHandler, Serializable {
	private final static boolean DEBUG = false;

	private File dir;

	private boolean verbose;

	public FileWriterHandler(File dir) {
		this.dir = dir;
	}

	public void setVerbose(boolean flag) {
		this.verbose = flag;
	}

	protected void handleException(Exception e) {
		e.printStackTrace();
	}

	public Object handle(ClassFile cf) {
		try {
			write(cf, dir);
		} catch (IOException e) {
			handleException(e);
		}
		return null;
	}

	void write(ClassFile cf, File base) throws IOException {
		String className = cf.getClassName();
		if (DEBUG) {
			System.out.println("className " + className);
		}
		int idx = className.lastIndexOf('.');
		File file = null;
		if (idx > 0) {
			String dirname = className.substring(0, idx).replace('.',
					File.separatorChar);
			File d = new File(base, dirname);
			if (DEBUG) {
				System.out.println("mkdir " + d);
			}
			d.mkdirs();
			file = new File(d, className.substring(idx + 1) + ".class");
		} else {
			file = new File(base, className + ".class");
		}
		if (DEBUG) {
			System.out.println("open " + file);
		}
		FileOutputStream fin = new FileOutputStream(file);
		DataOutputStream dout = new DataOutputStream(fin);
		cf.write(dout);
		dout.close();
		if (verbose) {
			System.out.println(file);
		}
	}
}
