/*
 * @(#)TranslateContext.java 1.4 05/06/13
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.util.HashSet;
import java.util.Set;

import pnuts.lang.Context;

/**
 */
class TranslateContext extends Context {

	Frame rootEnv = new Frame();
	Frame env = rootEnv;

	void openFrame(String func, String locals[]){
		env = new Frame(locals, func, env, false);
	}

	void closeFrame(){
		env = env.parent;
	}

	Reference findReference(String symbol){
		String sym = symbol.intern();
		Frame f = env;
		while (f != null){
			Reference r = f.findReference(sym);
			if (r != null){
				return r;
			}
			f = f.parent;
		}
		return null;
	}

	Reference getReference(String symbol){
		String sym = symbol.intern();
		Frame f = env;
		while (f != null){
			Reference r = f.getReference(sym, true);
			if (r != null){
				return r;
			}
			f = f.parent;
		}
		return null;
	}

	int declare(String symbol){
		env.declare(symbol, 0, 0);
		return 0;
	}

	void openScope(String locals[]) {
		env.openLocal();
		for (int i = 0; i < locals.length; i++) {
			env._declare(locals[i], 0);
		}
	}

	void closeScope(){
		env.closeLocal();
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

	void addToFreeVarSet(String symbol){
		Frame f = env;
		while (f.parent != rootEnv){
			f = f.parent;
		}
		Set set = (Set)f.attr;
		if (set == null){
			set = new HashSet();
			f.attr = set;
		}
		set.add(symbol);
	}

	Set getFreeVarSet(){
		return (Set)env.attr;
	}
}
