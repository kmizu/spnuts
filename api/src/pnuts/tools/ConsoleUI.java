/*
 * @(#)ConsoleUI.java 1.1 05/05/16
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

/*
 * Abstract interface for Console UI
 */
public interface ConsoleUI {

	/**
	 * Displays an output from the scripting engine
	 */
	void append(String str);
        
        void close();
}
