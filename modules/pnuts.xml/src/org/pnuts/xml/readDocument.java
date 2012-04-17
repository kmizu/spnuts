/*
 * @(#)readDocument.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import pnuts.lang.*;
import org.pnuts.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import java.util.*;
import java.io.*;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

public class readDocument extends PnutsFunction {

	public readDocument(){
		super("readDocument");
	}

	public boolean defined(int nargs){
		return nargs >= 1 && nargs <= 4;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Object schema = null;
		Map properties = null;
		Object errorHandler;
		Object input;
		switch (nargs){
		case 4:
			schema = args[3];
		case 3:
			properties = (Map)args[2];
		case 2:
			errorHandler = args[1];
			break;
		case 1:
			errorHandler = Util.getDefaultErrorHandler(context);
			break;
		default:
			undefined(args, context);
			return null;
		}
		input = args[0];
		if (properties == null){
			properties = new LinkedHashMap();
		}
		if (schema != null){
			properties.put(Util.KEY_SCHEMA, schema);
		}
		DocumentBuilder builder = Util.getDocumentBuilder(properties, context);
		if (errorHandler != null){
			builder.setErrorHandler((ErrorHandler)Util.contentHandler(errorHandler, context));
		}
		try {
			return builder.parse(Util.inputSource(input, context));
		} catch (IOException e1){
			throw new PnutsException(e1, context);
		} catch (SAXException e2) {
			throw new PnutsException(e2, context);
		}
	}

	public String toString(){
		return "function readDocument(in {, errorHandler, {, properties {, schema }}} )";
	}
}
