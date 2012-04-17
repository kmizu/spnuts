/*
 * @(#)SAXException.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import java.io.*;

class SAXException extends org.xml.sax.SAXException {
	private Throwable cause;

	public SAXException(String message){
		super(message);
	}

	public SAXException(Exception cause){
		super(cause);
		this.cause = cause;
	}

	public void printStackTrace(PrintStream s) {
		if (cause != null){
			cause.printStackTrace(s);
		} else {
			super.printStackTrace(s);
		}
	}

	public void printStackTrace(PrintWriter s){
		if (cause != null){
			cause.printStackTrace(s);
		} else {
			super.printStackTrace(s);
		}
	}
}
