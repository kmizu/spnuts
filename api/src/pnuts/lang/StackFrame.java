/*
 * @(#)StackFrame.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

class StackFrame {
	StackFrame parent;

	SymbolTable symbolTable;

	StackFrame() {
		this.symbolTable = new SymbolTable();
	}

	StackFrame(String locals[], StackFrame parent) {
		this.parent = parent;
		this.symbolTable = new SymbolTable();
		openLocal(locals);
	}

	public void openLocal(String locals[]) {
		symbolTable = new SymbolTable(symbolTable);

		for (int i = 0; i < locals.length; i++) {
			symbolTable.set(locals[i], null);
		}
	}

	public void closeLocal() {
		symbolTable = symbolTable.parent;
	}

	public NamedValue lookup(String sym) {
		return symbolTable.lookup(sym);
	}

	public void assign(String sym, Object value) {
		symbolTable.assign(sym, value);
	}

	public void declare(String sym, Object value) {
		SymbolTable t = symbolTable;
		while (t.parent != null) {
			t = t.parent;
		}
		t.set(sym, value);
	}

	public final void bind(String sym, Object value) {
		symbolTable.set(sym, value);
	}

	SymbolTable makeLexicalScope() {
	    SymbolTable tab = new SymbolTable();
	    makeLexicalScope(tab);
	    return tab;
	}

	private void makeLexicalScope(SymbolTable out) {
		makeLexicalScope(symbolTable, out);
	}

	private void makeLexicalScope(SymbolTable env, SymbolTable out) {
		if (env.parent != null) {
			makeLexicalScope(env.parent, out);
		}
		for (int i = 0, j = 0; i < env.count; j++) {
			Binding b = env.table[j];
			while (b != null) {
				out.assign(b.name, b);
				i++;
				b = b.chain;
			}
		}
	}

	/*
	 * void dump(){ symbolTable.dump(); if (parent != null){ parent.dump(); } }
	 */
}
