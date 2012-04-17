/*
 * @(#)init.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.java_util;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the java.util module
 */
public class init extends ModuleBase {

	static String[] classNames = {
		"Date",
		"BitSet",
		"Calendar",
		"Hashtable",
		"Vector",
		"Stack",
		"Map",
		"List",
		"Set",
		"Collection",
		"Collections",
		"Arrays",
		"Random",
		"Locale",
		"TimeZone",
		"Enumeration",
		"Iterator"
	};

	public Object execute(Context context){
		for (int i = 0; i < classNames.length; i++){
			autoloadClass("java.util", classNames[i], context);
		}
		return null;
	}
}

