/*
 * @(#)init.java 1.3 05/01/14
 *
 * Copyright (c) 2001-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.jdo;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.math module.
 */
public class init extends ModuleBase {

	static String[] files  = {
		"pnuts/jdo/functions"
	};

	static String[][] functions = {
		// pnuts/jdo/functions
		{
			"openJDO"
		},

	};

	static String[] requiredModules  = {
	    "pnuts.lib",
	    "pnuts.beans"
	};

	protected String[] getRequiredModules(){
		return requiredModules;
	}

	public Object execute(Context context){
		for (int i = 0; i < files.length; i++){
			autoload(functions[i], files[i], context);
		}
		return null;
	}
}
