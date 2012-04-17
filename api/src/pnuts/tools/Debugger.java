/*
 * @(#)Debugger.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

/**
 * An abstract interface for debugger
 */
public interface Debugger extends CommandListener {
	/**
	 * Sets a breakpoint
	 */
	public void setBreakPoint(Object source, int lineno);

	/**
	 * Remove a breakpoint
	 */
	public void removeBreakPoint(Object source, int lineno);

	/**
	 * Remove all breakpoints
	 */
	public void clearBreakPoints();
}