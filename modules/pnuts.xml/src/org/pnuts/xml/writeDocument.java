/*
 * @(#)writeDocument.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import pnuts.lang.*;
import org.pnuts.lib.PathHelper;
import org.pnuts.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import java.util.*;
import java.io.*;
import org.w3c.dom.Node;

public class writeDocument extends PnutsFunction {

	public writeDocument(){
		super("writeDocument");
	}

	public boolean defined(int nargs){
		return nargs >= 1 && nargs <= 3;
	}

	protected Object exec(Object[] args, Context context){
		Map props = null;
		Result result = new StreamResult(context.getWriter());
		Source source;
		Node node = null;
		int nargs = args.length;
		switch (nargs){
		case 3:
			props = (Map)args[2];
		case 2:
			Object arg1 = args[1];
			if (arg1 instanceof Result){
				result = (Result)arg1;
			} else if (arg1 instanceof String){
				result = new StreamResult(PathHelper.getFile((String)arg1, context));
			} else if (arg1 instanceof File){
				result = new StreamResult((File)arg1);
			} else if (arg1 instanceof OutputStream){
				result = new StreamResult((OutputStream)arg1);
			} else if (arg1 instanceof Writer){
				result = new StreamResult((Writer)arg1);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg1));
			}
		case 1:
			Object arg0 = args[0];
			if (arg0 instanceof Source){
				source = (Source)arg0;
			} else if (arg0 instanceof Node){
				node = (Node)arg0;
				source = new DOMSource(node);
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			break;
		default:
			undefined(args, context);
			return null;
		}
		Transformer trans = Util.getTransformer(props, context);
		try {
			trans.transform(source, result);
		} catch (TransformerException e){
			throw new PnutsException(e, context);
		}
		return null;
	}

	public String toString(){
		return "function writeDocument(doc {, out {, properties } } )";
	}
}
