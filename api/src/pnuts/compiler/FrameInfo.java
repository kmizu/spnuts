/*
 * @(#)FrameInfo.java 1.3 05/05/09
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.util.Set;

class FrameInfo {
	boolean isLeaf;
	boolean leafCheckDone;
	boolean preprocessed;
	Set freeVars;
}
