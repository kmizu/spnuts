/*
 * PnutsInterpreter.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.pnuts.lang.ConstraintsTransformer;
import org.pnuts.lang.GeneratorHelper;
import org.pnuts.util.Cell;

import pnuts.compiler.Compiler;
import pnuts.compiler.ClassGenerator;

/**
 * The pure interpreter
 */
public class PnutsInterpreter extends Runtime implements Visitor {

	private final static Integer one = new Integer(1);

	private final static Object GENERATOR = new Object();

	private final static Object FUNCTION = new Object();

	static PnutsInterpreter instance = new PnutsInterpreter();
	static Compiler compiler = new Compiler();

	static PnutsInterpreter getInstance() {
		return instance;
	}

	final static int CODE = 0;
	final static int VALUE = 1;
	final static int BROKEN = 0;
	final static int CONT = 1;
	final static int PASSED = 2;

	public Object start(SimpleNode node, Context context) {
		if (node.jjtGetNumChildren() > 0) {
			if (context.stackFrame == null) {
				context.stackFrame = new StackFrame();
			}
			try {
				return accept(node, 0, context);
			} catch (Throwable t) {
				checkException(context, t);
			}
		}
		return null;
	}

	public Object startSet(SimpleNode node, Context context) {
		if (context.stackFrame == null) {
			context.stackFrame = new StackFrame();
		}
		int n = node.jjtGetNumChildren();
		Object ret = null;
		try {
			for (int i = 0; i < n; i++) {
				ret = accept(node, i, context);
			}
		} catch (Throwable t) {
			checkException(context, t);
		} finally {
		    if (context.evalFrameStack == null){
			PrintWriter pw = context.getWriter();
			if (pw != null) {
				pw.flush();
			}
		    } else {
			Cell cell = context.evalFrameStack;
			if (cell != null){
			    context.evalFrameStack = cell.next;
			    context.stackFrame = (StackFrame)cell.object;
			}
		    }
		}
		return ret;
	}

	public Object expressionList(SimpleNode node, Context context) {
		Object ret = null;
		int n = node.jjtGetNumChildren();
		for (int i = 0; i < n; i++) {
			ret = accept(node, i, context);
		}
		return ret;
	}

	public Object global(SimpleNode node, Context context) {
		String symbol = node.str;
		Package pkg = Package.find("", context);
		if (pkg != null) {
			Value val = pkg.lookup(symbol, context);
			if (val != null) {
				return val.get();
			}
		}
		return context.undefined(symbol);
	}

	public Object idNode(SimpleNode node, Context context) {
		return context.getValue(node.str);
	}

	public Object className(SimpleNode node, Context context) {
		return resolveClassNameNode(node, context);
	}

