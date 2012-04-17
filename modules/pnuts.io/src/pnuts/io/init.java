/*
 * @(#)init.java 1.4 05/05/25
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.io;

import pnuts.lang.Context;
import pnuts.ext.ModuleBase;

/**
 * Initialization of the pnuts.io module
 */
public class init extends ModuleBase {

	static String[] files  = {
		"pnuts/io/stream",
		"pnuts/io/gzip",
		"pnuts/io/encode"
	};

	static String[][] functions = {
		// pnuts/io/stream
		{
			"pipe",
			"stringReader",
			"stringWriter",
			"openByteArray",
			"openCharArray",
			"getByteArray",
			"getCharArray"
		},
		// pnuts/io/gzip
		{
			"zcat",
			"gzip"
		},
		// pnuts/io/encode
		{
			"base64encode",
			"base64decode",
			"uuencode",
			"uudecode",
			"unicodeToString",
			"stringToUnicode"
		}
	};

	static String[] javaFunctions = {
		"readBoolean",
		"readShort",
		"readUnsignedByte",
		"readUnsignedShort",
		"readInt",
		"readChar",
		"readLong",
		"readFloat",
		"readDouble",
		"readUTF",
		"writeInt",
		"writeBoolean",
		"writeShort",
		"writeChar",
		"writeLong",
		"writeFloat",
		"writeDouble",
		"writeChars",
		"writeUTF",
		"dataInput",
		"dataOutput",
		"read",
		"readText",
		"writeText",
		"readBytes",
		"writeBytes",
		"open",
		"reader",
		"writer",
		"openURL",
		"setCharacterEncoding",
		"getCharacterEncoding",
		"writeObject",
		"readObject",
		"translate"
	};

	static String[] requiredModules  = {
	    "pnuts.lib",
	    "pnuts.multithread"
	};

	protected String[] getRequiredModules(){
		return requiredModules;
	}

	protected String getPrefix(){
		return "org";
	}

	public Object execute(Context context){
		for (int i = 0; i < files.length; i++){
			autoload(functions[i], files[i], context);
		}
		for (int i = 0; i < javaFunctions.length; i++){
			autoloadFunction(javaFunctions[i], context);
		}
		return null;
	}
}
