/*
 * @(#)transformXSL.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.Node;

public class transformXSL extends PnutsFunction {
	public transformXSL(){
		super("transformXSL");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Result result;
		Node node = null;
		try {
			if (nargs == 2){
				node = Util.getDocumentBuilder(null, context).newDocument();
				result = new DOMResult(node);
			} else if (nargs == 3){
	 			result = Util.getResult(args[2], context);
			} else {
				undefined(args, context);
				return null;
			}
		 	Util.getTransformerXSL(args[1], context).transform(Util.streamSource(args[0], context),  result);
			return node;
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function transformXSL(xml, xsl {,  output })";
	}
}
