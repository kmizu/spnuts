/*
 * ScopeAnalyzer.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import pnuts.lang.*;
import pnuts.lang.Context;
import pnuts.lang.SimpleNode;
import pnuts.lang.Visitor;
import pnuts.lang.PnutsParserTreeConstants;
import java.io.Reader;
import java.io.IOException;

public class ScopeAnalyzer implements Visitor {

	public Object start(SimpleNode node, Context context){
		return expressionList(node, context);
	}

	public Object startSet(SimpleNode node, Context context){
		return expressionList(node, context);
	}

	public Object expressionList(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	/**
	 * If this metohd returns false, the node is not passed to handleFreeVariable() method
	 * even if the node represents a free variable.
	 */
	protected boolean isTargetIdNode(SimpleNode node, Context context){
		return true;
	}

	/**
	 * This method is called for each free variable, and supposed to be
	 * redefined by subclasses.
	 *
	 * @param node a SimpleNode that represents a free variable
	 * @param context the context
	 */
	protected void handleFreeVariable(SimpleNode node, Context context){
		// skip
	}

	/**
	 * This method is called for each local variable, and supposed to be
	 * redefined by subclasses.
	 *
	 * @param node a SimpleNode that represents a local variable
	 * @param context the context
	 */
	protected void handleLocalVariable(SimpleNode node, Context context){
		// skip
	}


	protected void declared(SimpleNode node, Context context, String symbol){
		// skip
	}

	public Object idNode(SimpleNode node, Context context){
		TranslateContext cc = (TranslateContext)context;
		if (isTargetIdNode(node, cc)){
			Reference ref = cc.getReference(node.str);
			if (ref == null){
				handleFreeVariable(node, cc);
			} else {
				handleLocalVariable(node, cc);
			}
		}
		return null;
	}

	public Object global(SimpleNode node, Context context){
		return null;
	}

	public Object className(SimpleNode node, Context context){
		return null;
	}

	public Object arrayType(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object castExpression(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object listElements(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object mapNode(SimpleNode node, Context context){
		int num = node.jjtGetNumChildren();
		for (int i = 0; i < num; i++){
			SimpleNode c = node.jjtGetChild(i);
			acceptChildren(c, context);
		}
		return null;
	}

	public Object classNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object newNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

       public Object classDef(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
       }

       public Object classDefBody(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
       }

       public Object methodDef(SimpleNode node, Context context){
		FrameInfo info = (FrameInfo)node.getAttribute("frameInfo");
		if (info == null){
			node.setAttribute("frameInfo", info = new FrameInfo());
		}
		SimpleNode paramList;
		SimpleNode c = node.jjtGetChild(0);
		if (c.id == PnutsParserTreeConstants.JJTTYPEDPARAMLIST){
		    paramList = c;
		} else {
		    paramList = node.jjtGetChild(1);
		}
		int nargs = paramList.jjtGetNumChildren();
		String[] locals = new String[nargs];
		for (int i = 0; i < nargs; i++){
		    SimpleNode param = paramList.jjtGetChild(i);
		    SimpleNode p = param.jjtGetChild(0);
		    if (p.id == PnutsParserTreeConstants.JJTCLASSNAME){
			locals[i] = param.jjtGetChild(1).str;
		    } else {
			locals[i] = p.str;
		    }
		}
		String name = node.str;
		TranslateContext cc = (TranslateContext)context;
		cc.openFrame(name, locals);
		acceptChildren(node, context);
		info.freeVars = cc.getFreeVarSet();
		cc.closeFrame();
		info.preprocessed = true;
		return null;
       }

       public Object classScript(SimpleNode node, Context context){
	   return null;
       }

	public Object primitiveNode(SimpleNode node, Context context){
		return null;
	}

	public Object packageNode(SimpleNode node, Context context){
		return null;
	}

	public Object importNode(SimpleNode node, Context context){
		return null;
	}

	public Object indexNode(SimpleNode node, Context context){
		FrameInfo info = (FrameInfo)node.getAttribute("frameInfo");
		if (info == null){
			node.setAttribute("frameInfo", new FrameInfo());
		}
		acceptChildren(node, context);
		return null;
	}

	public Object rangeNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object methodNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object staticMethodNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object memberNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object staticMemberNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object applicationNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object integerNode(SimpleNode node, Context context){
		return null;
	}

	public Object floatingNode(SimpleNode node, Context context){
		return null;
	}

	public Object characterNode(SimpleNode node, Context context){
		return null;
	}

	public Object stringNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object trueNode(SimpleNode node, Context context){
		return null;
	}

	public Object falseNode(SimpleNode node, Context context){
		return null;
	}

	public Object nullNode(SimpleNode node, Context context){
		return null;
	}

	public Object assignment(SimpleNode node, Context context){
		node.jjtGetChild(1).accept(this, context);
		SimpleNode lhs = node.jjtGetChild(0);
		if (lhs.id == PnutsParserTreeConstants.JJTIDNODE){
			assignId(lhs, context);
		} else {
			lhs.accept(this, context);
		}
		return null;
	}

	public Object assignmentTA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentMA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentDA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentPA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentSA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentLA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentRA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentRAA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentAA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentEA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object assignmentOA(SimpleNode node, Context context){
		acceptChildren(node.jjtGetChild(1), context);
		return null;
	}

	public Object orNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object andNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object xorNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object logAndNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object logOrNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object logNotNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object equalNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object notEqNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object instanceofExpression(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object ltNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object gtNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object leNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object geNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object shiftLeftNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object shiftRightNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object shiftArithmeticNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object addNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object subtractNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object multNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object divideNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object modNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object negativeNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object preIncrNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object preDecrNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object notNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object postIncrNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object postDecrNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object breakNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object continueNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object returnNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}


	public Object yieldNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object tryStatement(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object catchBlock(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object finallyBlock(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object blockNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object ifStatement(SimpleNode node, Context context){
		TranslateContext cc = (TranslateContext)context;
		SimpleNode condNode = node.jjtGetChild(0);
		condNode.accept(this, context);
		cc.openBranchEnv();
		node.jjtGetChild(1).accept(this, context);

		int n = node.jjtGetNumChildren();
		for (int i = 2; i < n; i++) {
			SimpleNode _node = node.jjtGetChild(i);
			if (_node.id == PnutsParserTreeConstants.JJTELSEIFNODE) {
				cc.addBranch();
				SimpleNode _condNode = _node.jjtGetChild(0);
				_condNode.accept(this, context);
				_node.jjtGetChild(1).accept(this, context);
			} else if (_node.id == PnutsParserTreeConstants.JJTELSENODE) {
				cc.addBranch();
				_node.jjtGetChild(0).accept(this, context);
				
			}
		}
		cc.closeBranchEnv();
		return null;
	}

	public Object doStatement(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object whileStatement(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object forStatement(SimpleNode node, Context context){
		int j = 0;
		SimpleNode n = node.jjtGetChild(j);

		TranslateContext cc = (TranslateContext)context;
		SimpleNode blockNode = null;
		if (n.id == PnutsParserTreeConstants.JJTFORENUM) {
	    		SimpleNode n0 = n.jjtGetChild(0);

			if (n0.id == PnutsParserTreeConstants.JJTMULTIASSIGNLHS){
			    n.jjtGetChild(1).accept(this, context);
			    int num = n0.jjtGetNumChildren();
			    String[] vars = new String[num];
			    for (int i = 0; i < num; i++){
				vars[i] = n0.jjtGetChild(i).str;
			    }
			    cc.openScope(vars);
			    blockNode = node.jjtGetChild(1);
			    acceptChildren(blockNode, context);
			    cc.closeScope();
			    
			    return null;
			}

			int num = n.jjtGetNumChildren();
			for (int i = 0; i < num; i++){
				n.jjtGetChild(i).accept(this, context);
			}

			int nc = n.jjtGetNumChildren();
			blockNode = node.jjtGetChild(1);
			if (nc == 1 || nc == 2) { // for (i : value) or for (i : start .. end)
				cc.openScope(new String[]{n.str});
				acceptChildren(blockNode, context);
				cc.closeScope();
			} else {
				return null;
			}

		} else {   // for (;;)


			String[] env;
			if (n.id == PnutsParserTreeConstants.JJTFORINIT) {
				int _num = n.jjtGetNumChildren();
				env = new String[_num];
				for (int i = 0; i < _num; i++) {
					SimpleNode sn = n.jjtGetChild(i);
					sn.jjtGetChild(0).accept(this, context);
					env[i] = sn.str;
				}
				j++;
			} else {
				env = new String[0];
			}
			cc.openScope(env);

			n = node.jjtGetChild(j);
			if (n.id != PnutsParserTreeConstants.JJTFORUPDATE
			    && n.id != PnutsParserTreeConstants.JJTBLOCK) {
				n.accept(this, context);
				j++;
			}
			n = node.jjtGetChild(j);
			if (n.id == PnutsParserTreeConstants.JJTFORUPDATE) {
				n.jjtGetChild(0).accept(this, context);
				j++;
			}
			
			blockNode = node.jjtGetChild(j);
			acceptChildren(blockNode, context);
			cc.closeScope();
		}
		return null;
	}

	public Object foreachStatement(SimpleNode node, Context context){
		TranslateContext cc = (TranslateContext)context;
		cc.openScope(new String[]{node.str});
		SimpleNode listNode = node.jjtGetChild(0);
		SimpleNode blockNode = node.jjtGetChild(1);
		acceptChildren(listNode, context);
		acceptChildren(blockNode, context);
		cc.closeScope();
		return null;
	}

	public Object switchStatement(SimpleNode node, Context context){
		int num = node.jjtGetNumChildren();
		for (int i = 0; i < num; i++){
			SimpleNode c = node.jjtGetChild(i);
			if (c.id == PnutsParserTreeConstants.JJTSWITCHLABEL){
				if (c.jjtGetNumChildren() > 0){
					c.jjtGetChild(0).accept(this, context);
				}
			} else {
				c.accept(this, context);
			}
		}
		return null;
	}

	public Object switchBlock(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object functionStatement(SimpleNode node, Context context){
		FrameInfo info = (FrameInfo)node.getAttribute("frameInfo");
		if (info == null){
			node.setAttribute("frameInfo", info = new FrameInfo());
		}
		TranslateContext cc = (TranslateContext)context;
		String name = node.str;
		SimpleNode block = node.jjtGetChild(1);
		SimpleNode param = node.jjtGetChild(0);
		int nargs = param.jjtGetNumChildren();
		String[] locals = new String[nargs];
		SimpleNode n0 = null;
		if (nargs == 1 &&
			(n0 = param.jjtGetChild(0)).id == PnutsParserTreeConstants.JJTINDEXNODE){
			nargs = -1;
			locals[0] = n0.jjtGetChild(0).str;
		} else {
			for (int j = 0; j < nargs; j++){
				locals[j] = param.jjtGetChild(j).str;
			}
		}
		if (cc.env.parent != null && name != null){
			cc.declare(name);
		}
		cc.openFrame(name, locals);
		block.accept(this, context);

//		((FrameInfo)node.info).freeVars = cc.getFreeVarSet();
		info.freeVars = cc.getFreeVarSet();

		cc.closeFrame();

//		((FrameInfo)node.info).preprocessed = true;
		info.preprocessed = true;
		return null;
	}

	public Object ternary(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object catchNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object throwNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object finallyNode(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	public Object beanDef(SimpleNode node, Context context){
		acceptChildren(node, context);
		return null;
	}

	void assignId(SimpleNode lhs, Context context){
		String symbol= lhs.str;
		TranslateContext cc = (TranslateContext)context;
		Reference ref = cc.getReference(symbol);
		if (cc.env.parent != null){
			if (ref == null){
				cc.declare(symbol);
				declared(lhs, context, symbol);
			}
		}
	}

	void acceptChildren(SimpleNode node, Context context){
		FrameInfo info = (FrameInfo)node.getAttribute("frameInfo");
		if (info == null){
			node.setAttribute("frameInfo", new FrameInfo());
		}
		int num = node.jjtGetNumChildren();
		for (int i = 0; i < num; i++){
			node.jjtGetChild(i).accept(this, context);
		}
	}

	/**
	 * Analyzes a script
	 */
	public void analyze(Reader reader) throws ParseException, IOException {
	    analyze(new PnutsParser(reader));
	}

	public void analyze(PnutsParser parser) throws ParseException, IOException  {
	    parser.StartSet(null).accept(this, new TranslateContext());
	}

	public void analyze(SimpleNode node) {
	    node.accept(this, new TranslateContext());
	}

	public static void main(String[] args) throws Exception {
		ScopeAnalyzer analyzer = new ScopeAnalyzer(){
			protected void handleFreeVariable(SimpleNode node, Context context){
			    System.out.println("free: " + node.str + ", " + node.beginLine);
			}
			protected void handleLocalVariable(SimpleNode node, Context context){
			    System.out.println("local: " + node.str + ", " + node.beginLine);
			}
		    };
		analyzer.analyze(new java.io.FileReader(args[0]));
	}
}
