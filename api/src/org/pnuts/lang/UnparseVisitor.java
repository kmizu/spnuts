/*
 * UnparseVisitor.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import pnuts.lang.Context;
import pnuts.lang.SimpleNode;
import pnuts.lang.Visitor;
import pnuts.lang.PnutsParserTreeConstants;

/**
 * This class is used to retrieve a textual representation of a script from
 * parsed tree.
 */
public class UnparseVisitor implements Visitor {

	private StringBuffer sbuf;
	private boolean escape;

	public UnparseVisitor(StringBuffer sbuf) {
		this(sbuf, false);
	}

	public UnparseVisitor(StringBuffer sbuf, boolean escape) {
		this.sbuf = sbuf;
		this.escape = escape;
	}

	void operand(SimpleNode node, int idx) {
		int id = node.jjtGetChild(idx).id;
		if (id == PnutsParserTreeConstants.JJTIDNODE
				|| id == PnutsParserTreeConstants.JJTINDEXNODE
				|| id == PnutsParserTreeConstants.JJTRANGENODE
				|| id == PnutsParserTreeConstants.JJTSTATICMETHODNODE
				|| id == PnutsParserTreeConstants.JJTSTATICMEMBERNODE
				|| id == PnutsParserTreeConstants.JJTAPPLICATIONNODE
				|| id == PnutsParserTreeConstants.JJTMETHODNODE
				|| id == PnutsParserTreeConstants.JJTMEMBERNODE
				|| id == PnutsParserTreeConstants.JJTCASTEXPRESSION
				|| id == PnutsParserTreeConstants.JJTINTEGERNODE
				|| id == PnutsParserTreeConstants.JJTFLOATINGNODE
				|| id == PnutsParserTreeConstants.JJTCHARACTERNODE
				|| id == PnutsParserTreeConstants.JJTSTRINGNODE
				|| id == PnutsParserTreeConstants.JJTTRUENODE
				|| id == PnutsParserTreeConstants.JJTFALSENODE
				|| id == PnutsParserTreeConstants.JJTNULLNODE) {
			accept(node, idx);
		} else if (id == PnutsParserTreeConstants.JJTMULTIASSIGNLHS){
		    paramList(node.jjtGetChild(idx));
		} else {
			sbuf.append('(');
			accept(node, idx);
			sbuf.append(')');
		}
	}

	Object binary(SimpleNode node, String operator) {
		operand(node, 0);
		sbuf.append(' ');
		sbuf.append(operator);
		sbuf.append(' ');
		operand(node, 1);
		return null;
	}

	public Object startSet(SimpleNode node, Context context) {
		expressionList(node, context, '\n');
		return null;
	}

	public Object start(SimpleNode node, Context context) {
		node.jjtGetChild(0).accept(this, context);
		return null;
	}

	public Object expressionList(SimpleNode node, Context context) {
		expressionList(node, context, ';');
		return null;
	}

	public Object expressionList(SimpleNode node, Context context,
			char delimiter) {
		int num = node.jjtGetNumChildren();
		if (num > 0) {
			accept(node, 0);
			for (int i = 1; i < num; i++) {
				sbuf.append(delimiter);
				accept(node, i);
			}
		}
		return null;
	}

	static String strip(String name){
		if (name == null){
			return null;
		}
		int idx = name.indexOf('!');
		if (idx < 0){
			return name;
		} else if (idx == 0){
			return null;
		} else {
			return name.substring(0, idx);
		}
	}

	public Object idNode(SimpleNode node, Context context) {
		String s = strip(node.str);
		if (s == null){
			return null;
		}
		sbuf.append(s);
		int dim = node.jjtGetNumChildren();
		for (int i = 0; i < dim; i++) {
			sbuf.append("[]");
		}
		return null;
	}

	public Object global(SimpleNode node, Context context) {
		sbuf.append("::");
		sbuf.append(node.str);
		return null;
	}

	public Object className(SimpleNode node, Context context) {
		sbuf.append(node.jjtGetChild(0).str);
		for (int i = 1; i < node.jjtGetNumChildren(); i++) {
			sbuf.append('.');
			sbuf.append(node.jjtGetChild(i).str);
		}
		return null;
	}

	public Object arrayType(SimpleNode node, Context context) {
		accept(node, 0);
		sbuf.append("[]");
		return null;
	}

	public Object listElements(SimpleNode node, Context context) {
		return listElements(node, context, "[", "]");
	}

