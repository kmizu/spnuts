/*
 * @(#)Frame.java 1.4 05/06/14
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * This class represents a function scope.
 */
class Frame implements Cloneable {
	Frame parent;
	SymbolSet symbolSet;
	SymbolSet bottom;
	String fname;
	String[] locals;
	List imports = new ArrayList();
	Map exports = new HashMap();
	boolean leaf = true;
	Object attr;
	boolean finallySet;
	BranchEnv branch_env;

	Frame() {
		openLocal();
	}

	Frame(String locals[], String fname, Frame parent, boolean leaf) {
		this.parent = parent;
		if (fname != null) {
			this.fname = fname.intern();
		}
		for (int i = 0; i < locals.length; i++) {
			locals[i] = locals[i].intern();
		}
		this.locals = locals;
		this.bottom = new SymbolSet(null);
		this.symbolSet = bottom;
		this.leaf = leaf;
		openLocal();
	}

	void openLocal() {
		this.symbolSet = new SymbolSet(symbolSet);
	}

	void closeLocal() {
		this.symbolSet = symbolSet.parent;
	}

	LocalInfo lookup(String sym){
		if (branch_env != null){
			LocalInfo info = symbolSet.assoc(sym);
			if (info == null){
				info = branch_env.lookup(sym);
			}
			return info;
		} else {
			return symbolSet.assoc(sym);
		}
	}

	LocalInfo assoc(String sym){
		if (branch_env != null){
			LocalInfo info = symbolSet.assoc(sym);
			if (info == null){
				info = branch_env.assoc(sym);
			}
			return info;
		} else {
			return symbolSet.assoc(sym);
		}
	}

	boolean setReference(String sym) {
		LocalInfo info = assoc(sym);
		if (info != null) {
			info.initialized = true;
			return true;
		} else {
			return false;
		}
	}

	Reference findReference(String sym) {
		return getReference(sym, true);
	}

	Reference getReference(String sym) {
		return getReference(sym, false);
	}

	Reference getReference(String sym, boolean flag) {
		LocalInfo info;
		if (flag){
			info = lookup(sym);
			if (info != null){
				if (branch_env != null){
					branch_env.declare(sym, info.map, info.index);
				} else {
					symbolSet.add(sym, info.map, info.index);
				}
			}
		} else {
			info = assoc(sym);
		}

		if (info != null) {
			return new Reference(sym, info);
		}
		if (locals != null) {
			for (int i = 0; i < locals.length; i++) {
				if (locals[i] == sym) {
					return new Reference(sym, 1, i, true);
				}
			}
		}
		return null; // not found
	}

	public void declare(String symbol, int key, int idx) {
		if (branch_env != null){
			branch_env.declare(symbol, key, idx);
		} else {
			bottom.add(symbol, key, idx);
		}
	}

	public void declare(String symbol, int key) {
		if (branch_env != null){
			branch_env.declare(symbol, key);
		} else {
			bottom.add(symbol, key);
		}
	}

	public void _declare(String symbol, int key, int idx) {
		symbolSet.add(symbol, key, idx);
	}

	public void _declare(String symbol, int key) {
		symbolSet.add(symbol, key);
	}
	
	public void _declare_frame(String symbol, int key){
		symbolSet.add(symbol, key, this);
	}
	
	void openBranchEnv(){
		branch_env = new BranchEnv(branch_env, symbolSet);
	}

	void addBranch(){
		branch_env.addBranch(symbolSet);
	}

	void closeBranchEnv(){
		branch_env.close();
		if (branch_env.parent == null && bottom != null){
			for (Iterator it = branch_env.symbolToLocalInfo.values().iterator(); it.hasNext();){
				LocalInfo info = (LocalInfo)it.next();
				bottom.add(info.symbol, info.map, info.index);
			}
		}
		branch_env = branch_env.parent;
	}


	public Object clone() {
		try {
			Frame frame = (Frame) super.clone();
			/*
			 * frame.locals = new String[locals.length];
			 * System.arraycopy(frame.locals, 0, locals, 0, locals.length);
			 * frame.bottom = (SymbolSet)bottom.clone();
			 * frame.symbolSet =(SymbolSet)symbolSet.clone();
			 * frame.imports = (ArrayList)imports.clone();
			 * frame.exports =(HashMap)exports.clone();
			 */
			return frame;
		} catch (Throwable t) {
			throw new InternalError();
		}
	}
}