	public Object arrayType(SimpleNode node, Context context) {
		SimpleNode n = node;
		int count = 0;
		while (n != null && n.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
			count++;
			n = n.jjtGetChild(0);
		}
		if (n != null && n.id == PnutsParserTreeConstants.JJTINDEXNODE) {
			Class type;
			Object[] idx = parseIndex(n);
			SimpleNode idx0 = (SimpleNode) idx[0];
			Object tgt = idx0.accept(this, context);
			if (tgt instanceof Class) {
				type = Runtime.arrayType((Class) tgt, count);
			} else {
				throw new PnutsException("classOrArray.expected",
						new Object[] { Pnuts.format(tgt) }, context);
			}
			Object[] dim = (Object[]) idx[1];
			int[] idim = new int[dim.length];
			for (int i = 0; i < dim.length; i++) {
				Object o = ((SimpleNode) dim[i]).accept(this, context);
				if (o instanceof Number) {
					idim[i] = ((Number) o).intValue();
				} else {
					throw new PnutsException("number.expected",
							new Object[] { Pnuts.format(o) }, context);
				}
			}
			return Array.newInstance(type, idim);
		} else if (node.jjtGetParent().id != PnutsParserTreeConstants.JJTNEW) {
			Object tgt = n.accept(this, context);
			if (tgt instanceof Class) {
				return Runtime.arrayType((Class) tgt, count);
			} else {
				throw new PnutsException("classOrArray.expected",
						new Object[] { Pnuts.format(tgt) }, context);
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	public Object[] _listElements(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		Object array[] = new Object[n];

		for (int i = 0; i < n; i++) {
			array[i] = accept(node, i, context);
		}
		return array;
	}

	public Object listElements(SimpleNode node, Context context) {
	    if ("{".equals(node.str)){
		List list = context.config.createList();
		int n = node.jjtGetNumChildren();
		for (int i = 0; i < n; i++) {
			list.add(accept(node, i, context));
		}
		return list;
	    } else {
		return context.config.makeArray(_listElements(node, context), context);
	    }
	}

	public Object mapNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		Map map = context.config.createMap(n, context);
		for (int i = 0; i < n; i++) {
			SimpleNode c = node.jjtGetChild(i);
			map.put(accept(c, 0, context), accept(c, 1, context));
		}
		return map;
	}

	public Object castExpression(SimpleNode node, Context context) {
		SimpleNode typeNode = node.jjtGetChild(0);
		Class type = resolveTypeNode(typeNode, context);
		Object obj = accept(node, 1, context);
		return Runtime.cast(context, type, obj, true);
	}

	public Object classNode(SimpleNode node, Context context) {
		SimpleNode c = node.jjtGetChild(0);
		String className = null;
		if (c.id == PnutsParserTreeConstants.JJTCLASSNAME){
		    StringBuffer sbuf = new StringBuffer();
		    sbuf.append(c.children[0].str);
		    int n = c.jjtGetNumChildren();
		    for (int i = 1; i < n; i++) {
			SimpleNode ch = c.children[i];
			sbuf.append('.');
			sbuf.append(ch.str);
		    }
		    className = sbuf.toString();
		} else {
		    className = (String)c.accept(this, context);
		}
		try {
		    return Pnuts.loadClass(className, context);
		} catch (ClassNotFoundException e) {
		    throw new PnutsException(e, context);
		}
	}

	Object buildSubclassInstance(SimpleNode node, Context context){
	    return compiler.buildSubclassInstance(node, context);
	}

	public Object newNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		SimpleNode n0 = node.jjtGetChild(0);
		if (n == 3 && node.jjtGetChild(2).id == PnutsParserTreeConstants.JJTCLASSDEFBODY) { // subclass
		    return buildSubclassInstance(node, context);

		} else if (n0.id == PnutsParserTreeConstants.JJTINDEXNODE) {
			Object[] idx = parseIndex(n0);
			SimpleNode c = (SimpleNode) idx[0];
			Object[] dim = (Object[]) idx[1];
			Class cls = resolveClassNameNode(c, context);
			return newArray(cls, dim, context);

		} else if (n0.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
			return arrayType(n0, context);

		} else if (n0.id == PnutsParserTreeConstants.JJTARRAYNODE) {
			Class type = (Class) accept(n0, 0, context);
			Object obj = _listElements(n0.jjtGetChild(1), context);
			return Runtime.cast(context, type, obj, true);

		} else { // instantiation
			SimpleNode nameNode = node.jjtGetChild(0);
			Class cls = resolveClassNameNode(nameNode, context);
			Object[] arg = _listElements(node.jjtGetChild(1), context);
			return Runtime.callConstructor(context, cls, arg, null);
		}
	}

       public Object classDef(SimpleNode node, Context context){
		return compiler.defineClass(node, context);
       }

       public Object methodDef(SimpleNode node, Context context){
           return null;
       }

       public Object classDefBody(SimpleNode node, Context context){
           return null;
       }

       public Object classScript(SimpleNode node, Context context){
	   return null;
       }

	public Object packageNode(SimpleNode node, Context context) {
	    int n = node.jjtGetNumChildren();
	    if (n == 0){
		if ("(".equals(node.str)){
		    return PnutsFunction.PACKAGE.call(NO_PARAM, context);
		} else {
		    return PnutsFunction.PACKAGE;
		}
	    } else {
		SimpleNode c = node.jjtGetChild(0);
		if (n == 1 && c.id != PnutsParserTreeConstants.JJTPACKAGE){ // package("...")
		    return PnutsFunction.PACKAGE.call(new Object[]{c.accept(this, context)}, context);
		} else {
		    StringBuffer sbuf = new StringBuffer();
		    sbuf.append(node.jjtGetChild(0).str);
		    for (int i = 1; i < n; i++) {
			sbuf.append('.');
			sbuf.append(node.jjtGetChild(i).str);
		    }
		    return PnutsFunction.PACKAGE.call(new Object[]{sbuf.toString()}, context);
		}
	    }
	}

	public Object importNode(SimpleNode node, Context context) {
		String t2 = node.str;
		int n = node.jjtGetNumChildren();
		if (n == 0) {
			if ("*".equals(t2)) { // import *
				context.addPackageToImport("");
				return null;
			} else if ("(".equals(t2)) {
				return context.importEnv.list();
			} else {
				return PnutsFunction.IMPORT;
			}
		}
		if (n == 1
		    && node.children[0].id != PnutsParserTreeConstants.JJTPACKAGE) { // import(...)
			String s = (String) node.children[0].accept(this, context);
			if (s == null) {
				context.importEnv = new ImportEnv();
			} else if (!s.endsWith("*")) {
				context.addClassToImport(s);
			} else {
				int idx = s.lastIndexOf('.');
				if (idx > 0) {
					context.addPackageToImport(s.substring(0, idx));
				} else {
					context.addPackageToImport("");
				}
			}
			return null;
		}
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(node.children[0].str);
		for (int i = 1; i < n; i++) {
			sbuf.append('.');
			sbuf.append(node.children[i].str);
		}
		String arg = sbuf.toString();
		if (node.info != null) { // static import
			context.addStaticMembers(arg, t2 != null);
		} else {
			if (t2 == null) {
				context.addClassToImport(arg);
			} else {
				context.addPackageToImport(arg);
			}
		}
		return null;
	}

	public Object rangeNode(SimpleNode node, Context context) {
		Object target = accept(node, 0, context);
		Object idx1 = accept(node, 1, context);
		Object idx2 = null;
		if (node.jjtGetNumChildren() >= 3) {
			idx2 = accept(node, 2, context);
		}
		return Runtime.getRange(target, idx1, idx2, context);
	}

	/**
	 * @return [base_component_node, [idx_node_0, idx_node_1, ...]]
	 */
	static Object[] parseIndex(SimpleNode node) {
		SimpleNode c0 = node.jjtGetChild(0);
		SimpleNode c1 = node.jjtGetChild(1);
		if (c0.id != PnutsParserTreeConstants.JJTINDEXNODE) {
			return new Object[] { c0, new Object[] { c1 } };
		} else {
			Object[] r = parseIndex(c0);
			Object[] d = (Object[]) r[1];
			Object[] a = new Object[d.length + 1];
			System.arraycopy(d, 0, a, 0, d.length);
			a[d.length] = c1;
			return new Object[] { r[0], a };
		}
	}

	static void convertIndexNode(SimpleNode node) {
		SimpleNode n0 = node.jjtGetChild(0);
		SimpleNode n1 = node.jjtGetChild(1);
		if (ConstraintsTransformer.isPredicate(n1)) {
			SimpleNode n = ConstraintsTransformer.buildFunc(n1);
			node.jjtAddChild(n, 1);
			n.jjtSetParent(node);
		}
		if (n0.id == PnutsParserTreeConstants.JJTINDEXNODE) {
			convertIndexNode(n0);
		}
	}

	public Object indexNode(SimpleNode node, Context context) {
		synchronized (node) {
			if (node.getAttribute("index") == null) {
				node.setAttribute("index", Boolean.TRUE);
				convertIndexNode(node);
			}
		}
		Object[] idx = parseIndex(node);
		SimpleNode c = (SimpleNode) idx[0];
		Object[] dim = (Object[]) idx[1];
		Object target = c.accept(this, context);
		if (target instanceof Class) {
			return newArray((Class) target, dim, context);
		} else {
			for (int i = 0; i < dim.length; i++) {
				target = Runtime.getElement(target, ((SimpleNode) dim[i])
						.accept(this, context), context);
			}
			return target;
		}
	}

	Object newArray(Class cls, Object[] dim, Context context) {
		int[] sizes = new int[dim.length];
		for (int i = 0; i < dim.length; i++) {
			sizes[i] = ((Number) ((SimpleNode) dim[i]).accept(this, context))
					.intValue();
		}
		return Array.newInstance(cls, sizes);
	}

	public Object methodNode(SimpleNode node, Context context) {
		Object target = accept(node, 0, context);
		Object[] arg = _listElements(node.jjtGetChild(1), context);

		SimpleNode argNode = node.children[1];
		Class types[] = null;
		for (int i = 0; i < arg.length; i++) {
			SimpleNode n = argNode.children[i];
			if (n.id == PnutsParserTreeConstants.JJTCASTEXPRESSION) {
				if (types == null) {
					types = new Class[arg.length];
				}
				types[i] = resolveType(n.jjtGetChild(0), context);
			}
		}

		if (target == null) {
			PnutsException pe = new PnutsException(new NullPointerException(),
					context);
			pe.line = node.beginLine;
			throw pe;
		}
		return Runtime.callMethod(context, target.getClass(), node.str, arg,
				types, target);
	}

	public Object staticMethodNode(SimpleNode node, Context context) {
		SimpleNode c1 = node.children[0];
		String pkgName = getPackageName(c1);

		SimpleNode argNode = node.children[1];
		Class types[] = null;
		Object[] arg = null;

		Package pkg = Package.find(pkgName, context);
		if (pkg != null) {
			Object fun = pkg.get(node.str, context);

			if (fun instanceof PnutsFunction) {
				arg = _listElements(node.jjtGetChild(1), context);
				PnutsFunction ft = (PnutsFunction) fun;
				return ft.exec(arg, context);
			} else if (fun instanceof Class) {
				arg = _listElements(node.jjtGetChild(1), context);
				for (int i = 0; i < arg.length; i++) {
					SimpleNode n = argNode.children[i];
					if (n.id == PnutsParserTreeConstants.JJTCASTEXPRESSION) {
						if (types == null) {
							types = new Class[arg.length];
						}
						types[i] = resolveType(n.jjtGetChild(0), context);
					}
				}
				return Runtime
						.callConstructor(context, (Class) fun, arg, types);
			} else {
				throw new PnutsException("illegal.staticCall", new Object[] {
						pkgName, node.str,
						new Integer(argNode.jjtGetNumChildren()) }, context);
			}
		}
		Object target = accept(node, 0, context);
		arg = _listElements(node.jjtGetChild(1), context);
		if (target instanceof Class) {
			for (int i = 0; i < arg.length; i++) {
				SimpleNode n = argNode.children[i];
				if (n.id == PnutsParserTreeConstants.JJTCASTEXPRESSION) {
					if (types == null) {
						types = new Class[arg.length];
					}
					types[i] = resolveType(n.jjtGetChild(0), context);
				}
			}
			return Runtime.callMethod(context, (Class) target, node.str, arg,
					types, null);
		} else {
			throw new PnutsException("illegal.staticCall", new Object[] {
					pkgName, node.str, new Integer(arg.length) }, context);
		}
	}

	public Object memberNode(SimpleNode node, Context context) {
		Object target = accept(node, 0, context);
		
		if (target == null) {
			throw new PnutsException(new NullPointerException(), context);
		} else if (Runtime.isArray(target) && node.str.equals("length")) {
			return new Integer(Runtime.getArrayLength(target));
		} 
		if (node.info != null){ /* experimental BIND feature */
		    Object[] info = (Object[])node.info;
		    Object obj = info[0];
		    SimpleNode c = (SimpleNode)info[1];
		    SimpleNode c0 = c.jjtGetChild(0);
		    Map table = (Map)info[2];
		    watchProperty(table, obj, c.str, target, node.str,
				  (Callable)toFunctionNode(c0).accept(this, context));
		    if (c0 == node){
			watchProperty(table, target, node.str, obj, c.str, null);
		    }
		    node.info = null;
		}
		return Runtime.getField(context, target, node.str);
	}

	static String getPackageName(SimpleNode node) {
		if (node.jjtGetNumChildren() > 0) {
			SimpleNode c1 = node.children[0];
			return getPackageName(c1) + "::" + node.str;
		} else {
			return node.str;
		}
	}

	public Object staticMemberNode(SimpleNode node, Context context) {

		SimpleNode c1 = node.children[0];
		String pkgName = getPackageName(c1);

		Package pkg = Package.find(pkgName, context);
		if (pkg != null) {
			Value v = pkg.lookup(node.str, context);
			if (v != null) {
				return v.get();
			} else {
				return context.undefined(node.str);
			}
		}
		Object target = c1.accept(this, context);
		if (target instanceof Class) {
			return Runtime.getStaticField(context, (Class) target, node.str);
		} else {
			throw new PnutsException("packageOrClass.expected",
					new Object[] { Pnuts.format(target) }, context);
		}
	}

	public Object applicationNode(SimpleNode node, Context context) {

		Object target = accept(node, 0, context);
		Object[] arg = _listElements(node.jjtGetChild(1), context);

		SimpleNode argNode = node.children[1];
		Class types[] = null;
		for (int i = 0; i < arg.length; i++) {
			SimpleNode n = argNode.children[i];
			if (n.id == PnutsParserTreeConstants.JJTCASTEXPRESSION) {
				if (types == null) {
					types = new Class[arg.length];
				}
				types[i] = resolveType(n.jjtGetChild(0), context);
			}
		}
		return Runtime.call(context, target, arg, types, node.beginLine, node.beginColumn);
	}

	// node.info => [int[] unitOffset]

	public Object integerNode(SimpleNode node, Context context) {
		if (node.value != null) {
			return node.value;
		}
		String str = node.str;
		Object[] a = (Object[]) node.info;
		int[] off = (int[]) a[1];
		Number n = (Number) a[0];
		if (off != null) {
			int offset = off[0];
			return Runtime.quantity(n, str.substring(0, offset), str
					.substring(offset), context);
		} else {
			node.value = n;
			return n;
		}
	}

	public Object floatingNode(SimpleNode node, Context context) {
		if (node.value != null) {
			return node.value;
		}
		String str = node.str;
		Object[] p = (Object[]) node.info;
		Number n = (Number) p[0];
		if (p[1] != null) {
			int offset = ((int[]) p[1])[0];
			return Runtime.quantity(n, str.substring(0, offset), str
					.substring(offset), context);
		} else {
			node.value = n;
			return n;
		}
	}

	public Object characterNode(SimpleNode node, Context context) {
		return node.info;
	}

	public Object stringNode(SimpleNode node, Context context) {
	    String str = node.str;
	    if (str != null){
		return str;
	    } else {
		int n = node.jjtGetNumChildren();
		StringBuffer sbuf = new StringBuffer();
		for (int i = 0; i < n; i++){
		    SimpleNode c = node.jjtGetChild(i);
		    if (c.id == PnutsParserTreeConstants.JJTSTRINGNODE){
			sbuf.append(c.str);
		    } else {
			sbuf.append(String.valueOf(c.accept(this, context)));
		    }
		}
		return sbuf.toString();
	    }
	}

	public Object trueNode(SimpleNode node, Context context) {
		return Boolean.TRUE;
	}

	public Object falseNode(SimpleNode node, Context context) {
		return Boolean.FALSE;
	}

	public Object nullNode(SimpleNode node, Context context) {
		return null;
	}

	private void doAssign(String symbol, Object expr, Context context){
		if (context.inGeneratorClosure){
			if (context.frame.outer != null){
				context.setValue(symbol, expr);
			} else {
				context.getCurrentPackage().set(symbol, expr);
			}
		} else {
			context.setValue(symbol, expr);
		}
	}

	private Object doAssign(SimpleNode node, Object expr, Context context,
			BinaryOperator op) {
		if (node.id == PnutsParserTreeConstants.JJTIDNODE) {
			if (op != null) {
				expr = op.operateOn(context.getValue(node.str), expr);
			}
			doAssign(node.str, expr, context);
			return expr;
		} else if (node.id == PnutsParserTreeConstants.JJTGLOBAL) {
			Package pkg = Package.getPackage("", context);
			if (op != null) {
				expr = op.operateOn(pkg.get(node.str, context), expr);
			}
			pkg.set(node.str, expr, context);
			return expr;

		} else if (node.id == PnutsParserTreeConstants.JJTINDEXNODE) {
			Object o = accept(node, 0, context);
			Object idx = accept(node, 1, context);
			if (o instanceof String) {
				if (op != null || !(expr instanceof Character)) {
					throw new PnutsException("illegal.assign", NO_PARAM,
							context);
				}
				expr = Runtime.replaceChar((String)o, (Number)idx, expr);
				String sym = node.jjtGetChild(0).str;
				if (sym != null){
				    context.setValue(sym, expr);
				}
				return expr;
			} else {
				if (op != null) {
					expr = op.operateOn(Runtime.getElement(o, idx, context),
							expr);
				}
				Runtime.setElement(o, idx, expr, context);
				return expr;
			}
		} else if (node.id == PnutsParserTreeConstants.JJTSTATICMEMBERNODE) {
			SimpleNode c1 = node.children[0];
			String pkgName = getPackageName(c1);
			Package pkg = Package.find(pkgName, context);
			if (pkg != null) {
				if (op != null) {
					Object f = pkg.get(node.str, context);
					expr = op.operateOn(f, expr);
				}
				pkg.set(node.str, expr, context);
				return expr;
			}
			Object target = c1.accept(this, context);
			if (target instanceof Class) {
				Class clazz = (Class) target;
				if (op != null) {
					expr = op.operateOn(Runtime.getStaticField(context, clazz,
							node.str), expr);
				}
				Runtime.putStaticField(context, clazz, node.str, expr);
				return expr;
			} else {
				throw new PnutsException("package.notFound", NO_PARAM, context);
			}
		} else if (node.id == PnutsParserTreeConstants.JJTMEMBERNODE) {
			Object target = accept(node, 0, context);
			if (op != null) {
				expr = op.operateOn(
						Runtime.getField(context, target, node.str), expr);
			}
			Runtime.putField(context, target, node.str, expr);
			return expr;
		} else if (node.id == PnutsParserTreeConstants.JJTRANGENODE) {
			if (op != null) {
				throw new PnutsException("illegal.assign", NO_PARAM, context);
			}
			Object target = accept(node, 0, context);
			Object idx1 = accept(node, 1, context);
			Object idx2 = null;
			if (node.jjtGetNumChildren() >= 3) {
				idx2 = accept(node, 2, context);
			}
			Object value = setRange(target, idx1, idx2, expr, context);
			if (target instanceof String) {
				SimpleNode cld = node.children[0];

				if (cld.id == PnutsParserTreeConstants.JJTIDNODE
						|| cld.id == PnutsParserTreeConstants.JJTGLOBAL
						|| cld.id == PnutsParserTreeConstants.JJTINDEXNODE
						|| cld.id == PnutsParserTreeConstants.JJTSTATICMEMBERNODE
						|| cld.id == PnutsParserTreeConstants.JJTMEMBERNODE) {
					return doAssign(cld, value, context, null);
				} else {
					return value;
				}
			}
			return value;
		} else {
			throw new PnutsException("illegal.assign", NO_PARAM, context);
		}
	}

	private Object assign(SimpleNode node, Context context, BinaryOperator op) {

		Object expr = accept(node, 1, context);
		try {
			SimpleNode lhs = node.children[0];
			if (lhs.id == PnutsParserTreeConstants.JJTMULTIASSIGNLHS){
			    int n = lhs.jjtGetNumChildren();
			    for (int i = 0; i < n; i++){
				    doAssign(lhs.children[i].str,
					     Runtime.getElementAt(expr, i, context),
					     context);
			    }
			    return expr;
			} else {
			    return doAssign(lhs, expr, context, op);
			}
		} catch (PnutsException p) {
			throw p;
		} catch (Throwable t) {
			throw new PnutsException(t, context);
		}
	}

	public Object assignment(SimpleNode node, Context context) {
		return assign(node, context, null);
	}

	public Object assignmentTA(SimpleNode node, Context context) {
		return assign(node, context, context._multiply);
	}

	public Object assignmentMA(SimpleNode node, Context context) {
		return assign(node, context, context._mod);
	}

	public Object assignmentDA(SimpleNode node, Context context) {
		return assign(node, context, context._divide);
	}

	public Object assignmentPA(SimpleNode node, Context context) {
		return assign(node, context, context._add);
	}

	public Object assignmentSA(SimpleNode node, Context context) {
		return assign(node, context, context._subtract);
	}

	public Object assignmentLA(SimpleNode node, Context context) {
		return assign(node, context, context._shiftLeft);
	}

	public Object assignmentRA(SimpleNode node, Context context) {
		return assign(node, context, context._shiftRight);
	}

	public Object assignmentRAA(SimpleNode node, Context context) {
		return assign(node, context, context._shiftArithmetic);
	}

	public Object assignmentAA(SimpleNode node, Context context) {
		return assign(node, context, context._and);
	}

	public Object assignmentEA(SimpleNode node, Context context) {
		return assign(node, context, context._xor);
	}

	public Object assignmentOA(SimpleNode node, Context context) {
		return assign(node, context, context._or);
	}

	public Object logOrNode(SimpleNode node, Context context) {
		Object o1 = accept(node, 0, context);
		if (!(o1 instanceof Boolean)) {
			o1 = Runtime.toBoolean(o1);
		}
		if (((Boolean) o1).booleanValue()) {
			return Boolean.TRUE;
		} else {
			Object o2 = accept(node, 1, context);
			if (!(o2 instanceof Boolean)) {
				o2 = Runtime.toBoolean(o2);
			}
			return o2;
		}
	}

	public Object logAndNode(SimpleNode node, Context context) {
		Object o1 = accept(node, 0, context);
		if (!(o1 instanceof Boolean)) {
			o1 = Runtime.toBoolean(o1);
		}
		if (((Boolean) o1).booleanValue()) {
			Object o2 = accept(node, 1, context);
			if (!(o2 instanceof Boolean)) {
				o2 = Runtime.toBoolean(o2);
			}
			return o2;
		} else {
			return Boolean.FALSE;
		}
	}

	public Object orNode(SimpleNode node, Context context) {
		return context._or.operateOn(accept(node, 0, context), accept(node, 1,
				context));
	}

	public Object xorNode(SimpleNode node, Context context) {
		return context._xor.operateOn(accept(node, 0, context), accept(node, 1,
				context));
	}

	public Object andNode(SimpleNode node, Context context) {
		return context._and.operateOn(accept(node, 0, context), accept(node, 1,
				context));
	}

	public Object equalNode(SimpleNode node, Context context) {
		return eq(accept(node, 0, context), accept(node, 1, context), context) ? Boolean.TRUE
				: Boolean.FALSE;
	}

	public Object notEqNode(SimpleNode node, Context context) {
		return eq(accept(node, 0, context), accept(node, 1, context), context) ? Boolean.FALSE
				: Boolean.TRUE;
	}

	public Object instanceofExpression(SimpleNode node, Context context) {
		Object o1 = accept(node, 0, context);
		Class type = resolveType(node.jjtGetChild(1), context);
		return (type.isInstance(o1) ? Boolean.TRUE : Boolean.FALSE);
	}

	public Object ltNode(SimpleNode node, Context context) {
		if (context._lt.operateOn(accept(node, 0, context), accept(node, 1,
				context))) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}

	public Object gtNode(SimpleNode node, Context context) {
		if (context._gt.operateOn(accept(node, 0, context), accept(node, 1,
				context))) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}

	public Object leNode(SimpleNode node, Context context) {
		if (context._le.operateOn(accept(node, 0, context), accept(node, 1,
				context))) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}

	public Object geNode(SimpleNode node, Context context) {
		if (context._ge.operateOn(accept(node, 0, context), accept(node, 1,
				context))) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}

	public Object shiftLeftNode(SimpleNode node, Context context) {
		try {
			return context._shiftLeft.operateOn(accept(node, 0, context),
							    accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object shiftRightNode(SimpleNode node, Context context) {
		try {
			return context._shiftRight.operateOn(accept(node, 0, context),
							     accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object shiftArithmeticNode(SimpleNode node, Context context) {
		try {
			return context._shiftArithmetic.operateOn(accept(node, 0, context),
								  accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object addNode(SimpleNode node, Context context) {
		try {
			return context._add.operateOn(accept(node, 0, context),
						      accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object subtractNode(SimpleNode node, Context context) {
		try {
			return context._subtract.operateOn(accept(node, 0, context),
							   accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object multNode(SimpleNode node, Context context) {
		try {
			return context._multiply.operateOn(accept(node, 0, context),
							   accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object divideNode(SimpleNode node, Context context) {
		try {
			return context._divide.operateOn(accept(node, 0, context),
							 accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object modNode(SimpleNode node, Context context) {
		try {
			return context._mod.operateOn(accept(node, 0, context),
						      accept(node, 1, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object negativeNode(SimpleNode node, Context context) {
		try {
			return context._negate.operateOn(accept(node, 0, context));
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	public Object preIncrNode(SimpleNode node, Context context) {
		return doAssign(node.children[0], one, context, context._add);
	}

	public Object preDecrNode(SimpleNode node, Context context) {
		return doAssign(node.children[0], one, context, context._subtract);
	}

	public Object postIncrNode(SimpleNode node, Context context) {
		SimpleNode n = node.children[0];
		Object ret = n.accept(this, context);
		doAssign(n, one, context, context._add);
		return ret;
	}

	public Object postDecrNode(SimpleNode node, Context context) {
		SimpleNode n = node.children[0];
		Object ret = n.accept(this, context);
		doAssign(n, one, context, context._subtract);
		return ret;
	}

	public Object notNode(SimpleNode node, Context context) {
		return context._not.operateOn(accept(node, 0, context));
	}

	public Object logNotNode(SimpleNode node, Context context) {
		Object o = accept(node, 0, context);
		if (o instanceof Boolean) {
			return ((Boolean) o).booleanValue() ? Boolean.FALSE : Boolean.TRUE;
		} else {
			return Runtime.toBoolean(o).booleanValue() ? Boolean.FALSE : Boolean.TRUE;
		}
	}

	public Object continueNode(SimpleNode node, Context context) {
		throw new Runtime.Continue();
	}

	public Object breakNode(SimpleNode node, Context context) {
		Object o = null;
		if (node.jjtGetNumChildren() > 0) {
			o = accept(node, 0, context);
		}

		if (context.inGeneratorClosure) {
			throw new Generator.Break(o);
		} else {
			throw new Runtime.Break(o);
		}
	}

	public Object returnNode(SimpleNode node, Context context) {
		Object o = null;
		if (node.jjtGetNumChildren() > 0) {
			o = accept(node, 0, context);
		}
		throw new Jump(o);
	}

	public Object yieldNode(SimpleNode node, Context context) {
		throw new PnutsException("yield must be used in a generator function",
				context);
	}

	public Object catchNode(SimpleNode node, Context context) {
		int nargs = node.jjtGetNumChildren();
		if (nargs == 0) {
			return PnutsFunction.CATCH;
		} else if (nargs == 1) {
			Class cls = (Class) accept(node, 0, context);
			PnutsFunction func = (PnutsFunction) accept(node, 1, context);
			context.catchException(cls, func);
		}
		return null;
	}

	public Object throwNode(SimpleNode node, Context context) {
		int nargs = node.jjtGetNumChildren();
		if (nargs == 0) {
			return PnutsFunction.THROW;
		} else if (nargs == 1) {
			Object arg = accept(node, 0, context);
			if (arg instanceof PnutsException){
				throw (PnutsException)arg;
			} else if (arg instanceof Throwable) {
				throw new PnutsException((Throwable) arg, context);
			} else {
				throw new PnutsException(String.valueOf(arg), context);
			}
		}
		return null;
	}

	public Object finallyNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n == 1) {
			PnutsFunction f = (PnutsFunction) accept(node, 0, context);
			context.setFinallyFunction(f);
			return f;
		} else if (n == 2) {
			PnutsFunction f0 = (PnutsFunction) accept(node, 0, context);
			PnutsFunction f1 = (PnutsFunction) accept(node, 1, context);
			try {
				return f0.exec(NO_PARAM, context);
			} finally {
				f1.exec(NO_PARAM, context);
			}
		}
		return null;
	}

	public Object tryStatement(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		SimpleNode lastNode = node.jjtGetChild(n - 1);
		PnutsException pe = null;
		Object value = null;
		boolean handled = false;
		SimpleNode finallyBlock = null;
		if (lastNode.id == PnutsParserTreeConstants.JJTFINALLYBLOCK) {
			finallyBlock = lastNode;
		}
		try {
			value = accept(node, 0, context);
		} catch (Escape esc) {
			throw esc;
		} catch (Runtime.Break brk) {
			throw brk;
		} catch (Runtime.Continue cont) {
			throw cont;
		} catch (Throwable t) {
			if (t instanceof PnutsException) {
				pe = (PnutsException) t;
				t = ((PnutsException) t).getThrowable();
			}
			for (int i = 1; i < n; i++) {
				SimpleNode c = node.jjtGetChild(i);
				if (c.id == PnutsParserTreeConstants.JJTCATCHBLOCK) {
					String var = c.str;
					StringBuffer sbuf = new StringBuffer();
					SimpleNode classNode = c.jjtGetChild(0);
					sbuf.append(classNode.jjtGetChild(0).str);
					for (int j = 1; j < classNode.jjtGetNumChildren(); j++) {
						sbuf.append('.');
						sbuf.append(classNode.jjtGetChild(j).str);
					}
					Class cls = context.resolveClass(sbuf.toString());

					if (cls != null && cls.isInstance(t)) {
						try {
							context.openLocal(new String[] {});
							context.bind(var, t);
							return accept(c, 1, context);
						} finally {
							context.closeLocal();
						}
					}
				}
			}
			if (pe != null) {
				throw pe;
			} else {
				throw new PnutsException(t, context);
			}
		} finally {
			if (finallyBlock != null) {
				accept(finallyBlock, 0, context);
			}
		}
		return value;
	}

       public Object catchBlock(SimpleNode node, Context context){
           return null;
       }

	public Object blockNode(SimpleNode node, Context context) {
		Object last = null;
		int n = node.jjtGetNumChildren();
		for (int i = 0; i < n; i++) {
			last = accept(node, i, context);
		}
		return last;
	}

	public Object ifStatement(SimpleNode node, Context context) {

		Object con = accept(node, 0, context);
		if (condition(con, context)) {
			return accept(node, 1, context);
		}
		int n = node.jjtGetNumChildren();
		for (int i = 2; i < n; i++) {
			SimpleNode _node = node.children[i];
			if (_node.id == PnutsParserTreeConstants.JJTELSEIFNODE) {
				if (condition(accept(_node, 0, context), context)) {
					return accept(_node, 1, context);
				}
			} else if (_node.id == PnutsParserTreeConstants.JJTELSENODE) {
				return accept(_node, 0, context);
			}
		}
		return null;
	}

	static boolean condition(Object cond, Context context) {
		if (cond instanceof Boolean) {
			return ((Boolean) cond).booleanValue();
		} else {
			return (Runtime.toBoolean(cond)).booleanValue();
		}
	}

	public Object doStatement(SimpleNode node, Context context) {
		Object last = null;
		try {
			while (true) {
				try {
					last = accept(node, 0, context);
					Object cond = accept(node, 1, context);
					if (!(cond instanceof Boolean)) {
					    cond = Runtime.toBoolean(cond);
					}
					boolean b = ((Boolean) cond).booleanValue();
					if (!b) {
					    return last;
					}
				} catch (Runtime.Continue cont) {
				}
			}
		} catch (Runtime.Break brk) {
			return brk.getValue();
		}
	}

	public Object whileStatement(SimpleNode node, Context context) {
		Object last = null;
		try {
			while (true) {
				Object cond = accept(node, 0, context);
				if (!(cond instanceof Boolean)) {
				    cond = Runtime.toBoolean(cond);
				}
				try {
				    boolean b = ((Boolean) cond).booleanValue();
				    if (b) {
					last = accept(node, 1, context);
				    } else {
					return last;
				    }
				} catch (Runtime.Continue cont) {
				}
			}
		} catch (Runtime.Break brk) {
			return brk.getValue();
		}
	}

	public Object forStatement(SimpleNode node, final Context context) {
		SimpleNode initNode = null;
		SimpleNode condNode = null;
		SimpleNode updateNode = null;
		SimpleNode blockNode = null;
		int j = 0;
		SimpleNode n = node.children[j];
		if (n.id == PnutsParserTreeConstants.JJTFORENUM) {
			int nc = n.jjtGetNumChildren();
			SimpleNode node0 = n.children[0];
			String[] vars;
			Object n0;
			if (node0.id == PnutsParserTreeConstants.JJTMULTIASSIGNLHS){
			    int nc2 = node0.jjtGetNumChildren();
			    vars = new String[nc2];
			    for (int i = 0; i < nc2; i++){
				vars[i] = node0.jjtGetChild(i).str;
			    }
			    n0 = n.children[1].accept(this, context);
			} else {
			    vars = new String[]{n.str};
			    n0 = node0.accept(this, context);
			}
			blockNode = node.children[1];
			if (node0.id == PnutsParserTreeConstants.JJTMULTIASSIGNLHS || nc == 1) {
				return doForeach(vars, n0, blockNode, context);
			} else if (nc == 2) {
				Object n1 = n.children[1].accept(this, context);
				int start = ((Number) n0).intValue();
				int end = ((Number) n1).intValue();
				return doForeach(n.str, start, end, blockNode, context);
			} else {
				throw new IllegalArgumentException();
			}
		} else if (n.id == PnutsParserTreeConstants.JJTFORINIT) {
			initNode = n;
			j++;
		}
		n = node.children[j];
		if (n.id != PnutsParserTreeConstants.JJTFORUPDATE
				&& n.id != PnutsParserTreeConstants.JJTBLOCK) {
			condNode = n;
			j++;
		}
		n = node.children[j];
		if (n.id == PnutsParserTreeConstants.JJTFORUPDATE) {
			updateNode = n;
			j++;
		}

		blockNode = node.children[j];

		Object last = null;

		if (initNode != null) {
			int num = initNode.jjtGetNumChildren();
			String[] env = new String[num];
			for (int i = 0; i < env.length; i++) {
				SimpleNode sn = initNode.children[i];
				env[i] = sn.str;
			}
			context.openLocal(env);
			for (int i = 0; i < env.length; i++) {
				SimpleNode sn = initNode.children[i];
				context.bind(env[i], accept(sn, 0, context));
			}
		} else {
			context.openLocal(new String[] {});
		}

		Object c = null;
		if (condNode != null) {
			c = condNode.accept(this, context);
		} else {
			c = Boolean.TRUE;
		}
		try {
			while (true) {
				if (!(c instanceof Boolean)) {
					c = Runtime.toBoolean(c);
				}
				if (!((Boolean) c).booleanValue()) {
					break;
				}
				try {
					last = blockNode.accept(this, context);
				} catch (Runtime.Continue cont) {
				}
				if (updateNode != null) {
					expressionList(updateNode, context);
				}
				if (condNode != null) {
					c = condNode.accept(this, context);
				}
			}
		} catch (Runtime.Break brk) {
			last = brk.getValue();
		} finally {
			context.closeLocal();
		}
		return last;
	}

	private Object doForeach(String var, int start, int end,
			SimpleNode blockNode, Context context) {
		Object last = null;
		context.openLocal(new String[] { var });
		int delta;
		try {
			if (start < end) {
				for (int i = start; i <= end; i++) {
					try {
						context.bind(var, new Integer(i));
						last = blockNode.accept(this, context);
					} catch (Runtime.Continue cont) {
					}
				}
			} else {
				for (int i = start; i >= end; i--) {
					try {
						context.bind(var, new Integer(i));
						last = blockNode.accept(this, context);
					} catch (Runtime.Continue cont) {
					}
				}
			}
		} catch (Runtime.Break brk) {
			last = brk.getValue();
		} finally {
			context.closeLocal();
		}
		return last;
	}

	private Object doForeach(final String[] vars, Object array,
			SimpleNode blockNode, final Context context) {
		if (array == null) {
			return null;
		}
		if (array instanceof Generator) {
			final String tmpVar = "_tmp".intern();
			if (vars.length > 1){
				blockNode = GeneratorHelper.expandMultiAssign(vars, tmpVar, blockNode);
			}
			Function f = new Function(null,
						  vars.length > 1 ? new String[]{tmpVar} : vars,
						  1,
						  false,
						  blockNode,
						  context.getCurrentPackage(),
						  context)
				{
					protected Object exec(Object[] args, Context context){
						boolean inGeneratorClosure = context.inGeneratorClosure;
						try {
							context.inGeneratorClosure = true;
							return super.exec(args, context);
						} finally {
							context.inGeneratorClosure = inGeneratorClosure;
						}
					}
				};
			return Runtime.applyGenerator((Generator) array, f.register(null), context);
		}

		Enumeration e = Runtime.toEnumeration(array, context);
		if (e == null) {
			throw new PnutsException("illegal.type.foreach",
					new Object[] { Pnuts.format(array) }, context);
		}
		Object last = null;
		context.openLocal(vars);
		try {
		    if (vars.length == 1){
			while (e.hasMoreElements()) {
				try {
				    context.bind(vars[0], e.nextElement());
				    last = blockNode.accept(this, context);
				} catch (Runtime.Continue cont) {
				}
			}
		    } else {
			while (e.hasMoreElements()) {
				try {
				    Object elem = e.nextElement();
				    for (int i = 0; i < vars.length; i++){
					context.bind(vars[i], Runtime.getElementAt(elem, i, context));
				    }
				    last = blockNode.accept(this, context);
				} catch (Runtime.Continue cont) {
				}
			}
		    }
		} catch (Runtime.Break brk) {
			last = brk.getValue();
		} finally {
			context.closeLocal();
		}
		return last;
	}

	public Object foreachStatement(SimpleNode node, Context context) {
		SimpleNode n1 = node.children[0];
		SimpleNode n2 = node.children[1];
		String var = node.str;

		return doForeach(new String[]{var}, n1.accept(this, context), n2, context);
	}

	public Object switchStatement(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		Object target = accept(node, 0, context);
		Object last = null;
		boolean matched = false;
		try {
			for (int i = 1; i < n; i++) {
				SimpleNode _node = node.children[i];
				if (_node.jjtGetNumChildren() == 1) { // case
					Object o = accept(_node, 0, context);
					i++;
					if (matched || eq(target, o, context)) {
						last = accept(node, i, context);
						matched = true;
					}
				} else { // default
					matched = true;
					last = accept(node, ++i, context);
				}
			}
		} catch (Runtime.Break brk) {
			last = brk.getValue();
		}
		return last;
	}

	public Object switchBlock(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		Object last = null;
		for (int i = 0; i < n; i++) {
			last = accept(node, i, context);
		}
		return last;
	}

	public Object functionStatement(SimpleNode node, Context context) {
		SimpleNode param = node.jjtGetChild(0);
		int nargs = param.jjtGetNumChildren();
		String[] locals = new String[nargs];
		SimpleNode n0 = null;
		SimpleNode block = node.jjtGetChild(1);

		synchronized (node) {
			if (node.getAttribute("type") == null) {
				if (Runtime.isGenerator(block)) {
					node.setAttribute("type", GENERATOR);
				} else {
					node.setAttribute("type", FUNCTION);
				}
			}
		}

		boolean varargs = "[".equals(param.str);

		for (int j = 0; j < nargs; j++) {
			locals[j] = param.children[j].str;
		}

		String symbol = node.str;
		StackFrame stackFrame = context.stackFrame;
		Package pkg = context.getCurrentPackage();

		Function f;
		if (node.getAttribute("type") == GENERATOR) {
			f = new Generator.GeneratorFunction(symbol, locals, nargs, block,
					pkg, context);
		} else {
			f = new Function(symbol, locals, nargs, varargs, block, pkg, context);
		}
//		String name = f.getName();
		PnutsFunction ht = null;

		if (symbol != null) {
			if (stackFrame.parent != null) {
				Object o = null;
				Binding b = (Binding) stackFrame.lookup(symbol);
				boolean found = false;
				if (b != null) {
					o = b.value;
					if (o instanceof PnutsFunction) {
						ht = f.register((PnutsFunction) o);
					} else {
						ht = Runtime.defineUnboundFunction(f, symbol, pkg);
					}
					found = true;
				} else {
					Function ff = context.frame;
					while (ff != null) {
						SymbolTable ls = ff.lexicalScope;
						if (ls != null) {
							Binding bb = ls.lookup0(symbol);
							if (bb != null) {
								o = ((Binding) bb.value).value;
								if (o instanceof PnutsFunction) {
									ht = f.register((PnutsFunction) o, true);
								} else {
									ht = Runtime.defineUnboundFunction(f,
											symbol, pkg);
								}
								found = true;
								break;
							}
						}
						ff = ff.outer;
					}
				}
				if (!found) {
					ht = Runtime.defineUnboundFunction(f, symbol, pkg);
				}

				if (b != null) {
					b.set(ht);
				} else {
					stackFrame.assign(symbol, ht);
				}
			} else {
				ht = Runtime.defineTopLevelFunction(f, symbol, pkg, context);
			}
		} else {
			ht = f.register(null);
		}
		return ht;
	}

	public Object ternary(SimpleNode node, Context context) {
		if (condition(accept(node, 0, context), context)) {
			return accept(node, 1, context);
		} else {
			return accept(node, 2, context);
		}
	}

	protected Object accept(SimpleNode node, int idx, Context context) {
		return node.children[idx].accept(this, context);
	}

	static String getClassName(SimpleNode node){
		int n = node.jjtGetNumChildren();
		if (n > 1){
		    StringBuffer sbuf = new StringBuffer();
		    sbuf.append(node.children[0].str);
		    for (int i = 1; i < n; i++) {
			sbuf.append('.');
			sbuf.append(node.children[i].str);
		    }
		    return sbuf.toString().intern();
		} else {
		    return node.children[0].str;
		}
	}

	static Class resolveTypeNode(SimpleNode typeNode, Context context) {
		Class type = null;
		if (typeNode.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
			SimpleNode n = typeNode;
			int count = 0;
			while (n != null && n.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
				count++;
				n = n.jjtGetChild(0);
			}
			if (n != null && n.id == PnutsParserTreeConstants.JJTCLASSNAME) {
				type = Runtime.arrayType(resolveClassNameNode(n, context),
						count);
			}
		} else if (typeNode.id == PnutsParserTreeConstants.JJTCLASSNAME) {
			type = resolveClassNameNode(typeNode, context);
		} else {
			throw new RuntimeException();
		}
		return type;
	}


	static Class resolveClassNameNode(SimpleNode node, Context context) {
		int n = node.jjtGetNumChildren();
		if (n == 1) {
			String sym = node.children[0].str;
			if (sym == INT_SYMBOL) {
				return Integer.TYPE;
			} else if (sym == SHORT_SYMBOL) {
				return Short.TYPE;
			} else if (sym == CHAR_SYMBOL) {
				return Character.TYPE;
			} else if (sym == BYTE_SYMBOL) {
				return Byte.TYPE;
			} else if (sym == LONG_SYMBOL) {
				return Long.TYPE;
			} else if (sym == FLOAT_SYMBOL) {
				return Float.TYPE;
			} else if (sym == DOUBLE_SYMBOL) {
				return Double.TYPE;
			} else if (sym == BOOLEAN_SYMBOL) {
				return Boolean.TYPE;
			} else if (sym == VOID_SYMBOL) {
				return Void.TYPE;
			}
		}
		String name = getClassName(node);
		Class cls = context.resolveClass(name);
		if (cls == null) {
			throw new PnutsException("class.notFound", new Object[] { name },
					context);
		}
		return cls;
	}

	public Class resolveType(SimpleNode typeNode, Context context) {
		Class type = null;
		if (typeNode.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
			SimpleNode n = typeNode;
			int count = 0;
			while (n != null && n.id == PnutsParserTreeConstants.JJTARRAYTYPE) {
				count++;
				n = n.jjtGetChild(0);
			}
			if (n != null && n.id != PnutsParserTreeConstants.JJTCLASSNAME) {
				throw new RuntimeException();
			}
			return Runtime.arrayType(resolveClassNameNode(n, context), count);
		} else if (typeNode.id == PnutsParserTreeConstants.JJTCLASSNAME) {
			return resolveClassNameNode(typeNode, context);
		} else {
			throw new PnutsException("class.expected", NO_PARAM, context);
		}
	}

       public Object beanDef(SimpleNode node, Context context){
	   SimpleNode classNameNode = node.jjtGetChild(0);
	   Configuration config = context.getConfiguration();

	   boolean marked = (node.info != null); /* experimental BIND feature */
	   Map table = new HashMap();  // temporary hashtable to collect listeners
	   try {
	       Class cls = (Class)classNameNode.accept(this, context);
	       Object target = config.callConstructor(context, cls, NO_PARAM, null);

	       for (int i = 1; i < node.jjtGetNumChildren(); i++){
		   SimpleNode c = node.jjtGetChild(i);
		   boolean bound = "::".equals(c.info);

		   String propertyName = c.str;
		   SimpleNode valueNode = c.jjtGetChild(0);
		   if (bound && !marked){ /* experimental BIND feature */
		       List memberNodes = new ArrayList();
		       c.info = memberNodes;
		       markMemberNodeInBeanDef(c, target, table, valueNode);
		   }
		   Object value = valueNode.accept(this, context);
		   config.putField(context, target, propertyName, value);
	       }
	       if (!marked){
		   Runtime.setupPropertyChangeListeners(table, context);/* experimental BIND feature */
		   node.info = target; 
	       }
	       return target;
	   } catch (Exception e){
	       throw new PnutsException(e, context);
	   }
       }

    static void markMemberNodeInBeanDef(SimpleNode beanPropertyDef, Object target, Map table, SimpleNode node){
	if (node.id == PnutsParserTreeConstants.JJTMEMBERNODE){
	    node.info = new Object[]{target, beanPropertyDef, table};
	} else if (node.id != PnutsParserTreeConstants.JJTFUNCTIONSTATEMENT){
	    for (int i = 0; i < node.jjtGetNumChildren(); i++){
		markMemberNodeInBeanDef(beanPropertyDef, target, table, node.jjtGetChild(i));
	    }
	}
    }


}
