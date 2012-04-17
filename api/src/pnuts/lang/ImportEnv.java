/*
 * @(#)ImportEnv.java 1.3 05/05/25
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.pnuts.util.Cache;
import org.pnuts.util.MemoryCache;
import org.pnuts.lang.CallableMethod;

class ImportEnv implements Serializable, Cloneable {

	private final static boolean DEBUG = false;

	static final long serialVersionUID = -755390125835215465L;

	/**
	 * @serial
	 */
	private HashMap/*<String,Import>*/ imports = new HashMap();

	/**
	 * @serial
	 */
	private HashMap/*<String,List<String>>*/ importedClasses = new HashMap();

	/**
	 * @serial
	 */
	private HashMap statics = new HashMap();

	/**
	 * @serial
	 */
	private ArrayList/*<Import>*/ pkgOrder = new ArrayList();

	/**
	 * @serial
	 */
	private ArrayList/*<String>*/ classOrder = new ArrayList();

	/**
	 * @serial
	 */
	private ArrayList/*<String>*/ staticsOrder = new ArrayList();

	transient private Cache cache = createCache();

	transient private Cache failCache = createCache();

	private static Cache createCache(){
	    return new MemoryCache();
	}

	private Import getImport(String name) {
		Import imp = (Import) imports.get(name);
		if (imp != null) {
			return imp;
		} else {
			return new Import(name);
		}
	}

	synchronized void addClass(String className) {
		int idx = className.lastIndexOf('.');
		String name = className.substring(idx + 1);
		List vec = (List) importedClasses.get(name);
		if (vec == null) {
			importedClasses.put(name, vec = new ArrayList());
		}
		classOrder.remove(className);
		classOrder.add(className);

		vec.remove(className);
		vec.add(className);
		cache = createCache();
		failCache = createCache();
	}

	synchronized void addPackage(String pkgname) {
		Import imp = (Import) imports.get(pkgname);
		if (imp != null) {
			pkgOrder.remove(imp);
		} else {
			imp = getImport(pkgname);
		}
		pkgOrder.add(imp);
		imports.put(pkgname, imp);
		cache = createCache();
		failCache = createCache();
	}


	void addStaticMembers(String name, boolean wildcard, Context context) {
		String arg = wildcard ? name + ".*" : name;
		staticsOrder.remove(arg);
		staticsOrder.add(arg);

		try {
			if (wildcard) {
				Class cls = Pnuts.loadClass(name, context);
				Field[] fields = cls.getFields();
				for (int i = 0; i < fields.length; i++) {
					Field f = fields[i];
					if (Modifier.isStatic(f.getModifiers())) {
						statics.put(f.getName(), f.get(null));
					}
				}
				HashSet names = new HashSet();
				Method[] methods = cls.getMethods();
				for (int i = 0; i < methods.length; i++) {
					Method m = methods[i];
					if (Modifier.isStatic(m.getModifiers())) {
						names.add(m.getName());
					}
				}
				for (Iterator it = names.iterator(); it.hasNext();) {
					String memberName = (String) it.next();
					statics.put(memberName, new CallableMethod(cls, memberName));
				}
			} else {
				int idx = name.lastIndexOf('.');
				if (idx > 0) {
					String className = name.substring(0, idx);
					final String memberName = name.substring(idx + 1);
					final Class cls = Pnuts.loadClass(className, context);
					try {
						Field f = cls.getField(memberName);
						if (Modifier.isStatic(f.getModifiers())) {
							statics.put(memberName, f.get(null));
							return;
						}
					} catch (NoSuchFieldException e) {
					}
					statics.put(memberName, new CallableMethod(cls, memberName));
				}
			}
			cache = createCache();
			failCache = createCache();

		} catch (ClassNotFoundException e1) {
			throw new PnutsException(e1, context);
		} catch (IllegalAccessException e2) {
			throw new PnutsException(e2, context);
		}
	}


	synchronized Object get(String sym, Context context) {
		Object v = cache.get(sym);
		if (v != null) {
			return v;
		}
		List vec = (List) importedClasses.get(sym);
		if (vec != null) {
			int size = vec.size();
			for (int i = size - 1; i >= 0; i--) {
				try {
					return Pnuts.loadClass((String) vec.get(i), context);
				} catch (ClassNotFoundException cnf) {
				}
			}
		}
		int symlen = sym.length();
		StringBuffer buf = new StringBuffer(sym);
		boolean lowercase = !Character.isUpperCase(sym.charAt(0));

		int size = pkgOrder.size();
		for (int i = size - 1; i >= 0; i--) {
			Import imp = (Import) pkgOrder.get(i);
			buf.setLength(symlen);
			String name = imp.getName();
			if (lowercase && name.startsWith("java.")) {
				continue;
			}
			buf.append(name);
			String key = buf.toString();
			if (failCache.get(key) != null) {
				continue;
			}
			Class value = imp.get(sym, context);
			if (value != null) {
				cache.put(sym, value);
				return value;
			} else {
				failCache.put(key, key);
			}
		}
		Object value = statics.get(sym);
		if (value != null){
			cache.put(sym, value);
			return value;
		}
		return null;
	}

	String[] list() {
		String ret[] = new String[pkgOrder.size() + classOrder.size() + staticsOrder.size()];
		int i = ret.length - 1;
		for (Iterator it = staticsOrder.iterator(); it.hasNext();) {
			ret[i--] = "static " + (String) it.next();
		}
		for (Iterator it = pkgOrder.iterator(); it.hasNext();) {
			String name = ((Import) it.next()).getName();
			if ("".equals(name)) {
				ret[i--] = "*";
			} else {
				ret[i--] = name + ".*";
			}
		}
		for (Iterator it = classOrder.iterator(); it.hasNext();) {
			ret[i--] = (String) it.next();
		}
		return ret;
	}

	void reset() {
		cache = createCache();
		failCache = createCache();
		for (Iterator it = pkgOrder.iterator(); it.hasNext();) {
			((Import) it.next()).reset();
		}
	}

	public Object clone() {
		try {
			ImportEnv c = (ImportEnv) super.clone();
			c.imports = (HashMap) imports.clone();
			c.importedClasses = (HashMap) importedClasses.clone();
			c.pkgOrder = (ArrayList)pkgOrder.clone();
			c.classOrder = (ArrayList)classOrder.clone();
			c.statics = (HashMap)statics.clone();
			c.staticsOrder = (ArrayList)staticsOrder.clone();
			c.reset();
			return c;
		} catch (Throwable t) {
			throw new InternalError();
		}
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		cache = createCache();
		failCache = createCache();
	}
}
