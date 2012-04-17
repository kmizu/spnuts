/*
 * @(#)LineProcessor.java 1.2 04/12/06
 *
 * Copyright (c) 2002,2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import java.io.*;

public interface LineProcessor {
	public int processAll(boolean includeNewLine) throws IOException;
}
