/*
 * @(#)ConstraintsTransformer.java 1.2 04/12/06
 * 
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import pnuts.lang.SimpleNode;
import pnuts.lang.PnutsParserTreeConstants;

public class ConstraintsTransformer {

	protected ConstraintsTransformer() {
	}

	public static boolean isPredicate(SimpleNode node) {
		switch (node.id) {
		case PnutsParserTreeConstants.JJTLOGANDNODE:
		case PnutsParserTreeConstants.JJTLOGORNODE:
		case PnutsParserTreeConstants.JJTLOGNOTNODE:
		case PnutsParserTreeConstants.JJTEQUALNODE:
		case PnutsParserTreeConstants.JJTNOTEQNODE:
		case PnutsParserTreeConstants.JJTINSTANCEOFEXPRESSION:
		case PnutsParserTreeConstants.JJTLTNODE:
		case PnutsParserTreeConstants.JJTGTNODE:
		case PnutsParserTreeConstants.JJTLENODE:
		case PnutsParserTreeConstants.JJTGENODE:
			return true;
		default:
			return false;
		}
	}

	static void insertId(SimpleNode predicateNode, SimpleNode idNode) {
		switch (predicateNode.id) {
		case PnutsParserTreeConstants.JJTLOGANDNODE:
		case PnutsParserTreeConstants.JJTLOGORNODE:
			insertId(predicateNode.jjtGetChild(0), idNode);
			insertId(predicateNode.jjtGetChild(1), idNode);
			break;
		case PnutsParserTreeConstants.JJTLOGNOTNODE:
		case PnutsParserTreeConstants.JJTEQUALNODE:
		case PnutsParserTreeConstants.JJTNOTEQNODE:
		case PnutsParserTreeConstants.JJTINSTANCEOFEXPRESSION:
		case PnutsParserTreeConstants.JJTLTNODE:
		case PnutsParserTreeConstants.JJTGTNODE:
		case PnutsParserTreeConstants.JJTLENODE:
		case PnutsParserTreeConstants.JJTGENODE:
			SimpleNode left = predicateNode.jjtGetChild(0);
			while (left != null) {
				if (left.id == PnutsParserTreeConstants.JJTIDNODE) {
					SimpleNode parent = left.jjtGetParent();
					SimpleNode member = new SimpleNode(
							PnutsParserTreeConstants.JJTMEMBERNODE);
					member.jjtSetParent(parent);
					member.jjtAddChild(left, 0);
					member.str = left.str;
					left.str = ID_SYMBOL;
					member.beginLine = parent.beginLine;
					member.endLine = parent.endLine;
					left.jjtSetParent(member);
					parent.jjtAddChild(member, 0);
					break;
				}
				if (left.jjtGetNumChildren() > 0) {
					left = left.jjtGetChild(0);
				} else {
					break;
				}
			}
		}
	}

	final static String ID_SYMBOL = "i".intern();

	public static SimpleNode buildFunc(SimpleNode pred) {
		SimpleNode fn = new SimpleNode(
				PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT);
		SimpleNode p = new SimpleNode(PnutsParserTreeConstants.JJTPARAM);
		p.str = ID_SYMBOL;
		SimpleNode pl = new SimpleNode(PnutsParserTreeConstants.JJTPARAMLIST);
		pl.jjtAddChild(p, 0);
		fn.jjtAddChild(pl, 0);

		SimpleNode blockNode = new SimpleNode(PnutsParserTreeConstants.JJTBLOCK);
		fn.jjtAddChild(blockNode, 1);
		blockNode.jjtAddChild(pred, 0);

		SimpleNode idNode = new SimpleNode(PnutsParserTreeConstants.JJTIDNODE);
		idNode.jjtSetParent(blockNode);
		idNode.str = ID_SYMBOL;

		if (isPredicate(pred)) {
			insertId(pred, idNode);
		}
		return fn;
	}

	public static SimpleNode buildExpression(SimpleNode startSet) {
		if (startSet.jjtGetNumChildren() != 1) {
			throw new IllegalArgumentException();
		}
		SimpleNode el = startSet.jjtGetChild(0);
		if (el.jjtGetNumChildren() != 1) {
			throw new IllegalArgumentException();
		}
		SimpleNode pred = el.jjtGetChild(0);
		SimpleNode fn = buildFunc(pred);
		startSet.jjtAddChild(el, 0);
		el.jjtAddChild(fn, 0);
		fn.jjtSetParent(el);
		el.jjtSetParent(startSet);
		return startSet;
	}
}
