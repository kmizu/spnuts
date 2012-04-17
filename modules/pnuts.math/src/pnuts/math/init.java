/*
 * @(#)init.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.math;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.math module.
 */
public class init extends ModuleBase {

	static String[] javaFunctions = {
		"sin",
		"cos",
		"tan",
		"asin",
		"acos",
		"atan",
		"atan2",
		"exp",
		"log",
		"sqrt",
		"round",
		"pow",
		"ceil",
		"floor",
		"rint",
		"toRadians",
		"toDegrees",
		"IEEEremainder",
		"max",
		"min",
		"abs"
	};

	protected String getPrefix(){
		return "org";
	}

	public Object execute(Context context){
		context.clearPackages();
		for (int i = 0; i < javaFunctions.length; i++){
			autoloadFunction(javaFunctions[i], context);
		}
		return null;
	}
}
