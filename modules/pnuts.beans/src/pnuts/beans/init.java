/*
 * @(#)init.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.beans;

import pnuts.lang.Runtime;
import pnuts.lang.Package;
import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.beans module
 */
public class init extends ModuleBase {

	static String[] javaFunctions = {
		"getBeanProperty",
		"setBeanProperty",
		"getBeanProperties",
		"setBeanProperties",
		"bind",
		"unbind",
		"encodeBean",
		"decodeBean",
		"generateBeanClass"
	};

	static String[] files  = {
		"pnuts/beans/info"
	};

	static String[][] functions = {
		{
			"properties",
			"events",
			"methods",
			"listenerMethodNames",
			"listenerType"
		}
	};

	protected String getPrefix(){
		return "org";
	}

	public Object execute(Context context){
		context.clearPackages();
		for (int i = 0; i < files.length; i++){
			autoload(functions[i], files[i], context);
		}
		for (int i = 0; i < javaFunctions.length; i++){
			autoloadFunction(javaFunctions[i], context);
		}
		return null;
	}
}

