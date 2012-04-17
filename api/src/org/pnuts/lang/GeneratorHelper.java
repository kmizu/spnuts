/*
 * GeneratorHelper.java
 *
 * Copyright (c) 2005,2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.util.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;
import pnuts.compiler.*; // TODO

public class GeneratorHelper extends ScopeAnalyzer {
	private String identity;
	private ArrayList locals = new ArrayList();

	public GeneratorHelper(String identity){
		this.identity = identity;
	}

	protected void handleLocalVariable(SimpleNode node, Context context){
		locals.add(node);
	}

	public void resolve(){
		for (Iterator it = locals.iterator(); it.hasNext();){
			SimpleNode n = (SimpleNode)it.next();
			n.str = (n.str + "!" + identity).intern();
		}
	}

	public Object assignment(SimpleNode node, Context context){
		SimpleNode n0 = node.jjtGetChild(0);
		if (n0.id == PnutsParserTreeConstants.JJTIDNODE){
			locals.add(n0);
		}
		return super.assignment(node, context);
	}

	public Object foreachStatement(SimpleNode node, Context context) {
		locals.add(node);
		return super.foreachStatement(node, context);
	}

	public Object forStatement(SimpleNode node, Context context){
		SimpleNode n = node.jjtGetChild(0);
		if (n.id == PnutsParserTreeConstants.JJTFORENUM) {
			locals.add(n);
		} else if (n.id == PnutsParserTreeConstants.JJTFORINIT){
			for (int i = 0; i < n.jjtGetNumChildren(); i++){
				locals.add(n.jjtGetChild(i));
			}
		}
		return super.forStatement(node, context);
	}

	public Object functionStatement(SimpleNode node, Context context){
		super.functionStatement(node, context);
		SimpleNode params = node.jjtGetChild(0);
		for (int i = 0; i < params.jjtGetNumChildren(); i++){
			locals.add(params.jjtGetChild(i));
		}
		return null;
	}

	public static SimpleNode renameLocals(SimpleNode node, String identity) {
		org.pnuts.lang.GeneratorHelper gh = 
			new org.pnuts.lang.GeneratorHelper(identity);
		gh.analyze(node.jjtGetParent());
		gh.resolve();
		return node;
	}


    /*
     * Insert var_0 = try {tmpVar[0]} catch (IndexOutOfBoundsException e){}); .... into blockNode
     */
    public static SimpleNode expandMultiAssign(String[] vars, String tmpVar, SimpleNode blockNode){
	List children = new ArrayList();
	int nc = blockNode.jjtGetNumChildren();
	for (int i = 0; i < vars.length; i++){
	    SimpleNode lhs = new SimpleNode(PnutsParserTreeConstants.JJTIDNODE);
	    lhs.str = vars[i];
	    SimpleNode tryNode = new SimpleNode(PnutsParserTreeConstants.JJTTRYSTATEMENT);
	    SimpleNode tryBlock = new SimpleNode(PnutsParserTreeConstants.JJTBLOCK);
	    SimpleNode catchBlock = new SimpleNode(PnutsParserTreeConstants.JJTCATCHBLOCK);
	    catchBlock.str = "e";
	    SimpleNode emptyBlock = new SimpleNode(PnutsParserTreeConstants.JJTBLOCK);
	    SimpleNode className = new SimpleNode(PnutsParserTreeConstants.JJTCLASSNAME);
	    SimpleNode pkg = new SimpleNode(PnutsParserTreeConstants.JJTPACKAGE);
	    pkg.str = "IndexOutOfBoundsException";

	    SimpleNode rhs = new SimpleNode(PnutsParserTreeConstants.JJTINDEXNODE);
	    SimpleNode id = new SimpleNode(PnutsParserTreeConstants.JJTIDNODE);
	    id.str = tmpVar;
	    SimpleNode idx = new SimpleNode(PnutsParserTreeConstants.JJTINTEGERNODE);
	    idx.str = String.valueOf(i);
	    try {
		idx.info = Runtime.parseInt(idx.str);
	    } catch (ParseException pe){
		continue;
	    }
	    rhs.jjtAddChild(idx, 1);
	    rhs.jjtAddChild(id, 0);

	    className.jjtAddChild(pkg, 0);
	    catchBlock.jjtAddChild(className, 0);
	    catchBlock.jjtAddChild(emptyBlock, 1);
	    tryNode.jjtAddChild(tryBlock, 0);
	    tryNode.jjtAddChild(catchBlock, 1);
	    tryBlock.jjtAddChild(rhs, 0);

	    SimpleNode assign = new SimpleNode(PnutsParserTreeConstants.JJTASSIGNMENT);
	    assign.jjtAddChild(tryNode, 1);
	    assign.jjtAddChild(lhs, 0);
	    children.add(assign);
	}
	for (int i = 0; i < nc; i++){
	    children.add(blockNode.jjtGetChild(i));
	}
	SimpleNode block = new SimpleNode(PnutsParserTreeConstants.JJTBLOCK);
	int size = children.size();
	for (int i = 0; i < size; i++){
	    int pos = size - i - 1;
	    SimpleNode node = (SimpleNode)children.get(pos);
	    block.jjtAddChild(node, pos);
	}
	return block;
    }

//	public static void main(String[] args) throws Exception {
//		new GeneratorHelper("ZZZ").analyze(new java.io.FileReader(args[0]));
//	}
}