	public Object mapNode(SimpleNode node, Context context) {
		sbuf.append('{');
		int n = node.jjtGetNumChildren();
		if (n > 0) {
			SimpleNode c = node.jjtGetChild(0);
			accept(c, 0);
			sbuf.append(" => ");
			accept(c, 1);
		}
		for (int i = 1; i < n; i++) {
			sbuf.append(", ");
			SimpleNode c = node.jjtGetChild(i);
			accept(c, 0);
			sbuf.append(" => ");
			accept(c, 1);
		}
		sbuf.append('}');
		return null;
	}

	public Object listElements(SimpleNode node, Context context, String d1,
			String d2) {
		if (d1 != null) {
			sbuf.append(d1);
		}
		int n = node.jjtGetNumChildren();
		if (n > 0) {
			accept(node, 0);
			for (int i = 1; i < n; i++) {
				sbuf.append(", ");
				accept(node, i);
			}
		}
		if (d2 != null) {
			sbuf.append(d2);
		}
		return null;
	}

	void paramList(SimpleNode args) {
		int nargs = args.jjtGetNumChildren();
		if (nargs > 0) {
			sbuf.append(strip(args.jjtGetChild(0).str));
		}
		for (int i = 1; i < nargs; i++) {
			sbuf.append(", ");
			sbuf.append(strip(args.jjtGetChild(i).str));
		}
	}

