/*
 * SimpleNode.java
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;

/**
 * This class respresents a node of AST.
 */
public class SimpleNode implements Serializable, Cloneable {

	static final long serialVersionUID = 2950908047102096822L;

	/**
	 * @serial
	 */
	protected SimpleNode parent;

	/**
	 * @serial
	 */
	protected SimpleNode[] children;

	/**
	 * @serial
	 */
	public int id;

    int toplevel;

	public SimpleNode(int i) {
		id = i;
	}

	/**
	 * public void jjtOpen() { }
	 * 
	 * public void jjtClose() { }
	 */

	public void jjtSetParent(SimpleNode n) {
		parent = n;
	}

	public SimpleNode jjtGetParent() {
		return parent;
	}

	public void jjtAddChild(SimpleNode n, int i) {
		if (children == null) {
			children = new SimpleNode[i + 1];
		} else if (i >= children.length) {
			SimpleNode c[] = new SimpleNode[i + 1];
			System.arraycopy(children, 0, c, 0, children.length);
			this.children = c;
		}
		children[i] = n;
	}

	public final SimpleNode jjtGetChild(int i) {
		if (children == null) {
			System.out.println("children == null : " + id + ", " + str + ", "
					+ i);
		}
		return children[i];
	}

	public final int jjtGetNumChildren() {
		return (children == null) ? 0 : children.length;
	}

	public void clearChildren(){
		children = null;
	}

	/**/
	public String toString() {
		return PnutsParserTreeConstants.jjtNodeName[id];
	}

	public String toString(String prefix) {
		return prefix + PnutsParserTreeConstants.jjtNodeName[id];
	}

	public void dump(String prefix) {
		System.out.println(toString(prefix));
		if (children != null) {
			for (int i = 0; i < children.length; ++i) {
				SimpleNode n = (SimpleNode) children[i];
				if (n != null) {
					n.dump(prefix + " ");
				}
			}
		}
	}

	public String unparse(){
		StringBuffer sb = new StringBuffer();
		Visitor v = new org.pnuts.lang.UnparseVisitor(sb);
		accept(v, null);
		return sb.toString();
	}

	/**/

	// pnuts specific attributes
	/**
	 * @serial
	 */
	public String str;

	/**
	 * @serial
	 */
	public int beginLine;

	/**
	 * @serial
	 */
	public int beginColumn;

	/**
	 * @serial
	 */
	public int endLine;

	/**
	 * Value of the node. Act as cache.
	 */
	transient Object value;

	/**
	 * @serial
	 */
	public Object info;

	transient Map attribute;

	/**
	 * Gets an attribute value
	 *
	 * @param key the attribute name
	 * @return the attribute value
	 */
	public Object getAttribute(String key){
		if (attribute == null){
			return null;
		} else {
			return attribute.get(key);
		}
	}

	/**
	 * Sets an attribute value for the specified key
	 *
	 * @param key the attribute name
	 * @param value the value
	 */
	public void setAttribute(String key, Object value){
		if (attribute == null){
			attribute = new HashMap();
		}
		attribute.put(key, value);
	}

