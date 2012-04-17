/*
 * @(#)traverseDocument.java 1.4 05/01/22
 *
 * Copyright (c) 2004,2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.xml.sax.ContentHandler;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import pnuts.lang.Context;
import java.util.*;
import org.w3c.dom.Document;

public class traverseDocument extends PnutsFunction {

	public traverseDocument(){
		super("traverseDocument");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	static Result getTransformResult(Object r, Context context){
		if (r instanceof Result){
			return (Result)r;
		} else if (r instanceof Map){
			return new SAXResult(Util.contentHandler(r, context));
		} else if (r instanceof ContentHandler){
			return new SAXResult((ContentHandler)r);
		} else {
			throw new IllegalArgumentException(String.valueOf(r));
		}
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 2 && nargs != 3){
			undefined(args, context);
			return null;
		}
		Object doc = args[0];
		Node node = (Node)doc;
		if (!(doc instanceof Node)){
			throw new IllegalArgumentException(String.valueOf(doc));
		} else if (doc instanceof Document){
			node = ((Document)doc).getDocumentElement();
		}
		Object a1 = args[1];
		Object a2 = null;
		if (nargs == 3){
			a2 = args[2];
		}
		if (a1 instanceof PnutsFunction){
			if (doc instanceof Document){
				doc = ((Document)doc).getDocumentElement();
			} else if (!(doc instanceof Element)){
				throw new IllegalArgumentException(String.valueOf(doc));
			}
			PnutsFunction f1 = (PnutsFunction)a1;
			f1.call(new Object[]{node}, context);
			NodeList nodes = node.getChildNodes();
			int len = nodes.getLength();
			if (nargs == 3){
				for (int i = 0; i < len; i++){
					Node n = nodes.item(i);
					if (n instanceof Element){
						exec(new Object[]{n, a1, a2}, context);
					}
				}
				PnutsFunction f2 = (PnutsFunction)a2;
				f2.call(new Object[]{node}, context);
			} else {
				for (int i = 0; i < len; i++){
					Node n = nodes.item(i);
					if (n instanceof Element){
						exec(new Object[]{n, a1}, context);
					}
				}
			}
		} else {
			Transformer trans = Util.getTransformer(null, context);
			try {
				trans.transform(new DOMSource(node), getTransformResult(a1, context));
			} catch (TransformerException e){
				throw new PnutsException(e, context);
			}
		}
		return null;
	}

	public String toString(){
		return "function traverseDocument(doc, enterFunc {, exitFunc })";
	}
}
