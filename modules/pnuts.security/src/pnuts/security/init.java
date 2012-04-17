/*
 * @(#)init.java 1.3 05/01/14
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.security;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.security module
 */
public class init extends ModuleBase {

	static String[] files  = {
		"pnuts/security/security",
		"pnuts/security/cipher",
		"pnuts/security/digest",
		"pnuts/security/permission",
	};

	static String[][] functions = {
		{ // pnuts/util/security
			"getKeyStore",
			"getPublicKey",
			"getPrivateKey",
			"signObject",
			"verifyObject"
		},
		{ // pnuts/util/cipher
			"getSecretKey",
			"encrypt",
			"decrypt",
			"sealObject",
			"unsealObject"
		},
		{ // pnuts/util/digest
			"md5",
			"sha"
		},
		{
		    "permissions",
		    "secureFunc"
		}
	};

	static String[] requiredModules  = {
	    "pnuts.io",
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

