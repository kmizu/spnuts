/*
 * selectNodeList.java
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

public class selectNodeList extends PnutsFunction {

	public selectNodeList(){
		super("selectNodeList");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		try {
			int nargs = args.length;
			if (nargs == 2){
				Object target = args[0];
				String expr = (String)args[1];
				XPath xpath = XPathFactory.newInstance().newXPath();
				if (target instanceof Node){
					return xpath.evaluate(expr, target, XPathConstants.NODESET);
				} else {
					return xpath.evaluate(expr, Util.inputSource(target, context), XPathConstants.NODESET);
				}
			} else if (nargs == 3){
				Object target = args[0];
				String expr = (String)args[1];
				XPath xpath = XPathFactory.newInstance().newXPath();
				if (target instanceof Node){
					return xpath.evaluate(expr, target, XPathConstants.NODESET);
				} else {
					return xpath.evaluate(expr, Util.inputSource(target, context), XPathConstants.NODESET);
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
		return "function selectNodeList(Node, xpathExpression)";
	}
}
