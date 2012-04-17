/*
 * @(#)init.java 1.5 05/06/28
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.util;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.util module
 */
public class init extends ModuleBase {

	static String[] javaFunctions = {
		"nonPublicMemberAccess",
		"publicMemberAccess",
		"versionInfo",
		"rangeEnum",
		"loopEnum",
		"getProperty",
		"loadProperties",
		"saveProperties",
		"setProperty",
	};

	static String[] files  = {
		"pnuts/util/file",
		"pnuts/util/ls",
		"pnuts/util/class",
		"pnuts/util/shellUtil",
		"pnuts/util/zip",
		"pnuts/util/system",
		"pnuts/util/manifest",
		"pnuts/util/property",
		"pnuts/util/pack200"
	};

	static String[][] functions = {
		{ // pnuts/util/file
			"exists",
			"isDirectory",
			"canRead",
			"canWrite",
			"pwd",
			"chdir",
			"delete",
			"mkdir",
			"renameTo",
			"walkDirectory",
			"cat",
			"createTempFile",
			"copy"
		},
		{ // pnuts/util/ls
			"ls"
		},
		{ // pnuts/util/class
			"dumpclass",
			"supertypes"
		},
		{ // pnuts/util/shellUtil
			"shellExpand"
		},
		{ // pnuts/util/zip
			"readZip",
			"readZipEntries",
			"openZip",
			"writeZip",
			"writeZipEntries",
			"updateZip",
			"mergeZip",
			"extractZip"
		},
		{ // pnuts/util/system
			"system"
		},
		{ // pnuts/util/manifest
			"manifest"
		},
		// pnuts/util/property
		{
			"loadProperty"  // for backward compatibility
		},
		// pnuts/util/pack200
		{
			"pack200",
			"unpack200"
		}
	};

	static String[] requiredModules  = {
	    "pnuts.lib",
	    "pnuts.io",
	    "pnuts.multithread",
	    "pnuts.regex"
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

