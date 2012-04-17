/*
 * @(#)SymbolSet.java 1.4 05/06/13
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

/**
 * This class represents a variable scope made by functions,
 * for/foreach statements, and so on.
 *
 * @see pnuts.compiler.Frame
 * @see pnuts.compiler.LocalInfo
 */
final class SymbolSet implements Cloneable {
	SymbolSet parent;
	String keys[] = new String[4];
	LocalInfo info[] = new LocalInfo[4];
	int count = 0;

	SymbolSet(){
	}

	SymbolSet(SymbolSet parent){
		this.parent = parent;
	}

	void add(String sym, int map){
		add(sym, map, 0);
	}

	void add(String sym, int map, Frame frame){
		add(sym,new LocalInfo(sym, map, frame));
	}
	
	void add(String sym, int map, int idx){
		ensureCapacity(count + 1);
		sym = sym.intern();
		keys[count] = sym;
		info[count] = new LocalInfo(sym, map, idx, false);
		count++;
	}

	void add(String sym, LocalInfo i){
		ensureCapacity(count + 1);
		keys[count] = sym;
		info[count] = i;
		count++;
	}

	void ensureCapacity(int size){
		if (size > keys.length){
			String newKeys[] = new String[keys.length + size];
			System.arraycopy(keys, 0, newKeys, 0, keys.length);
			this.keys = newKeys;
			LocalInfo newInfo[] = new LocalInfo[info.length + size];
			System.arraycopy(info, 0, newInfo, 0, info.length);
			this.info = newInfo;
		}
	}

	LocalInfo assoc(String key){
		return _assoc(key.intern());
	}

	LocalInfo _assoc(String key){
		for (int i = 0; i < count; i++){
			if (keys[i] == key){
				return info[i];
			}
		}
		if (parent != null){
			return parent._assoc(key);
		} else {
			return null;
		}
	}

	public Object clone() {
		try {
			SymbolSet ss = (SymbolSet)super.clone();
			ss.keys = new String[keys.length];
			System.arraycopy(ss.keys, 0, keys, 0, keys.length);
			ss.info = new LocalInfo[info.length];
			System.arraycopy(ss.info, 0, info, 0, info.length);
			return ss;
		} catch (Throwable t){
			throw new InternalError();
		}
	}
}
