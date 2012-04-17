/*
 * @(#)Visitor.java 1.3 05/02/17
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * This is the interface of Visit operations for a syntax tree.
 * 
 * @see pnuts.lang.Pnuts#accept(pnuts.lang.Visitor, pnuts.lang.Context)
 * @see pnuts.lang.PnutsFunction#accept(int, pnuts.lang.Visitor,
 *      pnuts.lang.Context)
 */
public interface Visitor {
	public Object start(SimpleNode node, Context context);

	public Object startSet(SimpleNode node, Context context);

	public Object expressionList(SimpleNode node, Context context);

	public Object idNode(SimpleNode node, Context context);

	public Object global(SimpleNode node, Context context);

	public Object arrayType(SimpleNode node, Context context);

	public Object castExpression(SimpleNode node, Context context);

	public Object listElements(SimpleNode node, Context context);

	public Object classNode(SimpleNode node, Context context);

	public Object className(SimpleNode node, Context context);

	public Object indexNode(SimpleNode node, Context context);

	public Object rangeNode(SimpleNode node, Context context);

	public Object methodNode(SimpleNode node, Context context);

	public Object staticMethodNode(SimpleNode node, Context context);

	public Object memberNode(SimpleNode node, Context context);

	public Object staticMemberNode(SimpleNode node, Context context);

	public Object applicationNode(SimpleNode node, Context context);

	public Object integerNode(SimpleNode node, Context context);

	public Object floatingNode(SimpleNode node, Context context);

	public Object characterNode(SimpleNode node, Context context);

	public Object stringNode(SimpleNode node, Context context);

	public Object trueNode(SimpleNode node, Context context);

	public Object falseNode(SimpleNode node, Context context);

	public Object nullNode(SimpleNode node, Context context);

	public Object assignment(SimpleNode node, Context context);

	public Object assignmentTA(SimpleNode node, Context context);

	public Object assignmentMA(SimpleNode node, Context context);

	public Object assignmentDA(SimpleNode node, Context context);

	public Object assignmentPA(SimpleNode node, Context context);

	public Object assignmentSA(SimpleNode node, Context context);

	public Object assignmentLA(SimpleNode node, Context context);

	public Object assignmentRA(SimpleNode node, Context context);

	public Object assignmentRAA(SimpleNode node, Context context);

	public Object assignmentAA(SimpleNode node, Context context);

	public Object assignmentEA(SimpleNode node, Context context);

	public Object assignmentOA(SimpleNode node, Context context);

	public Object orNode(SimpleNode node, Context context);

	public Object andNode(SimpleNode node, Context context);

	public Object xorNode(SimpleNode node, Context context);

	public Object equalNode(SimpleNode node, Context context);

	public Object notEqNode(SimpleNode node, Context context);

	public Object instanceofExpression(SimpleNode node, Context context);

	public Object ltNode(SimpleNode node, Context context);

	public Object gtNode(SimpleNode node, Context context);

	public Object leNode(SimpleNode node, Context context);

	public Object geNode(SimpleNode node, Context context);

	public Object shiftLeftNode(SimpleNode node, Context context);

	public Object shiftRightNode(SimpleNode node, Context context);

	public Object shiftArithmeticNode(SimpleNode node, Context context);

	public Object addNode(SimpleNode node, Context context);

	public Object subtractNode(SimpleNode node, Context context);

	public Object multNode(SimpleNode node, Context context);

	public Object divideNode(SimpleNode node, Context context);

	public Object modNode(SimpleNode node, Context context);

	public Object negativeNode(SimpleNode node, Context context);

	public Object preIncrNode(SimpleNode node, Context context);

	public Object preDecrNode(SimpleNode node, Context context);

	public Object logAndNode(SimpleNode node, Context context);

	public Object logOrNode(SimpleNode node, Context context);

	public Object logNotNode(SimpleNode node, Context context);

	public Object notNode(SimpleNode node, Context context);

	public Object postIncrNode(SimpleNode node, Context context);

	public Object postDecrNode(SimpleNode node, Context context);

	public Object breakNode(SimpleNode node, Context context);

	public Object continueNode(SimpleNode node, Context context);

	public Object returnNode(SimpleNode node, Context context);

	public Object yieldNode(SimpleNode node, Context context);

	public Object blockNode(SimpleNode node, Context context);

	public Object ifStatement(SimpleNode node, Context context);

	public Object whileStatement(SimpleNode node, Context context);

	public Object doStatement(SimpleNode node, Context context);

	public Object forStatement(SimpleNode node, Context context);

	public Object foreachStatement(SimpleNode node, Context context);

	public Object switchStatement(SimpleNode node, Context context);

	public Object switchBlock(SimpleNode node, Context context);

	public Object functionStatement(SimpleNode node, Context context);

	public Object catchNode(SimpleNode node, Context context);

	public Object throwNode(SimpleNode node, Context context);

	public Object catchBlock(SimpleNode node, Context context);

	public Object finallyNode(SimpleNode node, Context context);

	public Object packageNode(SimpleNode node, Context context);

	public Object importNode(SimpleNode node, Context context);

	public Object tryStatement(SimpleNode node, Context context);

	public Object newNode(SimpleNode node, Context context);

	public Object mapNode(SimpleNode node, Context context);

	public Object ternary(SimpleNode node, Context context);

	public Object classScript(SimpleNode node, Context context);

	public Object classDef(SimpleNode node, Context context);

	public Object classDefBody(SimpleNode node, Context context);

	public Object methodDef(SimpleNode node, Context context);

	public Object beanDef(SimpleNode node, Context context);
}
