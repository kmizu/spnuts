/*
 * @(#)CompileContext.java 1.5 05/06/14
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import pnuts.lang.Context;
import pnuts.lang.PnutsException;

/**
 * This class is used with pnuts.compiler.Compiler class to compile Pnuts
 * scripts.
 * 
 * @see pnuts.compiler.Compiler
 * @see pnuts.lang.Context
 */
class CompileContext extends Context {

	private final static boolean DEBUG = false;

	Frame env = new Frame();

	Symbol sym = new Symbol();

	Map constants = new HashMap();

	ClassFile cf;

	List classFiles = new ArrayList();

	String constClassName;

	List classes = new ArrayList();

	boolean hasAttachMethod;

	int line = 0xffffffff;

	int column = 0xffffffff;

	Label returnLabel;

	boolean inGeneratorBlock;

	Object scriptSource;

	BeanEnv beanEnv;

	CompileContext(){
	}

	CompileContext(Context context){
		super(context);
	}

	Class loadClasses(CodeLoader loader) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Class ret = load(cf, loader, bout);

		for (int i = 0, n = classFiles.size(); i < n; i++){
			load((ClassFile) classFiles.get(i), loader, bout);
		}
		/*
		for (Enumeration e = classFiles.elements(); e.hasMoreElements();) {
			load((ClassFile) e.nextElement(), loader, bout);
		}
		*/
		resolve(loader);
		return ret;
	}

	void resolve(CodeLoader loader) {
	    for (int i = 0, n = classes.size(); i < n; i++){
		Class c = (Class) classes.get(i);
		loader.resolve(c);		
	    }
	    /*
		for (Enumeration e = classes.elements(); e.hasMoreElements();) {
			Class c = (Class) e.nextElement();
			loader.resolve(c);
		}
	    */
	}

	Class load(ClassFile file, CodeLoader loader) throws IOException {
		return load(file, loader, new ByteArrayOutputStream());
	}

	Class load(ClassFile file, CodeLoader loader, ByteArrayOutputStream bout)
			throws IOException {
		bout.reset();
		file.write(new DataOutputStream(bout));

		byte array[] = bout.toByteArray();
		Class ret = loader.define(file.getClassName(), array, 0, array.length);
		classes.add(ret);
		return ret;
	}

	/**
	 * Get the primary class file
	 */
	public ClassFile getClassFile() {
		return cf;
	}

	/**
	 * Enumerate related class files.
	 */
	public List getClassFiles() {
		return classFiles;
	}

	public void write(DataOutputStream out) throws IOException {
		cf.write(out);
	}

	void debug(ClassFile file) {
		try {
			String fileName = "/tmp/" + file.getClassName() + ".class";
			System.out.println(fileName);
			FileOutputStream fout = new FileOutputStream(fileName);
			DataOutputStream dout = new DataOutputStream(fout);
			file.write(dout);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void debug() {
		debug(cf);
		for (int i = 0, n = classFiles.size(); i < n; i++){
		    ClassFile file = (ClassFile)classFiles.get(i);
		    debug(file);
		}
		/*
		for (Enumeration e = classFiles.elements(); e.hasMoreElements();) {
			ClassFile file = (ClassFile) e.nextElement();
			debug(file);
		}
		*/
	}

	void _openFrame(String func, String locals[], boolean leaf) {
		env = new Frame(locals, func, env, leaf);
	}

	void _closeFrame() {
		env = env.parent;
	}

	void openScope(String locals[]) {
		env.openLocal();
		for (int i = 0; i < locals.length; i++) {
			_declare(locals[i]);
		}
	}

	void closeScope() {
		env.closeLocal();
	}

	void setReference(String symbol) {
		String sym = symbol.intern();
		Frame f = env;
		f.setReference(sym);
	}

	Reference findReference(String symbol) {
		return getReference(symbol, true);
	}

	Reference getReference(String symbol) {
		return getReference(symbol, false);
	}

	Reference getReference(String symbol, boolean flag) {
		String sym = symbol.intern();
		Frame f = env;
		Reference ref = f.getReference(sym, flag);
		if (ref != null) {
			return ref;
		}
		f = f.parent;

		while (f != null) {
			Reference r = f.getReference(sym, flag);
			if (r != null) {
				Frame f0 = env;
				Frame f1 = null;
				while (f0 != f) {
					f1 = f0;
					if (!f0.imports.contains(sym)) {
						if (DEBUG) {
							System.out.println(f0.fname + " imports " + sym);
						}
						f0.imports.add(sym);
					}
					f0 = f0.parent;
				}
				Reference ret = new Reference(sym, -1,
						((r.offset >= 0) ? r.offset : 0));
				Reference _ref = f.getReference(sym, flag);
				List vec = (ArrayList) f.exports.get(f1);
				if (vec == null) {
					vec = new ArrayList();
					f.exports.put(f1, vec);
				}
				if (!vec.contains(_ref)) {
					vec.add(_ref);
					if (DEBUG) {
						System.out.println(f.fname + "["
								+ Integer.toHexString(f.hashCode())
								+ "] exports " + _ref + " to " + f1.fname + "["
								+ Integer.toHexString(f1.hashCode()) + "]");
					}
				}
				f0 = env;
				while (f0 != f1) {
					Frame p = f0.parent;
					vec = (ArrayList) p.exports.get(f0);
					if (vec == null) {
						vec = new ArrayList();
						p.exports.put(f0, vec);
					}
					if (!vec.contains(ret)) {
						vec.add(ret);
						if (DEBUG) {
							System.out.println(p.fname + "["
									+ Integer.toHexString(p.hashCode())
									+ "] exports " + ret + " to " + f0.fname
									+ "[" + Integer.toHexString(f0.hashCode())
									+ "]");
						}
					}
					f0 = p;
				}
				return ret;
			}
			f = f.parent;
		}
		return null;
	}

	int _declare(String symbol) {
		int local = cf.getLocal();
		_declare(symbol, local);
		return local;
	}

	void _declare_frame(String symbol, int local){
		env._declare_frame(symbol, local);
	}
	
	void redefine(String symbol){
		LocalInfo i = env.lookup(symbol);
		if (i != null){
			i.frame = null;
		}
	}
		
	void _declare(String symbol, int local) {
		_declare(symbol, local, env.leaf ? -1 : 0);
	}

	void _declare(String symbol, int local, int idx) {
		if (DEBUG) {
			System.out.println("_declare " + symbol + " " + local + " " + idx);
		}
		env._declare(symbol, local, idx);
	}

	int declare(String symbol) {
		int local = cf.declareLocal();
		declare(symbol, local);
		return local;
	}

	void declare(String symbol, int local) {
		declare(symbol, local, env.leaf ? -1 : 0);
	}

	void declare(String symbol, int local, int idx) {
		if (DEBUG) {
			System.out.println("declare " + symbol + " " + local + " " + idx);
		}
		env.declare(symbol, local, idx);
	}

	int contextIndex = 0;

	int getContextIndex() {
		return contextIndex;
	}

	void setContextIndex(int index) {
		contextIndex = index;
	}

	/*
	 * loops
	 */

	ControlEnv ctrl_env;

	java.util.Stack finallyBlocks = new java.util.Stack(); // for outside loop

	ControlEnv openControlEnv(int id) {
		return ctrl_env = new ControlEnv(id, ctrl_env);
	}

	void closeControlEnv() {
		ctrl_env = ctrl_env.parent;
	}

	Label getContinueLabel() {
		if (ctrl_env == null) {
			return null;
		}
		return ctrl_env.continueLabel;
	}

	Label getBreakLabel() {
		if (ctrl_env == null) {
			return null;
		}
		return ctrl_env.breakLabel;
	}

	void pushFinallyBlock(Label label) {
		if (ctrl_env != null) {
			ctrl_env.pushFinallyBlock(label);
		} else {
			this.finallyBlocks.push(label);
		}
	}

	Label popFinallyBlock() {
		if (ctrl_env != null) {
			return ctrl_env.popFinallyBlock();
		} else {
			return (Label) this.finallyBlocks.pop();
		}
	}

	void leaveControlEnv() {
		if (ctrl_env != null) {
			leaveControlEnv(ctrl_env);
		}
	}

	void leaveControlEnv(ControlEnv env) {
		java.util.Stack finallyBlocks = env.finallyBlocks;
		for (int i = 0; i < finallyBlocks.size(); i++) {
			Label finalTag = (Label) finallyBlocks.get(finallyBlocks.size() - 1
					- i);
			cf.add(Opcode.JSR, finalTag);
		}
	}

	void leaveFrame() {
		ControlEnv env = ctrl_env;
		while (env != null) {
			leaveControlEnv(env);
			env = env.parent;
		}
		java.util.Stack blocks = this.finallyBlocks;
		for (int i = 0; i < blocks.size(); i++) {
			Label finalTag = (Label) blocks.get(blocks.size() - 1 - i);
			cf.add(Opcode.JSR, finalTag);
		}
	}

	void openBranchEnv(){
		env.openBranchEnv();
	}

	void addBranch(){
		env.addBranch();
	}

	void closeBranchEnv(){
		env.closeBranchEnv();
	}

	public Object clone() {
		try {
			CompileContext cc = (CompileContext) super.clone();
			cc.ctrl_env = null;
			cc.finallyBlocks = new java.util.Stack();
			cc.inGeneratorBlock = false;
			cc.env = (Frame) cc.env.clone();
			return cc;
		} catch (Throwable t) {
			throw new PnutsException(t, this);
		}
	}
}
