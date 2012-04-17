/*
 * @(#)Function.java 1.6 05/06/21
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;

import org.pnuts.util.Cell;

/**
 * This class represents a function with a certain number of parameters. In
 * Pnuts, functions should be accessed through PnutsFunction. This class is used
 * mainly by compiler implementors. Note that there is no way to create a
 * Function object through public API.
 *  
 */
public class Function extends Runtime implements Serializable {

	static final long serialVersionUID = -997304045111474857L;

	/**
	 * name of the function including the scope information
	 * 
	 * @serial
	 */
	protected String funcName;

	/**
	 * name of the function without the scope information
	 * 
	 * @serial
	 */
	protected String name;

	boolean anonymous;

	/**
	 * the number of arguments
	 * 
	 * @serial
	 */
	protected int nargs;

	/**
	 * variable length arugments
	 * 
	 * @serial
	 */
	protected boolean varargs;

	/**
	 * local parameters
	 * 
	 * @serial
	 */
	protected String locals[];

	/**
	 * file name in which this function is defined
	 *  
	 */
	transient protected Object file;

	/**
	 * reference to the definition
	 *  
	 */
	protected SimpleNode node;

	/**
	 * @serial
	 */
	SymbolTable lexicalScope;

	/**
	 * "import" environment
	 * 
	 * @serial
	 */
	protected ImportEnv importEnv;

	/**
	 * Used modules
	 * 
	 * @serial
	 */
	protected ModuleList moduleList;

	/**
	 * reference to the outer function
	 * 
	 * @serial
	 */
	protected Function outer;

	/**
	 * the package name in which this function is defined
	 * 
	 * @serial
	 */
	protected String pkgName;

	/**
	 * reference to PnutsFunction
	 * 
	 * @serial
	 */
	protected PnutsFunction function;

	protected transient Package pkg;

	protected Configuration config;

	boolean finallySet;

	protected Function() {
	}

	Function(Function func) {
		this.funcName = func.funcName;
		this.name = func.name;
		this.anonymous = func.anonymous;
		this.locals = func.locals;
		this.nargs = func.nargs;
		this.varargs = func.varargs;
		this.pkg = func.pkg;
		this.pkgName = func.pkgName;
		this.lexicalScope = func.lexicalScope;
		this.importEnv = func.importEnv;
		this.moduleList = func.moduleList;
		this.file = func.file;
		this.outer = func.outer;
		this.node = func.node;
		this.config = func.config;
		this.function = func.function;
	}

	protected Function(String func, String[] locals, int nargs, 
			SimpleNode node, Package pkg, Context context) {
	    this(func, locals, nargs, false, node, pkg, context);
	}

	protected Function(String func, String[] locals, int nargs, boolean varargs,
			SimpleNode node, Package pkg, Context context) {
		if (context.frame != null) {
			this.outer = context.frame;
			this.file = outer.file;
			this.importEnv = outer.importEnv;
		} else {
			Cell c = context.loadingResource;
			if (c != null) {
				this.file = c.object;
			}
			this.importEnv = context.importEnv;
		}
		this.config = context.config;
		this.moduleList = context.localModuleList();

		if (func != null) {
			this.name = func;
		} else {
			this.name = "";
			this.anonymous = true;
		}
		this.nargs = nargs;
		this.varargs = varargs;
		this.locals = locals;
		this.node = node;
		this.pkg = pkg;
		this.pkgName = pkg.getName();

		StackFrame lex = context.stackFrame;
		if (lex != null) {
		    this.lexicalScope = lex.makeLexicalScope();
		}
	}

	/**
	 * Returns the name of the function
	 */
	public synchronized String getName() {
		if (this.funcName == null) {
			Function of = outer;
			if (of != null) {
				this.funcName = of.getName() + "." + this.name;
			} else {
				this.funcName = this.name;
			}
		}
		return funcName;
	}

	/**
	 * Returns the number of parameters
	 */
	public int getNumberOfParameter() {
		return nargs;
	}

