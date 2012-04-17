/*
 * @(#)Generator.java 1.5 05/04/29
 * 
 * Copyright (c) 2004,2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import org.pnuts.util.Stack;
import org.pnuts.lang.GeneratorHelper;
import java.util.*;

public class Generator {
	private final static String THIS = "this".intern();
	private final static String SUPER = "super".intern();

	Context context;

	private PnutsFunction gfunc;

	protected Generator() {
	}

	public Generator(PnutsFunction gfunc) {
		this.gfunc = gfunc;
	}

	public Object apply(PnutsFunction closure, Context context) {
		return gfunc.exec(new Object[] { closure }, context);
	}

	public Object[] toArray(){
	    final List elements = new ArrayList();
	    apply(new PnutsFunction(){
		    protected Object exec(Object[] args, Context context){
			elements.add(args[0]);
			return null;
		    }
		}, context);
	    return elements.toArray();
	}

	public String toString() {
		return "<generator>";
	}

	/*
	 * Generator function in AST interpreter
	 */
	static class GeneratorFunction extends Function {

		private SimpleNode gnode;
		private static long ID = 0L;

		private Context ctx;
		private String closureSymbol;

		protected GeneratorFunction(String name, String[] locals, int nargs,
				SimpleNode node, Package pkg, Context context) {
			super(name, locals, nargs, false, node, pkg, context);
			String identity = Long.toHexString(ID++);
			this.closureSymbol = ("!<closure" + identity + ">").intern();
			this.gnode = convertYield(GeneratorHelper.renameLocals(node, identity), closureSymbol);
			this.ctx = (Context) context.clone(false, false);

			String[] newLocals = new String[locals.length];
			for (int i = 0; i < locals.length; i++){
			    String local = locals[i];
			    if (THIS == local || SUPER == local){
				newLocals[i] = local;
			    } else {
				newLocals[i] = (local + "!" + identity).intern();
			    }
			}
			this.locals = newLocals;

		}

		protected Object exec(final Object[] args, Context _context) {
			Generator gen = new Generator();
			gen.context = _context;

			final Context context = this.ctx;

			gen.gfunc = new PnutsFunction() {
				protected Object exec(Object[] a, Context ctx) {
					try {
						PnutsFunction closure = (PnutsFunction) a[0];

						String[] lc = new String[locals.length + 1];
						for (int i = 0; i < locals.length; i++){
							lc[i] = locals[i];
						}
						lc[locals.length] = closureSymbol;

						context.openLocal(lc);
						context.bind(closureSymbol, closure);
						for (int i = 0; i < locals.length; i++) {
							context.bind(locals[i], args[i]);
						}
						return gnode.accept(PnutsInterpreter.instance, context);

					} catch (Jump j) {
						return j.getValue();
					} finally {
						context.closeLocal();
					}
				}
				};
			return gen;
		}
	}

	/*
	 * yield <expression>=> CLOSURE( <expression>)
	 */
	public static SimpleNode convertYield(SimpleNode node, String closureSymbol) {
		Stack stack = new Stack();
		return convertYield(node, stack, closureSymbol);
	}

	static SimpleNode convertYield(SimpleNode node, Stack stack, String closureSymbol) {
		if (node.id == PnutsParserTreeConstants.JJTYIELD) {
			SimpleNode applicationNode = new SimpleNode(
					PnutsParserTreeConstants.JJTAPPLICATIONNODE);
			SimpleNode idNode = new SimpleNode(
					PnutsParserTreeConstants.JJTIDNODE);
			idNode.str = closureSymbol;
			applicationNode.jjtAddChild(idNode, 0);
			idNode.jjtSetParent(applicationNode);
			SimpleNode argNode = new SimpleNode(
					PnutsParserTreeConstants.JJTLISTELEMENTS);
			SimpleNode tmp = convertYield(node.jjtGetChild(0), closureSymbol);
			argNode.jjtAddChild(tmp, 0);
			tmp.jjtSetParent(argNode);
			applicationNode.jjtAddChild(argNode, 1);
			argNode.jjtSetParent(applicationNode);
			return applicationNode;
		} else {
			stack.push(node);
		}
		int n = node.jjtGetNumChildren();
		for (int i = 0; i < n; i++) {
			SimpleNode ni = node.jjtGetChild(i);
			SimpleNode c = convertYield(ni, stack, closureSymbol);
			node.jjtAddChild(c, i);
			c.jjtSetParent(node);
		}
		return (SimpleNode) stack.pop();
	}

	public static class Break extends Escape {
		public Break(Object value) {
			super(value);
		}
	}
}
