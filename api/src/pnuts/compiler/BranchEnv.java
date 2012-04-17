/*
 * @(#)BranchEnv.java 1.1 05/06/21
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class BranchEnv {
	BranchEnv parent;
	ArrayList scopes;
	SymbolSet scope;
	Map symbolToLocalInfo;

	BranchEnv(BranchEnv parent, SymbolSet ss){
		this.parent = parent;
		this.scopes = new ArrayList();
		this.symbolToLocalInfo = new HashMap();
		addBranch(ss);
	}

	void addBranch(SymbolSet ss){
		this.scope = new SymbolSet(ss);
		scopes.add(scope);
	}

	void close(){
		if (parent != null){
			parent.symbolToLocalInfo.putAll(symbolToLocalInfo);
			for (Iterator it = symbolToLocalInfo.entrySet().iterator();
			     it.hasNext();){
				Map.Entry entry = (Map.Entry)it.next();
				String sym = (String)entry.getKey();
				LocalInfo info = (LocalInfo)entry.getValue();
				parent.scope.add(sym, info);
			}
		}
	}

	LocalInfo lookup(String key){
		LocalInfo info = (LocalInfo)symbolToLocalInfo.get(key);
		if (info != null){
			return info;
		} else if (parent != null){
			return parent.lookup(key);
		} else {
			return null;
		}
	}

	LocalInfo assoc(String key){
		LocalInfo info = scope.assoc(key);
		if (info != null){
			return info;
		} else if (parent != null){
			return parent.assoc(key);
		} else {
			return null;
		}
	}

	void declare(String symbol, int key, int idx){
//		if (!symbolToLocalInfo.containsKey(symbol)){
			scope.add(symbol, key, idx);
			LocalInfo info = scope.info[scope.count - 1];
			symbolToLocalInfo.put(symbol, info);
//		}
	}

	void declare(String symbol, int key){
//		if (!symbolToLocalInfo.containsKey(symbol)){
			scope.add(symbol, key);
			LocalInfo info = scope.info[scope.count - 1];
			symbolToLocalInfo.put(symbol, info);
//		}
	}
}
