/*
 * Package.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.Vector;
import org.pnuts.lang.MapPackage;
import org.pnuts.lang.PackageMap;

/**
 * This class represents a Pnuts' package (not Java's).
 */
public class Package extends SymbolTable implements Property, Serializable,
		Cloneable {
	private final static boolean DEBUG = false;

	static final long serialVersionUID = 2542832545950985547L;

	/**
	 * All packages with a non-null name
	 */
	protected transient Hashtable packages;

	/**
	 * The package with name "".
	 */
	public final static Package globalPackage;

	transient protected Package parent;

	transient private Vector children;

	protected transient Package root;

	protected SymbolTable autoloadTable;

	protected SymbolTable exportedSymbols; // non-null if the package is used as
										   // a module.

	protected Vector requiredModuleNames;

	protected Vector providedModuleNames;

	protected boolean exports; // true if export() has been explicitly called

	protected boolean usedAsModule = false;

	protected boolean initialized = false;

	transient Object moduleIntializationLock = new Object();

	private HashSet autoloadingSymbols = new HashSet();

	/**
	 * The name of the package.
	 * 
	 * @serial
	 */
	protected String name;

	private static PackageFactory factory;

	static {
		try {
			String prop = Runtime.getProperty("pnuts.package.factory");
			if (prop != null) {
				Object obj = Class.forName(prop).newInstance();
				if (obj instanceof PackageFactory) {
					factory = (PackageFactory) obj;
				}
			}
		} catch (Throwable e) {/* skip */
		}

		globalPackage = getInstance("", null, null);
	}

	/**
	 * @return the global package
	 */
	public static Package getGlobalPackage() {
		return globalPackage;
	}

	/**
	 * Returns a Package object that wrap the specified Map
	 */
	public static Package wrap(Map map){
		return new MapPackage(map);
	}

	/**
	 * Returns a Map object that wraps this package
	 */
	public Map asMap(){
		return new PackageMap(this);
	}

	/**
	 * Creates an uninitialized instance of a Package subclass When a
	 * sub-package is created, this method is called.
	 * 
	 * @param name
	 *            the package name
	 * @return a Package object
	 */
	public Package newInstance(String name) {
		return new Package(name);
	}

	/*
	 * Creates a package specifying the parent package. If parent is not null,
	 * an instance of the same class as parent is created.
	 */
	static Package getInstance(String name, Package parent, Context context) {
		Package p = null;
		if (parent != null) {
			if (name != null) {
				Hashtable tab = context.rootPackage.packages;
				p = (Package) tab.get(name);
				if (p != null) {
					return p;
				}
			}
			p = parent.newInstance(name);

			p.parent = parent;
			p.root = parent.root;

			if (p.name != null) {
				context.rootPackage.addPackage(p, context);
			}
			return p;
		} else {
			if (factory != null) {
				return factory.createPackage(name, null);
			} else {
				return new Package(name, null);
			}
		}
	}

	/*
	 * Registers a sub-package to a root package. This method is called only by
	 * a root package.
	 * 
	 * @param pkg the sub-package @param context the context in which this
	 * operation is executed
	 */
	protected void addPackage(Package pkg, Context context) {
		packages.put(pkg.name, pkg);
		Package parent = pkg.parent;
		if (parent != null) {
			Vector children = parent.children;
			if (children == null) {
				parent.children = children = new Vector(10);
				children.addElement(pkg);
			} else if (!children.contains(pkg)) {
				children.addElement(pkg);
			}
		}
	}

	/*
	 * Unregisters a sub-package from a root package. This method is called only
	 * by a root package.
	 * 
	 * @param pkg the sub-package @param context the context in which this
	 * operation is executed
	 */
	protected void removePackage(Package pkg, Context context) {
		Package p2 = pkg.parent;
		if (p2 == null) {
			return;
		}
		if (p2.children != null) {
			p2.children.removeElement(pkg);
		}
		packages.remove(pkg.name);
	}

	/**
	 * If package "pkg" exists returns the package, otherwise creates and
	 * returns it.
	 * 
	 */
	public static Package getPackage(String pkg) {
		return getPackage(pkg, null);
	}

	/**
	 * If package "pkg" exists returns the package, otherwise creates and
	 * returns it.
	 */
	public static Package getPackage(String pkg, Context context) {
		Package rootPackage;
		if (context == null) {
			context = Runtime.getThreadContext();
		}
		if (context == null){
			rootPackage = globalPackage;
		} else {
			rootPackage = context.rootPackage;
		}
		Hashtable packages = rootPackage.packages;
		Package p = rootPackage;
		Package existing = (Package) packages.get(pkg);
		if (existing != null) {
			return existing;
		}
		int index = pkg.indexOf("::");
		String rest = pkg;
		StringBuffer sbuf = new StringBuffer();
		while (index > 0) {
			sbuf.append(rest.substring(0, index));
			String s = sbuf.toString();
			if (rootPackage.get(s, null) instanceof Class) {
				throw new PnutsException("package name and Class conflicted: " + pkg, context);
			}
			p = getInstance(s, p, context);
			rest = rest.substring(index + 2);
			index = rest.indexOf("::");
			sbuf.append("::");
		}
		sbuf.append(rest);
		return getInstance(sbuf.toString(), p, context);
	}

	/*
	 * Returns the module initialization script name.
	 */
	String getInitScript() {
		String s = getInitScript(name, "::");
		return getInitScript(s, ".") + "/init";
	}

	static String getInitScript(String name, String delimiter) {
		int index = name.indexOf(delimiter);
		int len = delimiter.length();
		int start = 0;
		StringBuffer sbuf = new StringBuffer();
		while (index > 0) {
			sbuf.append(name.substring(start, index));
			sbuf.append('/');
			start = index + len;
			index = name.indexOf(delimiter, start);
		}
		sbuf.append(name.substring(start));
		return sbuf.toString();
	}

	/**
	 * Checks if the specified name is already defined in this package.
	 * 
	 * @return true if name is defined in the package.
	 */
	public boolean defined(String name, Context context) {
		return lookup0(name) != null;
	}

	/**
	 * Get the value of a symbol in the package. When the symbol is not defined
	 * in the package, first, the associated autoloading hook is invoked if any,
	 * second, get the value in the parent package.
	 * 
	 * @param symbol
	 *            an interned name in the package
	 * @param context
	 *            the context in which the symbol is referenced. null means "not
	 *            specified".
	 * @return the value of specified variable in the package.
	 */
	public Object get(String symbol, Context context) {
		Value b = lookupRecursively(symbol, context);
		if (b == null) {
			return null;
		} else {
			return b.get();
		}
	}

	/**
	 * Set a value of a symbol in the package. If this package is "used" in the
	 * specified context, and if the target object is either a Class object or a
	 * function whose name matches the symbol, then the symbol is imported to
	 * the context.
	 * 
	 * @param symbol
	 *            an interned name of variable
	 * @param obj
	 *            the value of the variable
	 */
	public void set(String symbol, Object obj, Context context) {
		try {
			set(symbol, obj);
		} catch (IllegalStateException e) {
			throw new PnutsException("constant.modification",
					new Object[] { symbol }, context);
		}
		if (DEBUG) {
			System.out.println(symbol + "<=" + obj + "; " + getClass() + "@"
					+ hashCode());
		}
		if (usedAsModule) {
			Binding b = exportedSymbols.lookup0(symbol);
			if (b != null) {
				b.value = obj;
			}
		}
	}

	/**
	 * Exports a symbol of the module
	 * 
	 * @param name
	 *            the symbol
	 * @exception IllegalStateException
	 *                when the package is not used as a module.
	 */
	public void export(String name) {
		if (!usedAsModule) {
			throw new IllegalStateException("exporting " + name + " in "
					+ getName());
		}
		name = name.intern();
		Binding b = lookup0(name);

		if (b != null) {
			if (DEBUG) {
				System.out.println("export " + name + "<=" + b.value + "; "
						+ exportedSymbols);
			}
			exportedSymbols.set(name, b.value);
		} else {
			if (autoloadTable != null) {
				b = autoloadTable.lookup0(name);
				if (b != null) {
					b.value = new DelayedExports((AutoloadHook) b.value, false);
				}
			}
		}
		exports = true;
	}

	class DelayedExports implements AutoloadHook, Serializable {
		private AutoloadHook hook;

		private boolean func;

		DelayedExports(AutoloadHook hook, boolean onlyFunction) {
			this.hook = hook;
			this.func = onlyFunction;
		}

		public void load(String name, Context context) {
			if (DEBUG) {
				System.out.println("invoking autoload hook: " + hook);
			}
			Context c = (Context) context.clone();
			c.setCurrentPackage(Package.this);
			hook.load(name, c);
			if (func) {
				Binding v = lookup0(name);
				if (v != null) {
					exportFunction(name, v.value);
				}
			} else {
				if (DEBUG) {
					System.out.println("export: " + name + "; "
							+ Package.this.getClass() + "@"
							+ Package.this.hashCode());
				}
				export(name);
			}
		}
	}

	/*
	 * Exports all functions in this package, as well as autoloaded symbols.
	 * This method is called only when a module is initialized but export() has
	 * not been called to specify exported symbols of the module.
	 */
	void exportFunctions() {
		for (Enumeration e = bindings(); e.hasMoreElements();) {
			Binding b = (Binding) e.nextElement();
			exportFunction(b.name, b.value);
		}
		if (autoloadTable != null) {
			for (Enumeration e = autoloadTable.bindings(); e.hasMoreElements();) {
				Binding b = (Binding) e.nextElement();
				final AutoloadHook hook = (AutoloadHook) b.value;
				b.value = new DelayedExports(hook, true);
			}
		}
	}

	void exportFunction(String sym, Object value) {
		if (value instanceof PnutsFunction) {
			PnutsFunction f = (PnutsFunction) value;
			if (f.getName() == sym) {
				exportedSymbols.set(sym, f);
			}
		}
	}

	/**
	 * Deletes a symbol from the package.
	 * 
	 * @param symbol
	 *            a name of variable to be deleted
	 */
	public void clear(String symbol, Context context) {
		removeBinding(symbol);
	}

	/**
	 * Removes the specified package.
	 * 
	 * @deprecated replaced by remove(String, Context)
	 */
	public static void remove(String name) {
		Package root = globalPackage;
		Package p = (Package) root.packages.get(name);
		if (p == null) {
			return;
		}
		root.removePackage(p, null);
	}

	/**
	 * Removes the specified package.
	 */
	public static void remove(String name, Context context) {
		Package root = context.rootPackage;
		Package p = (Package) root.packages.get(name);
		if (p == null) {
			return;
		}
		root.removePackage(p, context);
	}

	/**
	 * Find a named package.
	 * 
	 * @param pkg
	 *            a name of package to look.
	 * @return a package with name "pkg" if it exits.
	 * 
	 * @deprecated replaced by find(String, Context)
	 */
	public static Package find(String pkg) {
		return (Package) globalPackage.packages.get(pkg);
	}

	/**
	 * Find a named package.
	 * 
	 * @param pkg
	 *            a name of package to look.
	 * @return a package with name "pkg" if it exits.
	 */
	public static Package find(String pkg, Context context) {
		return (Package) context.rootPackage.packages.get(pkg);
	}

	/**
	 * Creates a package that is not visible from other packages.
	 */
	public Package() {
		this(null);
	}

	/**
	 * Creates a package and register it in a static hashtable.
	 * 
	 * @param name
	 *            the name of the package
	 */
	public Package(String name) {
		this(name, globalPackage);
	}

	/**
	 * Creates a package and register it in a static hashtable. The method
	 * <tt>get()</tt> tries to find a symbol in this package and then consult
	 * the parent package. Other instance methods, such as
	 * <tt>set(), defined()</tt>, operates on this package only. Other
	 * constructors implicitly specify the global package as the parent package.
	 * 
	 * @param name
	 *            the name of the package
	 * @param parent
	 *            the parent package.
	 */
	public Package(String name, Package parent) {
		if (name != null) {
			this.name = name.intern();
		}
		this.parent = parent;
		this.root = (parent == null) ? this : parent.root;
	}

	/**
	 * Creates a package and register it in a static hashtable. The method
	 * <tt>get()</tt> tries to find a symbol in this package and then consult
	 * the parent package. Other instance methods, such as
	 * <tt>set(), defined()</tt>, operates on this package only. Other
	 * constructors implicitly specify the global package as the parent package.
	 * 
	 * @param name
	 *            the name of the package
	 * @param parent
	 *            the parent package.
	 * @param root
	 *            the root package.
	 */
	protected Package(String name, Package parent, Package root) {
		if (name != null) {
			this.name = name.intern();
		}
		this.parent = parent;
		this.root = root;
	}

	/**
	 * This method is called when the package become the current package with
	 * package() function. This method in a subclass must call
	 * super.init(context) first.
	 */
	protected synchronized void init(Context context) {
		if (this.root == this && this.packages == null) {
			Hashtable tab = new Hashtable(10);
			if (name != null) {
				tab.put(name, this);
			}
			this.packages = tab;
		}
	}

	/**
	 * @return the name of the package.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the parent package.
	 */
	public Package getParent() {
		return parent;
	}

	/**
	 * Looks up a symbol in this package. If not defined, the associated
	 * autoloading hook is invoked if any.
	 *
	 * @param symbol
	 *            an interned String
	 * @param context
	 *            the context
	 * @return a NamedValue
	 */
	public NamedValue lookup(String symbol, Context context) {
		NamedValue v = lookup0(symbol);
		if (v != null) {
			return v;
		}
		synchronized (this){
			SymbolTable tab = autoloadTable;
			if (tab != null) {
				AutoloadHook hook = (AutoloadHook) tab.get(symbol);
				if (hook != null) {
					if (DEBUG) {
						System.out.println("invoking autoload hook: " + hook);
					}
					if (autoloadingSymbols.contains(symbol)) {
						return null;
					}
					autoloadingSymbols.add(symbol);
					try {
						hook.load(symbol, context);
					} finally {
						autoloadingSymbols.remove(symbol);
					}
					v = lookup0(symbol);
					if (v != null) {
						return v;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Lookup the symbol in the package. When the symbol is not defined in the
	 * package and this.parent is not null, lookup the symbol in the parent
	 * package.
	 *
	 * Resulting value is an instance of  NamedValue.
	 * 
	 * @param symbol
	 *            intern'ed string
	 */
	protected NamedValue lookupRecursively(String symbol, Context context) {
		NamedValue v = lookup(symbol, context);
		if (v == null && parent != null) {
			return parent.lookupRecursively(symbol, context);
		} else {
			return v;
		}
	}

	NamedValue lookupExportedSymbol(String symbol, Context context) {
		if (!usedAsModule) {
			return null;
		}
		if (DEBUG) {
			System.out.println("lookup " + symbol + " in " + getName() + ":"
					+ getClass() + "@" + hashCode());
		}

		Binding v = exportedSymbols.lookup0(symbol);
		if (v != null) {
			return v;
		}
		synchronized (this){
			SymbolTable tab = autoloadTable;
			if (tab != null) {
				AutoloadHook hook = (AutoloadHook) tab.get(symbol);
				if (hook != null) {
					if (DEBUG) {
						System.out.println("invoking autoload hook: " + hook + "; "
								   + getClass() + "@" + hashCode());
					}
					if (autoloadingSymbols.contains(symbol)) {
						return null;
					}
					autoloadingSymbols.add(symbol);
					try {
						hook.load(symbol, context);
					} finally {
						autoloadingSymbols.remove(symbol);
					}
					if (usedAsModule) {
						v = exportedSymbols.lookup0(symbol);
						if (DEBUG) {
							System.out.println("(" + exportedSymbols.size() + "):"
									   + v);
						}
						if (v != null) {
							return v;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Starts using this package as a module.
	 */
	protected synchronized void initializeModule() {
		if (DEBUG) {
			System.out.println("initializeModule:" + getName() + "@"
					+ hashCode());
		}
		this.exportedSymbols = new SymbolTable();
		this.requiredModuleNames = new Vector();
		this.providedModuleNames = new Vector();
		usedAsModule = true;
	}

	/**
	 * Enumerates sub-packages
	 * 
	 * @deprecated
	 */
	public Enumeration elements() {
		if (children == null) {
			children = new Vector(10);
		}
		return children.elements();
	}

	/**
	 * Returns a clone package.
	 * 
	 *  
	 */
	public Object clone() {
		if (autoloadTable == null) {
			autoloadTable = new SymbolTable();
		}
		Package p = (Package) super.clone();
		if (root == this) {
			p.root = p;
			if (packages != null) {
				Hashtable tab = (Hashtable) packages.clone();
				for (Enumeration e = tab.keys(); e.hasMoreElements();) {
					String pkgName = (String) e.nextElement();
					Package pkg = (Package) tab.get(pkgName);
					if (pkg.root == pkg) {
						tab.put(pkgName, p);
					} else if (pkg.root == root) {
						pkg.root = p;
					}
				}
				p.packages = tab;
				if (name != null) {
					tab.put(name, p);
				}
			}
			if (children != null) {
				p.children = (Vector) children.clone();
			}
		} else {
			if (usedAsModule) {
				p.exportedSymbols = (SymbolTable) exportedSymbols.clone();
				p.requiredModuleNames = (Vector) requiredModuleNames.clone();
				p.providedModuleNames = (Vector) providedModuleNames.clone();
			}
		}
		return p;
	}

	/**
	 * Registers an autoload script for the <em>name</em>. If <em>name</em>
	 * is not defined when accessed, the registerred <em>file</em> is loaded.
	 * 
	 * @param name
	 *            variable name
	 * @param file
	 *            the file
	 * @param context
	 *            the context
	 */
	public void autoload(String name, String file, Context context) {
		if (file == null) {
			autoload(name, (AutoloadHook) null);
		} else {
			autoload(name, new Runtime.AutoloadScript(file, context));
		}
	}

	/**
	 * Registers an AutoloadHook for the <em>name</em>. If <em>name</em> is
	 * not defined when accessed, the registerred AutoloadHook is executed.
	 * 
	 * @param name
	 *            variable name
	 * @param hook
	 *            the AutoloadHook
	 */
	public void autoload(String name, AutoloadHook hook) {
		if (autoloadTable == null) {
			autoloadTable = new SymbolTable();
		}
		name = name.intern();
		if (hook == null) {
			autoloadTable.removeBinding(name);
		} else {
			autoloadTable.set(name, hook);
		}
	}

	public String toString() {
		if (name != null) {
			return "package \"" + name + "\"";
		} else {
			return "package null";
		}
	}
}
