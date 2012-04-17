/*
 * @(#)parseXML.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import pnuts.lang.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import javax.xml.parsers.*;
import org.xml.sax.helpers.DefaultHandler;

public class parseXML extends PnutsFunction {
	public parseXML(){
		super("parseXML");
	}

	public boolean defined(int nargs){
		return nargs >= 2 && nargs <= 4;
	}
	
	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Object input;
		Object handler;
		Map properties = null;
		Object schema = null;
		if (nargs == 2){
			input = args[0];
			handler = args[1];
			schema = null;
		} else if (nargs == 3){
			input = args[0];
			handler = args[1];
			properties = (Map)args[2];
		} else if (nargs == 4){
			input = args[0];
			handler = args[1];
			properties = (Map)args[2];
			schema = args[3];
		} else {
			undefined(args, context);
			return null;
		}
		if (properties == null){
			properties = new LinkedHashMap();
		}
		if (schema != null){
	 		properties.put(Util.KEY_SCHEMA, schema);
		}
		DefaultHandler defaultHandler;
		if (handler == null){
			defaultHandler = Util.getDefaultErrorHandler(context);
		} else {
			defaultHandler = (DefaultHandler)Util.contentHandler(handler, context);
		}
		try {
			SAXParser parser = Util.getSAXParser(properties, context);
			parser.parse(Util.inputSource(input, context), defaultHandler);
			return null;
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function parseXML(input, handler {, properties {, schema }})";
	}
}