	public Object castExpression(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n == 2) {
			sbuf.append('(');
			SimpleNode target = (SimpleNode) node.jjtGetChild(1);
			int id = target.id;
			if (id == PnutsParserTreeConstants.JJTIDNODE
					|| id == PnutsParserTreeConstants.JJTINDEXNODE
					|| id == PnutsParserTreeConstants.JJTRANGENODE
					|| id == PnutsParserTreeConstants.JJTSTATICMETHODNODE
					|| id == PnutsParserTreeConstants.JJTSTATICMEMBERNODE
					|| id == PnutsParserTreeConstants.JJTAPPLICATIONNODE
					|| id == PnutsParserTreeConstants.JJTMETHODNODE
					|| id == PnutsParserTreeConstants.JJTMEMBERNODE
					|| id == PnutsParserTreeConstants.JJTCASTEXPRESSION
					|| id == PnutsParserTreeConstants.JJTINTEGERNODE
					|| id == PnutsParserTreeConstants.JJTFLOATINGNODE
					|| id == PnutsParserTreeConstants.JJTCHARACTERNODE
					|| id == PnutsParserTreeConstants.JJTSTRINGNODE
					|| id == PnutsParserTreeConstants.JJTTRUENODE
					|| id == PnutsParserTreeConstants.JJTFALSENODE
					|| id == PnutsParserTreeConstants.JJTNULLNODE) {
				type(node.jjtGetChild(0));
				sbuf.append(')');
				target.accept(this, context);
			} else {
				type(node.jjtGetChild(0));
				sbuf.append(")(");
				target.accept(this, context);
				sbuf.append(')');
			}
		} else {
			accept(node, 0);
		}
		return null;
	}

	public Object classNode(SimpleNode node, Context context) {
		sbuf.append("class");
		int n = node.jjtGetNumChildren();
		if (node.str != null) {
			sbuf.append(' ');
			sbuf.append(node.str);
		}
		int dim = 0;
		for (int i = 0; i < n; i++) {
			SimpleNode ch = (SimpleNode) node.jjtGetChild(i);
			if (ch.id == PnutsParserTreeConstants.JJTPACKAGE) {
				sbuf.append('.');
				sbuf.append(ch.str);
			} else if (ch.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
				dim++;
			}
		}
		for (int i = 0; i < dim; i++) {
			sbuf.append("[]");
		}
		return null;
	}

	public Object newNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		SimpleNode n0 = node.jjtGetChild(0);

		sbuf.append("new ");

		if (n == 3
		    && node.jjtGetChild(2).id == PnutsParserTreeConstants.JJTCLASSDEFBODY) { // subclass
		    type(n0);
		    listElements(node.jjtGetChild(1), context, "(", ")");
		    SimpleNode classDefBody = node.jjtGetChild(2);
		    classDefBody.accept(this, context);

		} else if (n0.id == PnutsParserTreeConstants.JJTINDEXNODE) {
			SimpleNode n1 = n0;
			while (n1.id == PnutsParserTreeConstants.JJTINDEXNODE) {
				n1 = n1.jjtGetChild(0);
			}
			type(n1);
			n1 = n1.jjtGetParent();
			while (n1 != n0) {
				sbuf.append('[');
				accept(n1, 1);
				sbuf.append(']');
				n1 = n1.jjtGetParent();
			}
			sbuf.append('[');
			accept(n1, 1);
			sbuf.append(']');
		} else if (n0.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
			arrayType(n0, context);

		} else if (n0.id == PnutsParserTreeConstants.JJTARRAYNODE) {
			type(n0.jjtGetChild(0));
			listElements(n0.jjtGetChild(1), context, "{", "}");
		} else {
			type(node.jjtGetChild(0));
			listElements(node.jjtGetChild(1), context, "(", ")");
		}
		return null;
	}

       public Object classScript(SimpleNode node, Context context){
           return null;
       }

       public Object classDef(SimpleNode node, Context context){
	   sbuf.append("class ");
	   className(node.jjtGetChild(0), context);
	   SimpleNode ext = node.jjtGetChild(1);
	   SimpleNode impl = node.jjtGetChild(2);
	   SimpleNode classDefBody = node.jjtGetChild(3);
	   if (ext.jjtGetNumChildren() > 0){
	       sbuf.append(" extends ");
	       className(ext.jjtGetChild(0), context);
	   } 
	   if (impl.jjtGetNumChildren() > 0){
	       sbuf.append(" implements ");
	       int num = impl.jjtGetNumChildren();
	       if (num > 0){
		   className(impl.jjtGetChild(0), context);
	       }
	       for (int i = 1; i < num; i++){
		   sbuf.append(", ");
		   className(impl.jjtGetChild(i), context);
	       }
	   }
	   classDefBody.accept(this, context);
           return null;
       }

       public Object classDefBody(SimpleNode node, Context context){
	   sbuf.append("{");
	   int n = node.jjtGetNumChildren();
	   for (int i = 0; i < n; i++){
	       sbuf.append("\n");
	       SimpleNode c = node.jjtGetChild(i);
	       if (c.id == PnutsParserTreeConstants.JJTFIELDDEF){
		   int j = 0;
		   SimpleNode c2 = c.jjtGetChild(j);
		   if (c2.id == PnutsParserTreeConstants.JJTCLASSNAME){
		       type(c2);
		       sbuf.append(" ");
		       j++;
		   }
		   sbuf.append(c.str);
		   if (c.jjtGetNumChildren() > j){
		       SimpleNode c3 = c.jjtGetChild(j);
		       sbuf.append(" = ");
		       c3.accept(this, context);
		   }
	       } else {
		   methodDef(c, context);
	       }
	   }
	   sbuf.append("\n}");
           return null;
       }

       public Object methodDef(SimpleNode node, Context context){
	   int n = node.jjtGetNumChildren();
	   if (n == 2){
	       sbuf.append(node.str);
	       typedParamList(node.jjtGetChild(0), context);
	       node.jjtGetChild(1).accept(this, context);
	   } else if (n == 3){
	       type(node.jjtGetChild(0));
	       typedParamList(node.jjtGetChild(1), context);
	       node.jjtGetChild(2).accept(this, context);
	   } else {
	       throw new InternalError();
	   }
           return null;
       }


       public Object typedParamList(SimpleNode node, Context context){
	   sbuf.append("(");
	   int n = node.jjtGetNumChildren();
	   if (n > 0){
	       SimpleNode p = node.jjtGetChild(0);
	       typedParam(p, context);
	   }
	   for (int i = 1; i < n; i++){
	       sbuf.append(",");
	       SimpleNode p = node.jjtGetChild(i);
	       typedParam(p, context);
	   }
	   sbuf.append(")");
	   return null;
       }

       public Object typedParam(SimpleNode node, Context context){
	   int n = node.jjtGetNumChildren();
	   if (n == 2){
	       SimpleNode t = node.jjtGetChild(0);
	       type(t);
	       sbuf.append(' ');
	       sbuf.append(node.jjtGetChild(1).str);
	   } else if (n == 1){
	       sbuf.append(node.jjtGetChild(0).str);
	   } else {
	       throw new InternalError();
	   }
	   return null;
       }


	public Object primitiveNode(SimpleNode node, Context context) {
		sbuf.append(node.str);
		return null;
	}

	public Object packageNode(SimpleNode node, Context context) {
	    int n = node.jjtGetNumChildren();
	    sbuf.append("package");
	    if (node.str != null){
		sbuf.append("(");
		if (n > 0){
		    node.jjtGetChild(0).accept(this, context);
		}
	    } else {
		sbuf.append(" ");
		SimpleNode p0 = node.jjtGetChild(0);
		sbuf.append(p0.str);
		for (int i = 1; i < n; i++){
		    sbuf.append(".");
		    sbuf.append(node.jjtGetChild(i).str);
		}
	    }
	    if (node.str != null){
		sbuf.append(")");
	    } else {
		sbuf.append("\n");
	    }
	    return null;
	}

	public Object importNode(SimpleNode node, Context context) {
		sbuf.append("import ");
		if ("static".equals(node.info)) {
			sbuf.append("static ");
		}
		int n = node.jjtGetNumChildren();
		if (n == 0 && "*".equals(node.str)) {
			sbuf.append('*');
			return null;
		} else if ("(".equals(node.str)) {
			sbuf.append('(');
			if (n == 1) {
				accept(node, 0);
			}
			sbuf.append(')');
			return null;
		} else {
			sbuf.append(node.jjtGetChild(0).str);
			for (int i = 1; i < n; i++) {
				sbuf.append('.');
				sbuf.append(node.jjtGetChild(i).str);
			}
			if ("*".equals(node.str)) {
				sbuf.append(".*");
			}
		}
		return null;
	}

	public Object rangeNode(SimpleNode node, Context context) {
		if (node.jjtGetNumChildren() == 3) {
			accept(node, 0);
			sbuf.append('[');
			accept(node, 1);
			sbuf.append("..");
			accept(node, 2);
			sbuf.append(']');
		} else {
			accept(node, 0);
			sbuf.append('[');
			accept(node, 1);
			sbuf.append("..]");
		}
		return null;
	}

	public Object indexNode(SimpleNode node, Context context) {
		accept(node, 0);
		sbuf.append('[');
		accept(node, 1);
		sbuf.append(']');
		return null;
	}

	public Object methodNode(SimpleNode node, Context context) {
		int id = ((SimpleNode) node.jjtGetChild(0)).id;
		if (id == PnutsParserTreeConstants.JJTINTEGERNODE
				|| id == PnutsParserTreeConstants.JJTFLOATINGNODE) {
			sbuf.append('(');
			accept(node, 0);
			sbuf.append(')');
		} else {
			accept(node, 0);
		}
		sbuf.append('.');
		sbuf.append(node.str);
		listElements(node.jjtGetChild(1), context, "(", ")");
		return null;
	}

	public Object staticMethodNode(SimpleNode node, Context context) {
		accept(node, 0);
		sbuf.append("::");
		sbuf.append(node.str);
		listElements(node.jjtGetChild(1), context, "(", ")");
		return null;
	}

	public Object memberNode(SimpleNode node, Context context) {
		int id = ((SimpleNode) node.jjtGetChild(0)).id;
		if (id == PnutsParserTreeConstants.JJTINTEGERNODE
				|| id == PnutsParserTreeConstants.JJTFLOATINGNODE) {
			sbuf.append('(');
			accept(node, 0);
			sbuf.append(')');
		} else {
			accept(node, 0);
		}
		sbuf.append('.');
		sbuf.append(node.str);
		return null;
	}

	public Object staticMemberNode(SimpleNode node, Context context) {
		accept(node, 0);
		sbuf.append("::");
		sbuf.append(node.str);
		return null;
	}

	public Object applicationNode(SimpleNode node, Context context) {
		SimpleNode n0 = node.jjtGetChild(0);
		if (n0.str != null && n0.str.startsWith("!<closure")){
			sbuf.append("yield ");
			listElements(node.jjtGetChild(1), context, "", "");
		} else {
			accept(node, 0);
			listElements(node.jjtGetChild(1), context, "(", ")");
		}
		return null;
	}

	public Object integerNode(SimpleNode node, Context context) {
		sbuf.append(node.str);
		return null;
	}

	public Object floatingNode(SimpleNode node, Context context) {
		sbuf.append(node.str);
		return null;
	}

	public Object characterNode(SimpleNode node, Context context) {
		sbuf.append(node.str);
		return null;
	}

	String unparseString(String value, boolean escape) {
		char c[] = value.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == '"') {
				sbuf.append("\\\"");
			} else if (c[i] == '\\') {
				sbuf.append("\\\\");
			} else if (c[i] == '\n') {
				sbuf.append("\\n");
			} else if (c[i] == '\t') {
				sbuf.append("\\t");
			} else if (c[i] == '\r') {
				sbuf.append("\\r");
			} else if (c[i] == '\f') {
				sbuf.append("\\f");
			} else if (c[i] == '\b') {
				sbuf.append("\\b");
			} else if (c[i] == '\0') {
				sbuf.append("\\0");
			} else {
				sbuf.append(c[i]);
			}
		}
		return null;
	}

	public Object stringNode(SimpleNode node, Context context) {
	    if (escape){
		sbuf.append('\\');
	    }
	    sbuf.append('"');
	    if (node.str != null){
		unparseString(node.str, escape);
	    } else {
		int n = node.jjtGetNumChildren();
		for (int i = 0; i < n; i++){
		    SimpleNode c = node.jjtGetChild(i);
		    if (c.id == PnutsParserTreeConstants.JJTSTRINGNODE){
			unparseString(c.str, escape);
		    } else {
			sbuf.append(escape ? "\\\\(" : "\\(");
			c.accept(this, context);
			sbuf.append(escape ? "\\\\)" : "\\)");
		    }
		}
	    }
	    if (escape) {
		sbuf.append('\\');
	    }
	    sbuf.append('"');
	    return null;
	}

	public Object trueNode(SimpleNode node, Context context) {
		sbuf.append("true");
		return null;
	}

	public Object falseNode(SimpleNode node, Context context) {
		sbuf.append("false");
		return null;
	}

	public Object nullNode(SimpleNode node, Context context) {
		sbuf.append("null");
		return null;
	}

	public Object assignment(SimpleNode node, Context context) {
		return binary(node, "=");
	}

	public Object assignmentTA(SimpleNode node, Context context) {
		return binary(node, "*=");
	}

	public Object assignmentMA(SimpleNode node, Context context) {
		return binary(node, "%=");
	}

	public Object assignmentDA(SimpleNode node, Context context) {
		return binary(node, "/=");
	}

	public Object assignmentPA(SimpleNode node, Context context) {
		return binary(node, "+=");
	}

	public Object assignmentSA(SimpleNode node, Context context) {
		return binary(node, "-=");
	}

	public Object assignmentLA(SimpleNode node, Context context) {
		return binary(node, "<<=");
	}

	public Object assignmentRA(SimpleNode node, Context context) {
		return binary(node, ">>=");
	}

	public Object assignmentRAA(SimpleNode node, Context context) {
		return binary(node, ">>>=");
	}

	public Object assignmentAA(SimpleNode node, Context context) {
		return binary(node, "&=");
	}

	public Object assignmentEA(SimpleNode node, Context context) {
		return binary(node, "^=");
	}

	public Object assignmentOA(SimpleNode node, Context context) {
		return binary(node, "|=");
	}

	public Object logAndNode(SimpleNode node, Context context) {
		return binary(node, "&&");
	}

	public Object logOrNode(SimpleNode node, Context context) {
		return binary(node, "||");
	}

	public Object logNotNode(SimpleNode node, Context context) {
		sbuf.append('!');
		operand(node, 0);
		return null;
	}

	public Object orNode(SimpleNode node, Context context) {
		return binary(node, "|");
	}

	public Object xorNode(SimpleNode node, Context context) {
		return binary(node, "^");
	}

	public Object andNode(SimpleNode node, Context context) {
		return binary(node, "&");
	}

	public Object equalNode(SimpleNode node, Context context) {
		return binary(node, "==");
	}

	public Object notEqNode(SimpleNode node, Context context) {
		return binary(node, "!=");
	}

	void type(SimpleNode node) {
		int dim = 0;
		while (node.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
			dim++;
			node = node.jjtGetChild(0);
		}
		SimpleNode ch = (SimpleNode) node.jjtGetChild(0);
		sbuf.append(ch.str);

		int n = node.jjtGetNumChildren();
		for (int i = 1; i < n; i++) {
			ch = (SimpleNode) node.jjtGetChild(i);
			if (ch.id == PnutsParserTreeConstants.JJTPACKAGE) {
				sbuf.append('.');
				sbuf.append(ch.str);
			}
		}
		for (int i = 0; i < dim; i++) {
			sbuf.append("[]");
		}
	}

	public Object instanceofExpression(SimpleNode node, Context context) {
		accept(node, 0);
		sbuf.append(" instanceof ");
		type(node.jjtGetChild(1));
		return null;
	}

	public Object ltNode(SimpleNode node, Context context) {
		return binary(node, "<");
	}

	public Object gtNode(SimpleNode node, Context context) {
		return binary(node, ">");
	}

	public Object leNode(SimpleNode node, Context context) {
		return binary(node, "<=");
	}

	public Object geNode(SimpleNode node, Context context) {
		return binary(node, ">=");
	}

	public Object shiftLeftNode(SimpleNode node, Context context) {
		return binary(node, "<<");
	}

	public Object shiftRightNode(SimpleNode node, Context context) {
		return binary(node, ">>");
	}

	public Object shiftArithmeticNode(SimpleNode node, Context context) {
		return binary(node, ">>>");
	}

	public Object addNode(SimpleNode node, Context context) {
		return binary(node, "+");
	}

	public Object subtractNode(SimpleNode node, Context context) {
		return binary(node, "-");
	}

	public Object multNode(SimpleNode node, Context context) {
		return binary(node, "*");
	}

	public Object divideNode(SimpleNode node, Context context) {
		return binary(node, "/");
	}

	public Object modNode(SimpleNode node, Context context) {
		return binary(node, "%");
	}

	public Object negativeNode(SimpleNode node, Context context) {
		sbuf.append('-');
		operand(node, 0);
		return null;
	}

	public Object preIncrNode(SimpleNode node, Context context) {
		sbuf.append("++");
		operand(node, 0);
		return null;
	}

	public Object preDecrNode(SimpleNode node, Context context) {
		sbuf.append("--");
		operand(node, 0);
		return null;
	}

	public Object postIncrNode(SimpleNode node, Context context) {
		operand(node, 0);
		sbuf.append("++");
		return null;
	}

	public Object postDecrNode(SimpleNode node, Context context) {
		operand(node, 0);
		sbuf.append("--");
		return null;
	}

	public Object notNode(SimpleNode node, Context context) {
		sbuf.append('~');
		operand(node, 0);
		return null;
	}

	public Object continueNode(SimpleNode node, Context context) {
		sbuf.append("continue");
		return null;
	}

	public Object breakNode(SimpleNode node, Context context) {
		sbuf.append("break");
		if (node.jjtGetNumChildren() > 0) {
			accept(node, 0);
		}
		return null;
	}

	public Object returnNode(SimpleNode node, Context context) {
		sbuf.append("return");
		int n = node.jjtGetNumChildren();
		if (n == 1) {
			sbuf.append(' ');
			accept(node, 0);
		}
		return null;
	}

	public Object yieldNode(SimpleNode node, Context context) {
		sbuf.append("yield");
		int n = node.jjtGetNumChildren();
		if (n == 1) {
			sbuf.append(' ');
			accept(node, 0);
		}
		return null;
	}

	public Object catchNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n == 0) {
			sbuf.append("catch");
			return null;
		}
		sbuf.append("catch");
		sbuf.append('(');
		accept(node, 0);
		sbuf.append(", ");
		accept(node, 1);
		sbuf.append(')');
		return null;
	}

	public Object throwNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n == 0) {
			sbuf.append("throw");
		} else {
			sbuf.append("throw ");
			accept(node, 0);
		}
		return null;
	}

	public Object finallyNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n == 0) {
			sbuf.append("finally");
			return null;
		}
		sbuf.append("finally(");
		if (n == 1) {
			accept(node, 0);
			sbuf.append(')');
		} else if (n == 2) {
			accept(node, 0);
			sbuf.append(", ");
			accept(node, 1);
			sbuf.append(')');
		}
		return null;
	}

	private static String indent = " ";

	private int nest = 0;

	public Object tryStatement(SimpleNode node, Context context) {
		sbuf.append("try ");
		accept(node, 0);

		int n = node.jjtGetNumChildren();
		for (int i = 1; i < n; i++) {
			SimpleNode c = node.jjtGetChild(i);
			if (c.id == PnutsParserTreeConstants.JJTFINALLYBLOCK) {
				sbuf.append(" finally ");
				accept(c, 0);
				break;
			} else if (c.id == PnutsParserTreeConstants.JJTCATCHBLOCK) {
				sbuf.append(" catch (");
				SimpleNode type = c.jjtGetChild(0);
				int n2 = type.jjtGetNumChildren();
				sbuf.append(type.jjtGetChild(0).str);
				for (int j = 1; j < n2; j++) {
					sbuf.append('.');
					sbuf.append(type.jjtGetChild(j).str);
				}
				sbuf.append(' ');
				sbuf.append(c.str);
				sbuf.append(") ");
				accept(c, 1);
			}
		}
		return null;
	}

    public Object catchBlock(SimpleNode node, Context context){
	return null;
    }
	public Object blockNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n >= 1) {
			String s = null;
			if (escape) {
				sbuf.append("{\\n");
			} else {
				sbuf.append("{\n");
			}
			++nest;
			for (int i = 0; i < nest; i++) {
				sbuf.append(indent);
			}
			accept(node, 0);
			for (int i = 1; i < n; i++) {
				if (escape) {
					sbuf.append("\\n");
				} else {
					sbuf.append('\n');
				}
				for (int j = 0; j < nest; j++) {
					sbuf.append(indent);
				}
				accept(node, i);
			}
			--nest;
			if (escape) {
				sbuf.append("\\n");
			} else {
				sbuf.append('\n');
			}
			for (int i = 0; i < nest; i++) {
				sbuf.append(indent);
			}
			sbuf.append('}');
		} else {
			sbuf.append("{}");
		}
		return null;
	}

	public Object ifStatement(SimpleNode node, Context context) {
		sbuf.append("if (");
		accept(node, 0);
		sbuf.append(") ");
		accept(node, 1);
		int n = node.jjtGetNumChildren();
		for (int i = 2; i < n; i++) {
			SimpleNode c = (SimpleNode) node.jjtGetChild(i);
			if (c.id == PnutsParserTreeConstants.JJTELSEIFNODE) {
				sbuf.append(" else if (");
				accept(c, 0);
				sbuf.append(") ");
				accept(c, 1);
			} else if (c.id == PnutsParserTreeConstants.JJTELSENODE) {
				sbuf.append(" else ");
				accept(c, 0);
			}
		}
		return null;
	}

	public Object doStatement(SimpleNode node, Context context) {
		sbuf.append("do {");
		accept(node, 0);
		sbuf.append("} while (");
		accept(node, 1);
		sbuf.append(')');
		return null;
	}

	public Object whileStatement(SimpleNode node, Context context) {
		sbuf.append("while (");
		accept(node, 0);
		sbuf.append(") ");
		accept(node, 1);
		return null;
	}

	public Object forStatement(SimpleNode node, Context context) {
		SimpleNode initNode = null;
		SimpleNode condNode = null;
		SimpleNode updateNode = null;
		SimpleNode blockNode = null;

		sbuf.append("for (");

		int j = 0;
		SimpleNode n = (SimpleNode) node.jjtGetChild(j);
		if (n.id == PnutsParserTreeConstants.JJTFORENUM) {
			SimpleNode node0 = n.jjtGetChild(0);
			if (node0.id == PnutsParserTreeConstants.JJTMULTIASSIGNLHS){
				int nc2 = node0.jjtGetNumChildren();
				if (nc2 > 0){
				    sbuf.append(node0.jjtGetChild(0).str);
				    for (int i = 1; i < nc2; i++){
					sbuf.append(',');
					sbuf.append(node0.jjtGetChild(i).str);
				    }
				}
			} else {
				sbuf.append(strip(n.str));
			}
			sbuf.append(':');
			int nc = n.jjtGetNumChildren();
			if (node0.id == PnutsParserTreeConstants.JJTMULTIASSIGNLHS){
			    accept(n, 1);
			} else {
			    accept(n, 0);
			    if (nc == 2) {
				sbuf.append("..");
				accept(n, 1);
			    }
			}
			sbuf.append(')');
			j++;
		} else {
			if (n.id == PnutsParserTreeConstants.JJTFORINIT) {
				initNode = n;
				j++;
			}
			n = (SimpleNode) node.jjtGetChild(j);
			if (n.id != PnutsParserTreeConstants.JJTFORUPDATE
					&& n.id != PnutsParserTreeConstants.JJTBLOCK) {
				condNode = n;
				j++;
			}
			n = (SimpleNode) node.jjtGetChild(j);
			if (n.id == PnutsParserTreeConstants.JJTFORUPDATE) {
				updateNode = n;
				j++;
			}

			if (initNode != null) {
				int num = initNode.jjtGetNumChildren();
				SimpleNode sn = null;
				if (num > 0) {
					sn = (SimpleNode) initNode.jjtGetChild(0);
					sbuf.append(strip(sn.str));
					sbuf.append('=');
					accept(sn, 0);
				}
				for (int i = 1; i < num; i++) {
					sn = (SimpleNode) initNode.jjtGetChild(i);
					sbuf.append(',');
					sbuf.append(strip(sn.str));
					sbuf.append('=');
					accept(sn, 0);
				}
			}
			sbuf.append(';');
			if (condNode != null) {
				condNode.accept(this, context);
			}
			sbuf.append(';');
			if (updateNode != null) {
				expressionList(updateNode, context, ',');
			}
			sbuf.append(") ");
		}

		blockNode = (SimpleNode) node.jjtGetChild(j);
		blockNode.accept(this, context);
		return null;
	}

	public Object foreachStatement(SimpleNode node, Context context) {
		sbuf.append("foreach ");
		sbuf.append(strip(node.str));
		sbuf.append(" (");
		accept(node, 0);
		sbuf.append(") ");
		accept(node, 1);
		return null;
	}

	public Object switchStatement(SimpleNode node, Context context) {
		sbuf.append("switch (");
		accept(node, 0);
		sbuf.append("){");

		for (int i = 1; i < node.jjtGetNumChildren(); i++) {
			SimpleNode n = (SimpleNode) node.jjtGetChild(i);
			if (n.jjtGetNumChildren() == 1) { // case
				sbuf.append("case ");
				accept(n, 0);
				sbuf.append(": ");
				accept(node, ++i);
			} else {
				sbuf.append("default: ");
				accept(node, ++i);
			}
			sbuf.append("; ");
		}
		sbuf.append('}');
		return null;
	}

	public Object switchBlock(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n > 0) {
			accept(node, 0);
			for (int i = 1; i < n; i++) {
				sbuf.append("; ");
				accept(node, i);
			}
		}
		return null;
	}

	public Object functionStatement(SimpleNode node, Context context) {
		int pid = 0;
		if (node.jjtGetParent() != null){
			pid = node.jjtGetParent().id;
			if (pid == PnutsParserTreeConstants.JJTAPPLICATIONNODE
			    || pid == PnutsParserTreeConstants.JJTMETHODNODE) {
				sbuf.append('(');
			}
		}
		sbuf.append("function ");

		if (node.str != null) {
			sbuf.append(node.str);
		}
		SimpleNode args = (SimpleNode) node.jjtGetChild(0);
		int nargs = args.jjtGetNumChildren();
		boolean varargs = "[".equals(args.str);
		sbuf.append('(');
		SimpleNode n0 = null;
		if (nargs > 0) {
			paramList((SimpleNode) args);
			if (varargs){
			    sbuf.append("[]");
			}
		}
		sbuf.append(") ");
		accept(node, 1);
		if (pid == PnutsParserTreeConstants.JJTAPPLICATIONNODE
				|| pid == PnutsParserTreeConstants.JJTMETHODNODE) {
			sbuf.append(')');
		}
		return null;
	}

	public Object ternary(SimpleNode node, Context context) {
		sbuf.append('(');
		accept(node, 0);
		sbuf.append(" ? ");
		accept(node, 1);
		sbuf.append(" : ");
		accept(node, 2);
		sbuf.append(')');
		return null;
	}

	public Object beanDef(SimpleNode node, Context context){
	    node.jjtGetChild(0).accept(this, context);
	    sbuf.append("{\n");
	    ++nest;
	    for (int i = 1; i < node.jjtGetNumChildren(); i++){
		for (int j = 0; j < nest; j++) {
		    sbuf.append(indent);
		}
		SimpleNode cn = node.jjtGetChild(i);
		sbuf.append(cn.str);
		if ("::".equals(cn.info)){
		    sbuf.append(":: ");
		} else {
		    sbuf.append(": ");
		}
		accept(cn, 0);
		sbuf.append('\n');
	    }
	    --nest;
	    for (int i = 0; i < nest; i++) {
		sbuf.append(indent);
	    }
	    sbuf.append("}");
	    return null;
	}


	final void accept(SimpleNode node, int idx) {
		node.jjtGetChild(idx).accept(this, null);
	}
}