	/**
	 * dispatch the instance to a Visitor
	 */
	public Object accept(Visitor visitor, Context context) {
	    if (context != null && !context.eval) {
			context.updateLine(this);
		}

		switch (id) {
		case PnutsParserTreeConstants.JJTSTART:
			return visitor.start(this, context);
		case PnutsParserTreeConstants.JJTSTARTSET:
			return visitor.startSet(this, context);
		case PnutsParserTreeConstants.JJTEXPRESSIONLIST:
			return visitor.expressionList(this, context);
		case PnutsParserTreeConstants.JJTIDNODE:
			return visitor.idNode(this, context);
		case PnutsParserTreeConstants.JJTGLOBAL:
			return visitor.global(this, context);
		case PnutsParserTreeConstants.JJTARRAYTYPE:
			return visitor.arrayType(this, context);
		case PnutsParserTreeConstants.JJTLISTELEMENTS:
			return visitor.listElements(this, context);
		case PnutsParserTreeConstants.JJTCASTEXPRESSION:
			return visitor.castExpression(this, context);
		case PnutsParserTreeConstants.JJTCLASS: // fall-thru
		case PnutsParserTreeConstants.JJTCLASSEXPR:
			return visitor.classNode(this, context);
		case PnutsParserTreeConstants.JJTINDEXNODE:
			return visitor.indexNode(this, context);
		case PnutsParserTreeConstants.JJTRANGENODE:
			return visitor.rangeNode(this, context);
		case PnutsParserTreeConstants.JJTMETHODNODE:
			return visitor.methodNode(this, context);
		case PnutsParserTreeConstants.JJTSTATICMETHODNODE:
			return visitor.staticMethodNode(this, context);
		case PnutsParserTreeConstants.JJTMEMBERNODE:
			return visitor.memberNode(this, context);
		case PnutsParserTreeConstants.JJTSTATICMEMBERNODE:
			return visitor.staticMemberNode(this, context);
		case PnutsParserTreeConstants.JJTAPPLICATIONNODE:
			return visitor.applicationNode(this, context);
		case PnutsParserTreeConstants.JJTINTEGERNODE:
			return visitor.integerNode(this, context);
		case PnutsParserTreeConstants.JJTFLOATINGNODE:
			return visitor.floatingNode(this, context);
		case PnutsParserTreeConstants.JJTCHARACTERNODE:
			return visitor.characterNode(this, context);
		case PnutsParserTreeConstants.JJTSTRINGNODE:
			return visitor.stringNode(this, context);
		case PnutsParserTreeConstants.JJTTRUENODE:
			return visitor.trueNode(this, context);
		case PnutsParserTreeConstants.JJTFALSENODE:
			return visitor.falseNode(this, context);
		case PnutsParserTreeConstants.JJTNULLNODE:
			return visitor.nullNode(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENT:
			return visitor.assignment(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTTA:
			return visitor.assignmentTA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTMA:
			return visitor.assignmentMA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTDA:
			return visitor.assignmentDA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTPA:
			return visitor.assignmentPA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTSA:
			return visitor.assignmentSA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTLA:
			return visitor.assignmentLA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTRA:
			return visitor.assignmentRA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTRAA:
			return visitor.assignmentRAA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTAA:
			return visitor.assignmentAA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTEA:
			return visitor.assignmentEA(this, context);
		case PnutsParserTreeConstants.JJTASSIGNMENTOA:
			return visitor.assignmentOA(this, context);
		case PnutsParserTreeConstants.JJTTERNARYNODE:
			return visitor.ternary(this, context);
		case PnutsParserTreeConstants.JJTORNODE:
			return visitor.orNode(this, context);
		case PnutsParserTreeConstants.JJTANDNODE:
			return visitor.andNode(this, context);
		case PnutsParserTreeConstants.JJTXORNODE:
			return visitor.xorNode(this, context);
		case PnutsParserTreeConstants.JJTLOGORNODE:
			return visitor.logOrNode(this, context);
		case PnutsParserTreeConstants.JJTLOGANDNODE:
			return visitor.logAndNode(this, context);
		case PnutsParserTreeConstants.JJTLOGNOTNODE:
			return visitor.logNotNode(this, context);
		case PnutsParserTreeConstants.JJTEQUALNODE:
			return visitor.equalNode(this, context);
		case PnutsParserTreeConstants.JJTNOTEQNODE:
			return visitor.notEqNode(this, context);
		case PnutsParserTreeConstants.JJTINSTANCEOFEXPRESSION:
			return visitor.instanceofExpression(this, context);
		case PnutsParserTreeConstants.JJTLTNODE:
			return visitor.ltNode(this, context);
		case PnutsParserTreeConstants.JJTGTNODE:
			return visitor.gtNode(this, context);
		case PnutsParserTreeConstants.JJTLENODE:
			return visitor.leNode(this, context);
		case PnutsParserTreeConstants.JJTGENODE:
			return visitor.geNode(this, context);
		case PnutsParserTreeConstants.JJTSHIFTLEFTNODE:
			return visitor.shiftLeftNode(this, context);
		case PnutsParserTreeConstants.JJTSHIFTRIGHTNODE:
			return visitor.shiftRightNode(this, context);
		case PnutsParserTreeConstants.JJTSHIFTARITHMETICNODE:
			return visitor.shiftArithmeticNode(this, context);
		case PnutsParserTreeConstants.JJTADDNODE:
			return visitor.addNode(this, context);
		case PnutsParserTreeConstants.JJTSUBTRACTNODE:
			return visitor.subtractNode(this, context);
		case PnutsParserTreeConstants.JJTMULTNODE:
			return visitor.multNode(this, context);
		case PnutsParserTreeConstants.JJTDIVIDENODE:
			return visitor.divideNode(this, context);
		case PnutsParserTreeConstants.JJTMODNODE:
			return visitor.modNode(this, context);
		case PnutsParserTreeConstants.JJTNEGATIVENODE:
			return visitor.negativeNode(this, context);
		case PnutsParserTreeConstants.JJTPREINCRNODE:
			return visitor.preIncrNode(this, context);
		case PnutsParserTreeConstants.JJTPREDECRNODE:
			return visitor.preDecrNode(this, context);
		case PnutsParserTreeConstants.JJTNOTNODE:
			return visitor.notNode(this, context);
		case PnutsParserTreeConstants.JJTPOSTINCRNODE:
			return visitor.postIncrNode(this, context);
		case PnutsParserTreeConstants.JJTPOSTDECRNODE:
			return visitor.postDecrNode(this, context);
		case PnutsParserTreeConstants.JJTBREAK:
			return visitor.breakNode(this, context);
		case PnutsParserTreeConstants.JJTCONTINUE:
			return visitor.continueNode(this, context);
		case PnutsParserTreeConstants.JJTRETURN:
			return visitor.returnNode(this, context);
		case PnutsParserTreeConstants.JJTYIELD:
			return visitor.yieldNode(this, context);
		case PnutsParserTreeConstants.JJTCATCHNODE:
			return visitor.catchNode(this, context);
		case PnutsParserTreeConstants.JJTTHROWNODE:
			return visitor.throwNode(this, context);
		case PnutsParserTreeConstants.JJTFINALLYNODE:
			return visitor.finallyNode(this, context);
		case PnutsParserTreeConstants.JJTBLOCK:
			return visitor.blockNode(this, context);
		case PnutsParserTreeConstants.JJTIFSTATEMENT:
			return visitor.ifStatement(this, context);
		case PnutsParserTreeConstants.JJTDOSTATEMENT:
			return visitor.doStatement(this, context);
		case PnutsParserTreeConstants.JJTWHILESTATEMENT:
			return visitor.whileStatement(this, context);
		case PnutsParserTreeConstants.JJTFORSTATEMENT:
			return visitor.forStatement(this, context);
		case PnutsParserTreeConstants.JJTFOREACHSTATEMENT:
			return visitor.foreachStatement(this, context);
		case PnutsParserTreeConstants.JJTSWITCHSTATEMENT:
			return visitor.switchStatement(this, context);
		case PnutsParserTreeConstants.JJTSWITCHBLOCK:
			return visitor.switchBlock(this, context);
		case PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT:
			return visitor.functionStatement(this, context);
		case PnutsParserTreeConstants.JJTTRYSTATEMENT:
			return visitor.tryStatement(this, context);
		case PnutsParserTreeConstants.JJTCATCHBLOCK:
			return visitor.catchBlock(this, context);
		case PnutsParserTreeConstants.JJTIMPORT:
			return visitor.importNode(this, context);
		case PnutsParserTreeConstants.JJTPACKAGESTATEMENT:
			return visitor.packageNode(this, context);
		case PnutsParserTreeConstants.JJTNEW:
			return visitor.newNode(this, context);
		case PnutsParserTreeConstants.JJTMAPELEMENTS:
			return visitor.mapNode(this, context);
		case PnutsParserTreeConstants.JJTCLASSNAME:
			return visitor.className(this, context);
		case PnutsParserTreeConstants.JJTCLASSSCRIPT:
			return visitor.classScript(this, context);
		case PnutsParserTreeConstants.JJTCLASSDEF:
			return visitor.classDef(this, context);
		case PnutsParserTreeConstants.JJTCLASSDEFBODY:
			return visitor.classDefBody(this, context);
		case PnutsParserTreeConstants.JJTMETHODDEF:
			return visitor.methodDef(this, context);
		case PnutsParserTreeConstants.JJTBEANDEF:
			return visitor.beanDef(this, context);
		case PnutsParserTreeConstants.JJTPACKAGE:
		case PnutsParserTreeConstants.JJTELSEIFNODE:
		case PnutsParserTreeConstants.JJTELSENODE:
		case PnutsParserTreeConstants.JJTSWITCHLABEL:
		case PnutsParserTreeConstants.JJTPARAMLIST:
		case PnutsParserTreeConstants.JJTPARAM:
		default:
			return null;
		}
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		switch (id) {
		case PnutsParserTreeConstants.JJTIDNODE:
		case PnutsParserTreeConstants.JJTGLOBAL:
		case PnutsParserTreeConstants.JJTMETHODNODE:
		case PnutsParserTreeConstants.JJTMEMBERNODE:
		case PnutsParserTreeConstants.JJTSTATICMETHODNODE:
		case PnutsParserTreeConstants.JJTSTATICMEMBERNODE:
		case PnutsParserTreeConstants.JJTLOCAL:
		case PnutsParserTreeConstants.JJTPARAM:
		case PnutsParserTreeConstants.JJTFORENUM:
			str = str.intern();
			break;
		case PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT:
			if (str != null) {
				str = str.intern();
			}
		case PnutsParserTreeConstants.JJTSTART:
			break;
		case PnutsParserTreeConstants.JJTFOREACHSTATEMENT:
			str = str.intern();
		case PnutsParserTreeConstants.JJTBLOCK:
		case PnutsParserTreeConstants.JJTSWITCHBLOCK:
		case PnutsParserTreeConstants.JJTDOSTATEMENT:
		case PnutsParserTreeConstants.JJTWHILESTATEMENT:
		case PnutsParserTreeConstants.JJTFORSTATEMENT:
		case PnutsParserTreeConstants.JJTSWITCHSTATEMENT:
			break;
		}
	}

	void setToken(Token t) {
		setToken(t, t);
	}

	private void setToken(Token t1, Token t2) {
		beginLine = t1.beginLine;
		beginColumn = t1.beginColumn;
		endLine = t2.endLine;
	}

	public Object clone() {
		try {
			SimpleNode c = (SimpleNode) super.clone();
			c.children = null;
			return c;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
}
