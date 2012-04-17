/*
 * @(#)init.java 1.3 05/01/14
 *
 * Copyright (c) 2001-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.mail;

import pnuts.lang.Runtime;
import pnuts.lang.Package;
import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

public class init extends ModuleBase {

	static String[] files  = {
		"pnuts/mail/folder",
		"pnuts/mail/mail"
	};

	static String[][] functions = {
		{
			"listMailFolders",
			"openMailStore",
			"openMailFolder",
			"getMessages",
			"FETCH_ENVELOPE",
			"FETCH_FLAGS",
			"FETCH_CONTENT_INFO"
		},
		{
			"sendEmail",
			"emailAddress",
			"mimepart",
			"setMailCharset",
			"attachText",
			"attachFile",
			"attachURL"
		}
	};

	static String[] requiredModules  = {
	    "pnuts.lib"
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

