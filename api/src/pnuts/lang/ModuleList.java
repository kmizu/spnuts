/*
 * ModuleList.java
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/*
 * This class is responsible for managing packages used in a context. In
 * addition, it provides a cache mechanism for exported symbols.
 */
class ModuleList implements Cloneable, Serializable {

	transient ArrayList usedPackages = new ArrayList();

	transient Package basePackage;

	transient private SymbolTable cache = new SymbolTable();

	private int modCount = 0;

	ModuleList(Package basePackage) {
		this.basePackage = basePackage;
	}

	synchronized void add(Package pkg) {
		usedPackages.remove(pkg);
		usedPackages.add(pkg);
		modCount++;
		cache = new SymbolTable();
	}

	synchronized boolean remove(Package pkg) {
		if (usedPackages.remove(pkg)) {
			modCount++;
			cache = new SymbolTable();
			return true;
		}
		return false;
	}

	boolean contains(Package pkg) {
		return usedPackages.contains(pkg);
	}

	synchronized void clear() {
		usedPackages.clear();
		cache = new SymbolTable();
		modCount++;
	}

	Value resolve(String symbol, Context context) {
		Value v = cache.lookup0(symbol);
		if (v != null) {
			return v;
		}
		loop: while (true) {
			int size = usedPackages.size();
			int m = modCount;
			for (int i = 0; i < size; i++) {
				Package pkg = (Package) usedPackages.get(size - i - 1);
				v = pkg.lookupExportedSymbol(symbol, context);
				if (v != null) {
					synchronized (cache) {
						cache.set(symbol, v.get());
					}
					return v;
				}
				if (modCount != m) {
					continue loop;
				}
			}
			break;
		}
		return null;
	}

	public Object clone() {
		try {
			ModuleList mlist = (ModuleList) super.clone();
			mlist.usedPackages = (ArrayList) usedPackages.clone();
			mlist.cache = new SymbolTable();
			mlist.modCount = 0;
			return mlist;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	String[] getPackageNames() {
		int size = usedPackages.size();
		String[] names = new String[size];
		int i = 0;
		for (Iterator it = usedPackages.iterator(); it.hasNext();) {
			Package p = (Package) it.next();
			names[i++] = p.getName();
		}
		return names;
	}

	public int hashCode(){
		return usedPackages.hashCode() ^ basePackage.hashCode();
	}

	public boolean equals(Object obj){
		if (obj instanceof ModuleList){
			ModuleList m = (ModuleList)obj;
			return usedPackages.equals(m.usedPackages) &&
				basePackage.equals(m.basePackage);
		} else {
			return false;
		}
	}

	private synchronized void writeObject(ObjectOutputStream s)
			throws IOException {
		s.defaultWriteObject();
		s.writeUTF(basePackage.getName());
		int size = usedPackages.size();
		s.writeInt(size);
		for (int i = 0; i < size; i++) {
			Package p = (Package) usedPackages.get(i);
			s.writeUTF(p.getName());
		}
	}

	private static void readPackage(Context ctx, String pkgName, List newPkgs)
		throws IOException
	{
		Package pkg = Package.getPackage(pkgName, ctx);
		newPkgs.add(pkg);

		ModuleList m = ctx.moduleList;
		if (m == null || !m.contains(pkg)){
			pkg.initializeModule();
			if (!pkg.initialized) {
				ctx.loadModule(pkgName, pkg);
				List results = ctx.moduleList.usedPackages;
				for (Iterator it = results.iterator(); it.hasNext();){
					Package p = (Package)it.next();
					if (!newPkgs.contains(p)){
						p.initializeModule();
						if (!p.initialized) {
							ctx.loadModule(p.getName(), p);
							p.initialized = true;
						}
						newPkgs.add(p);
					}
				}
				pkg.initialized = true;
			}
		}
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		Context ctx = Runtime.getThreadContext();
		if (ctx == null){
			ctx = new Context();
		}
		String baseName = s.readUTF();
		basePackage = Package.getPackage(baseName, ctx);
		ArrayList newPkgs = new ArrayList();
		cache = new SymbolTable();
		int size = s.readInt();
		for (int i = 0; i < size; i++) {
			String pkgName = s.readUTF();
			readPackage(ctx, pkgName, newPkgs);
		}
		this.usedPackages = newPkgs;
	}
}
