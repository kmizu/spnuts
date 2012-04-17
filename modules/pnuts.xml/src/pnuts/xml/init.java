/*
 * @(#)init.java 1.3 05/01/14
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;
import pnuts.lang.Package;

/**
 * Initialization of the pnuts.xml module
 */
public class init extends ModuleBase {

	static String[] javaFunctions = {
		"nodeAccess",
		"nodeEdit",
		"element",
		"defineTags",
		"newDocument",
		"writeDocument",
		"traverseDocument",
		"readDocument",
		"transformXSL",
		"parseXML",
		"selectSingleNode",
		"selectNodeList"
	};

	static String[] files  = {
	};

	static String[][] functions = {
		{
			"openSOAPConnection",
			"createSOAPMessage",
			"sendSOAPMessage"
		}
	};

	static String[] requiredModules  = {
	    "pnuts.lib"
	};

	protected String[] getRequiredModules(){
		return requiredModules;
	}

	protected String getPrefix(){
		return "org";
	}

	public Object execute(Context context){
		for (int i = 0; i < javaFunctions.length; i++){
			autoloadFunction(javaFunctions[i], context);
		}

		for (int i = 0; i < files.length; i++){
			autoload(functions[i], files[i], context);
		}

		return null;
	}
}
