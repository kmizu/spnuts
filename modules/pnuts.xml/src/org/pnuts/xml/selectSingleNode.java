/*
 * selectSingleNode.java
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import com.sun.org.apache.xpath.internal.XPathAPI;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import org.w3c.dom.Node;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class selectSingleNode extends PnutsFunction {

	public selectSingleNode(){
		super("selectSingleNode");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		try {
			int nargs = args.length;
			if (nargs == 2){
				XPath xpath = XPathFactory.newInstance().newXPath();
				if (args[0] instanceof Node){
					return xpath.evaluate((String)args[1], args[0], XPathConstants.NODE);
				} else {
					return xpath.evaluate((String)args[1], Util.inputSource(args[0], context), XPathConstants.NODE);
				}
			} else if (nargs == 3){
				XPath xpath = XPathFactory.newInstance().newXPath();
				if (args[0] instanceof Node){
					return xpath.evaluate((String)args[1], args[0], XPathConstants.NODE);
				} else {
					return xpath.evaluate((String)args[1], Util.inputSource(args[0], context), XPathConstants.NODE);
				}
			} else {
				undefined(args, context);
				return null;
			}
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function selectSingleNode(Node, String {, Node })";
	}
}
