/*
 * @(#)Preprocessor.java 1.4 05/05/09
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import pnuts.lang.Context;
import pnuts.lang.SimpleNode;
import pnuts.lang.Visitor;
import pnuts.lang.PnutsParserTreeConstants;

/**
 * Preprocessor
 *
 * This visitor translates AST to a modified AST, so that
 * function calls could be done faster, by resolving the
 * function outside a loop;
 *
 * The translation occurs when:
 * (1) the call is in a local scope (in a function)
 * (2) the call is in a loop (while, for, or foreach) or a nested function
 *
 * e.g. before:
 * <pre>
 * function foo(){
 *   for (i = 0; i < 100; i++){
 *     println("hello")
 *   }
 * }
 * </pre>
 *
 * after:
 * <pre>
 * function foo(){
 *   println = println
 *   for (i = 0; i < 100; i++){
 *     println("hello")
 *   }
 * }
 * </pre>
 *
 */
class Preprocessor extends ScopeAnalyzer {

	protected boolean isTargetIdNode(SimpleNode node, Context context){
		SimpleNode parent = node.jjtGetParent();
		if (parent == null){
			return false;
		}
		int id = parent.id;
		if (id == PnutsParserTreeConstants.JJTAPPLICATIONNODE ||
		    id == PnutsParserTreeConstants.JJTSTATICMETHODNODE ||
		    id == PnutsParserTreeConstants.JJTMEMBERNODE)
		{
		    TranslateContext cc = (TranslateContext)context;
		    return cc.env.parent != null;
		}
		return false;
	}

	protected void handleFreeVariable(SimpleNode node, Context context){
		TranslateContext cc = (TranslateContext)context;
		SimpleNode n = node.jjtGetParent();
		String symbol = node.str;
		while (n != null){
			SimpleNode p = n.jjtGetParent();
			if (p == null){
				break;
			}
			if (p.id == PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT){
				if (cc.env.parent.parent == null){
					cc.addToFreeVarSet(symbol);
					break;
				} else {
				    break;
				}
			} else if (p.id == PnutsParserTreeConstants.JJTWHILESTATEMENT ||
				   p.id == PnutsParserTreeConstants.JJTDOSTATEMENT)
			{
				cc.addToFreeVarSet(symbol);
				break;
			} else if (p.id == PnutsParserTreeConstants.JJTFORSTATEMENT){
				if (p.jjtGetChild(0).str != symbol){
					cc.addToFreeVarSet(symbol);
				}
				break;
			} else if (p.id == PnutsParserTreeConstants.JJTFOREACHSTATEMENT){
				if (p.str != symbol){
					cc.addToFreeVarSet(symbol);
				}
				break;
			}
			n = p;
		}
	}

	public Object tryStatement(SimpleNode node, Context context){
		super.tryStatement(node, context);
		int n = node.jjtGetNumChildren();
		for (int i = 1; i < n; i++){
			acceptChildren(node.jjtGetChild(i), context);
		}
		while (node != null){
			if (node.id == PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT){
				break;
			}
			node.setAttribute("hasTryStatement", Boolean.TRUE);
			node = node.jjtGetParent();
		}
		return null;
	}

}
