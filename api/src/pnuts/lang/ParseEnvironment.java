/*
 * @(#)ParseEnvironment.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * This class defines how to handle ParseException thrown by the parser. The
 * instances can be passed to Pnuts.parse(..) method in order to customize the
 * way of error recovery of parsing.
 */
public interface ParseEnvironment {

	/**
	 * Thie method defines how to deal with parse errors
	 * 
	 * @param e
	 *            a ParseException object passed by the parser
	 * @exception ParseException
	 *                this method may rethrow the ParseException
	 */
	void handleParseException(ParseException e) throws ParseException;
}