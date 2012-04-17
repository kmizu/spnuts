/*
 * %W% %E%
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import pnuts.compiler.*;
import pnuts.lang.*;
import java.util.*;

public class DeclarationHelper extends ScopeAnalyzer {

	public static void preprocess(SimpleNode node){
		new DeclarationHelper().analyze(node);
	}

	static boolean isConditionalNode(SimpleNode node){
		return
			node.id == PnutsParserTreeConstants.JJTIFSTATEMENT ||
			node.id == PnutsParserTreeConstants.JJTSWITCHSTATEMENT ||
			node.id == PnutsParserTreeConstants.JJTWHILESTATEMENT || 
			node.id == PnutsParserTreeConstants.JJTDOSTATEMENT || 
			node.id == PnutsParserTreeConstants.JJTFORSTATEMENT || 
			node.id == PnutsParserTreeConstants.JJTFOREACHSTATEMENT;
	}

	protected void declared(SimpleNode node, Context context, String symbol){
		SimpleNode n = node.jjtGetParent();
		while (n != null && n.id != PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT){
			n = n.jjtGetParent();
		}
		if (n != null){ // node is in local scope
			n = node;
//			Set symbols = null;
			SimpleNode block = null;
			while (n != null && n.id != PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT){
				if (n.id == PnutsParserTreeConstants.JJTBLOCK || 
				    n.id == PnutsParserTreeConstants.JJTSWITCHBLOCK)
				{
					block = n;
				}
				if (isConditionalNode(n)){
					Set s = (Set)n.getAttribute("declaredSymbols");
					if (s == null){
						s = new HashSet();
						n.setAttribute("declaredSymbols", s);
						/*
						if (symbols == null){
							symbols = s = new HashSet();
						} else {
							s = symbols;
						}
						*/
					}
//					node.setAttribute("declaredSymbols", s);
					s.add(symbol);

//					if (block != null){
						s = (Set)block.getAttribute("declaredSymbols");
						if (s == null){
							s = new HashSet();
							block.setAttribute("declaredSymbols", s);
						}
						s.add(symbol);
//					}
 				}
				n = n.jjtGetParent();
			}
		}
	}
}
