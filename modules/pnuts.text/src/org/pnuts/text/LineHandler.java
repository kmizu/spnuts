/*
 * @(#)LineHandler.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

/**
 * An abstract interface for LineReader's callback
 */
public interface LineHandler {

	/**
	 * Processes the current line.
	 *
	 * @param cb the char buffer that contains the current line
	 * @param offset the offset of the buffer
	 * @param length the length of the current line
	 */
	void process(char[] cb, int offset, int length);
}