	public Object getScriptSource() {
		return file;
	}

	protected Object exec(Object[] args, Context context) {
		try {
			context.open(this, args);
			return node.accept(PnutsInterpreter.instance, context);
		} catch (ThreadDeath td) {
			throw td;
		} catch (Throwable t) {
			Runtime.checkException(context, t);
			return null;
		} finally {
			if (finallySet) {
				Value b = context.stackFrame
						.lookup(Context.finallyFunctionSymbol);
				if (b != null) {
					((PnutsFunction) b.get()).call(new Object[] {}, context);
				}
			}
			context.close(this, args);
		}
	}

	public Package getPackage() {
		return pkg;
	}

	public void setPackage(Package pkg) {
		this.pkg = pkg;
	}

	public String[] getImportEnv() {
		return importEnv.list();
	}

	String paramString() {
		StringBuffer sbuf = new StringBuffer("");
		
		sbuf.append("(");
		if (nargs != 0) {
			sbuf.append(locals[0]);
		}
		for (int i = 1; i < nargs; i++) {
		    sbuf.append(",");
		    sbuf.append(locals[i]);
		}
		if (varargs){
		    sbuf.append("[]");
		}
		sbuf.append(")");
		return sbuf.toString();
	}

	protected PnutsFunction register(PnutsFunction pf) {
		return register(pf, false);
	}

	protected PnutsFunction register(PnutsFunction pf, boolean isChild) {
		if (pf == null) {
			if (anonymous) {
				pf = new PnutsFunction();
			} else {
				pf = new PnutsFunction(name);
			}
			pf.pkg = pkg;
		} else if (isChild || pf.count == 0 || pf.pkg != pkg) {
			PnutsFunction func = new PnutsFunction(name, pf);
			func.pkg = pkg;
			if (varargs){
			    func.put(-1, this);
			} else {
			    func.put(nargs, this);
			}
			return func;
		}
		if (varargs){
		    pf.put(-1, this);
		} else {
		    pf.put(nargs, this);
		}
		return pf;
	}

	protected SimpleNode getNode() {
		if (node != null){
			return node.jjtGetParent();
		} else {
			return null;
		}
	}

	protected Object accept(Visitor visitor, Context context) {
		return getNode().accept(visitor, context);
	}

	protected String unparse(Context context) {
		return Runtime.unparse(getNode(), context);
	}

	public String toString() {
		String s = "function ";
		s += getName();
		s += paramString();
		return s;
	}

	void readAttributes(ObjectInputStream s) throws IOException, ClassNotFoundException {
		this.name = s.readUTF().intern();
		this.anonymous = s.readBoolean();
		this.finallySet = s.readBoolean();
		this.nargs = s.readInt();
		this.varargs = s.readBoolean();
		String[] locals = (String[])s.readObject();
		this.locals = locals;
		for (int j = 0; j < locals.length; j++){
			locals[j] = locals[j].intern();
		}
		this.importEnv = (ImportEnv)s.readObject();
		this.moduleList = (ModuleList)s.readObject();
		this.pkgName = (String)s.readUTF();
		this.config = (Configuration)s.readObject();
		this.file = s.readObject();
	}

	void writeAttributes(ObjectOutputStream s) throws IOException {
		s.writeUTF(name);
		s.writeBoolean(anonymous);
		s.writeBoolean(finallySet);
		s.writeInt(nargs);
		s.writeBoolean(varargs);
		s.writeObject(locals);
		s.writeObject(importEnv);
		s.writeObject(moduleList);
		s.writeUTF(pkgName);
		s.writeObject(config);
		s.writeObject(file);
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		for (int i = 0; i < locals.length; i++) {
			locals[i] = locals[i].intern();
		}
		pkg = Package.getPackage(pkgName, null);
		file = s.readObject();
	}

	private void writeObject(ObjectOutputStream s)
			throws IOException {
		s.defaultWriteObject();
		s.writeObject(file);
	}
}
