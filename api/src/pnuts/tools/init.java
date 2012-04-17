/*
 * @(#)init.java 1.4 05/06/14
 * 
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import pnuts.ext.ModuleBase;
import pnuts.lang.Context;

public class init extends ModuleBase {

	static String[] files = { "pnuts/tools/pnutool" };

	static String[][] functions = { { "pnutool" } };

	static String[] javaFunctions = { "console", "setThreadClassPath" };

	static String[] modules = { "pnuts.math", "pnuts.multithread",
			"pnuts.security", "pnuts.mail", "pnuts.xml", "pnuts.util",
			"pnuts.awt", "pnuts.beans", "pnuts.text", "pnuts.regex",
			"pnuts.io", "pnuts.nio", "pnuts.lib", "pnuts.jdbc", "pnuts.jdo",
			"pnuts.net", "pnuts.jmx", "java.util", "java.io", "java.net" };

	public String getPrefix() {
		return "org";
	}

	protected String[] getSubModules(){
		return modules;
	}

	public Object execute(Context context) {
		for (int i = 0; i < files.length; i++) {
			autoload(functions[i], files[i], context);
		}
		for (int i = 0; i < javaFunctions.length; i++) {
			autoloadFunction(javaFunctions[i], context);
		}
		return null;
	}
}
