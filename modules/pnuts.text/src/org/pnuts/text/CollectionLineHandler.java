/*
 * @(#)CollectionLineHandler.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import java.util.*;

class CollectionLineHandler implements LineHandler {
	Collection col;

	CollectionLineHandler(Collection col){
		this.col = col;
	}

	public void process(char[] c, int offset, int length){
		col.add(new String(c, offset, length));
	}
}
