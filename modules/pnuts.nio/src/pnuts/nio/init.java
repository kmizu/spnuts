/*
 * @(#)init.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.nio;

import pnuts.lang.Runtime;
import pnuts.lang.Package;
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.text module
 */
public class init extends ModuleBase {

	static String[] files  = {
		"pnuts/nio/nio",
	};

	static String[][] functions = {
		{
//		"open",
//		"reader",
//		"writer",
//		"openBuffer",
//		"openDirectBuffer",
//		"openChannel",
//		"transferChannel",
			"mapFile",
			"charset",
			"charsets"
		}
	};

	static String[] javaFunctions = {
		"scanLines"
	};

	protected String getPrefix(){
		return "org";
	}

	public Object execute(Context context){
		try {
			Class.forName("java.nio.Buffer");
		} catch (Exception e){
			return null;
		}
		context.clearPackages();
		context.usePackage("pnuts.io");
		context.usePackage("pnuts.lib");
		context.usePackage("pnuts.text");

		for (int i = 0; i < files.length; i++){
			autoload(functions[i], files[i], context);
		}
		for (int i = 0; i < javaFunctions.length; i++){
			autoloadFunction(javaFunctions[i], context);
		}
		return null;
	}
}
