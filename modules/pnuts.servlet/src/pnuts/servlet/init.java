/*
 * init.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import pnuts.lang.Pnuts;
import pnuts.lang.Runtime;
import pnuts.lang.Package;
import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.servlet module
 */
public class init extends ModuleBase {

	static String[] files  = {
		"pnuts/servlet/escape",
		"pnuts/servlet/session",
		"pnuts/servlet/setup"
	};

	static String[][] functions = {
		{
			"unescape"
		},
		{
			"session",
		},
		{
			"setupPages",
			"setupActions"
		},
	};

	static String[] javaFunctions = {
		"getFile",
		"getCookie",
		"addCookie",
		"getInitParameter",
		"makeQueryString",
		"parseQueryString",
		"readMultipartRequest",
		"readGetParameters",
		"readPostParameters",
		"readParameters",
		"getParameter",
		"decodeURL",
		"encodeURL",
		"getURL",
		"getSession",
		"getSessionMap",
		"sendRedirect",
		"getRequest",
		"getResponse",
		"requestScope",
		"requestPath",
		"forward",
		"debug",
		"readDynamicPage",
		"convertDynamicPage",
		"escape",
		"sendPostRequest"
	};

	protected String getPrefix(){
		return "org";
	}

	public Object execute(Context context){
		try {
			context.usePackage("pnuts.tools");
			try {
				Class.forName("java.lang.CharSequence");
				context.usePackage("pnuts.nio");
			} catch (ClassNotFoundException e){
			}
//		context.clearPackages();
			for (int i = 0; i < files.length; i++){
				autoload(functions[i], files[i], context);
			}
			for (int i = 0; i < javaFunctions.length; i++){
				autoloadFunction(javaFunctions[i], context);
			}

		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
