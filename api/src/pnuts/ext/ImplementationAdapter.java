/*
 * @(#)ImplementationAdapter.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import java.io.FileNotFoundException;
import java.net.URL;

import pnuts.lang.Context;
import pnuts.lang.Implementation;
import pnuts.lang.PnutsImpl;
import pnuts.lang.SimpleNode;

/**
 * This class is used to customize an existing Implementation.
 */
public class ImplementationAdapter extends PnutsImpl {

	private Implementation impl;

	public ImplementationAdapter(Implementation impl) {
		this.impl = impl;
	}

	/**
	 * Returns the base Implementation which was passed to the constructor
	 * 
	 * @return the base Implementation object.
	 */
	public Implementation getBaseImpl() {
		return impl;
	}

	/**
	 * Evaluate an expreesion
	 * 
	 * @param str
	 *            the expression to be evaluated
	 * @param context
	 *            the context in which the expression is evaluated
	 * @return the result of the evaluation
	 */
	public Object eval(String str, Context context) {
		return impl.eval(str, context);
	}

	/**
	 * Load a script file from local file system
	 * 
	 * @param filename
	 *            the file name of the script
	 * @param context
	 *            the context in which the expression is evaluated
	 * @return the result of the evaluation
	 */
	public Object loadFile(String filename, Context context)
			throws FileNotFoundException {
		return impl.loadFile(filename, context);
	}

	/**
	 * Load a script file using classloader
	 * 
	 * @param file
	 *            the name of the script
	 * @param context
	 *            the context in which the script is executed
	 * @return the result of the evaluation
	 */
	public Object load(String file, Context context)
			throws FileNotFoundException {
		return impl.load(file, context);
	}

	/**
	 * Load a script file from a URL
	 * 
	 * @param scriptURL
	 *            the URL of the script
	 * @param context
	 *            the context in which the script is executed
	 * @return the result of the evaluation
	 */
	public Object load(URL scriptURL, Context context) {
		return impl.load(scriptURL, context);
	}

	public Object accept(SimpleNode node, Context context) {
		return impl.accept(node, context);
	}
}