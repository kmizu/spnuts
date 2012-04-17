/*
 * DefaultParseEnv.java
 * 
 * Copyright (c) 2004,2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import pnuts.lang.ParseEnvironment;
import pnuts.lang.ParseException;

public class DefaultParseEnv implements ParseEnvironment {
	static DefaultParseEnv instance = new DefaultParseEnv();

	private Object scriptSource;

	private DefaultParseEnv(){
	}

	private DefaultParseEnv(Object scriptSource){
	    this.scriptSource = scriptSource;
	}

	public static DefaultParseEnv getInstance() {
		return instance;
	}

	public static DefaultParseEnv getInstance(Object scriptSource) {
	    return new DefaultParseEnv(scriptSource);
	}


	public void handleParseException(ParseException e) throws ParseException {
		if (scriptSource != null){
			e.setScriptSource(scriptSource);
		}
		throw e;
	}
}
