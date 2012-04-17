/*
 * @(#)Implementation.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.FileNotFoundException;
import java.net.URL;

/**
 * Defines an abstract interface of script interpreter's implementation,
 * 
 * @see pnuts.lang.Context#setImplementation(Implementation)
 * @see pnuts.lang.Context#getImplementation()
 */
public interface Implementation {

	/**
	 * Evaluate an expreesion
	 * 
	 * @param expr
	 *            the expression to be evaluated
	 * @param context
	 *            the context in which the expression is evaluated
	 * @return the result of the evaluation
	 */
	public Object eval(String expr, Context context);

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
			throws FileNotFoundException;

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
			throws FileNotFoundException;

	/**
	 * Load a script file from a URL
	 * 
	 * @param scriptURL
	 *            the URL of the script
	 * @param context
	 *            the context in which the script is executed
	 * @return the result of the evaluation
	 */
	public Object load(URL scriptURL, Context context);

	/**
	 * Interpret an AST
	 * 
	 * @param node
	 *            the AST
	 * @param context
	 *            the context in which the AST is interpreted
	 */
	public Object accept(SimpleNode node, Context context);
}