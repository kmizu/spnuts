/*
 * @(#)ContextFactory.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import pnuts.lang.Context;

/**
 * Factory for Context object
 * <p>
 * This interface is used by pnuts.tools.Main class to create the initial context.
 */
public interface ContextFactory {

	/**
	 * Create a context
	 */
	public Context createContext();
}
