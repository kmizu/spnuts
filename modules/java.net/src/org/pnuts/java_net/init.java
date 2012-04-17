/*
 * @(#)init.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.java_net;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the java.util module
 */
public class init extends ModuleBase {

	static String[] classNames = {
		"URL",
		"URI",
		"Socket",
		"ServerSocket",
		"MulticastSocket",
		"Authenticator",
		"DatagramSocket",
		"DatagramPacket"
	};

	public Object execute(Context context){
		for (int i = 0; i < classNames.length; i++){
			autoloadClass("java.net", classNames[i], context);
		}
		return null;
	}
}

