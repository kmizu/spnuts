/*
 * @(#)ModuleBase.java 1.4 05/01/19
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.pnuts.util.MemoryCache;

import pnuts.lang.AutoloadHook;
import pnuts.lang.Context;
import pnuts.lang.Executable;
import pnuts.lang.Package;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Property;
import pnuts.lang.Runtime;

/**
 * Base class of modules. This class provides convenient autloading functions
 * and an error reporting function. Modules may (or may not) subclass this
 * class.
 */
public abstract class ModuleBase implements Executable, Serializable {

	transient private MemoryCache cache = new MemoryCache();

	AutoloadFunction getAutoloadFunctionHook(Package pkg) {
		AutoloadFunction af = (AutoloadFunction) cache.get(pkg);
		if (af == null) {
			af = new AutoloadFunction(pkg);
			cache.put(pkg, af);
		}
		return af;
	}

	/**
	 * Registers an autoloaded script for functionNames. Also, functionNames are
	 * automatically exported.
	 * 
	 * @param functionNames
	 *            the function names
	 * @param file
	 *            a script file to be loaded when one of the functionNames is
	 *            first resolved.
	 * @param context
	 *            the context
	 */
	protected void autoload(String[] functionNames, String file, Context context) {
		Package pkg = context.getCurrentPackage();
		for (int i = 0; i < functionNames.length; i++) {
			String symbol = functionNames[i].intern();
			pkg.autoload(symbol, file, context);
			pkg.export(symbol);
		}
	}

	/**
	 * Registers an autoloaded class for functionName. Also, the functionName is
	 * automatically exported.
	 * 
	 * @param functionName
	 *            the function name
	 * @param context
	 *            the context
	 */
	protected void autoloadFunction(String functionName, Context context) {
		Package pkg = context.getCurrentPackage();
		String symbol = functionName.intern();
		pkg.autoload(symbol, getAutoloadFunctionHook(pkg));
		pkg.export(symbol);
	}

	/**
	 * Registers an autoloaded Class object.
	 * 
	 * @param javaPackage
	 *            Java package name, e.g. "java.util"
	 * @param name
	 *            short Class name, e.g. "HashMap"
	 * @param context
	 *            the context
	 */
	protected void autoloadClass(String javaPackage, String name,
			Context context) {
		Package pkg = context.getCurrentPackage();
		String symbol = name.intern();
		pkg.autoload(symbol, new AutoloadClass(pkg, javaPackage));
		pkg.export(symbol);
	}

	/**
	 * Defines the prefix of script class (resource) name.
	 * 
	 * This method is overriden by subclasses.
	 */
	protected String getPrefix() {
		return null;
	}

	/*
	 * Returns the Package object associated with this module.
	 * 
	 * @param context the context @return the package
	 */
	protected Package getPackage(Context context) {
		return context.getCurrentPackage();
	}

	/**
	 * This method is redefined in subclasses so that package private classes
	 * can be used.
	 * 
	 * @param cls
	 *            the class to be instantiated
	 * @return an instance of the class
	 */
	protected Object newInstance(Class cls) throws IllegalAccessException,
			InstantiationException {
		return cls.newInstance();
	}

	/**
	 * Makes a class name for the specified package and the symbol's name.
	 * 
	 * @param pkg
	 *            the package
	 * @param name
	 *            the symbol
	 * @return the name of the class
	 */
	protected String getClassName(Package pkg, String name) {
		String pkgName = pkg.getName();
		StringBuffer sbuf = new StringBuffer();
		String prefix = getPrefix();
		if (prefix != null) {
			sbuf.append(prefix);
			sbuf.append('.');
		}
		sbuf.append(pkgName);
		sbuf.append('.');
		sbuf.append(name);
		return sbuf.toString().replace('-', '_');
	}

	class AutoloadFunction implements AutoloadHook, Serializable {
		Package pkg;

		AutoloadFunction(Package pkg) {
			this.pkg = pkg;
		}

		public void load(String name, Context context) {
			try {
				String className = getClassName(pkg, name);
				Class cls = Pnuts.loadClass(className, context);
				PnutsFunction func = (PnutsFunction) newInstance(cls);
				pkg.set(name, func, context);
			} catch (Exception e) {
				Runtime.printError(e, context);
			}
		}
	}

	static class AutoloadClass implements AutoloadHook, Serializable {
		Package pnutsPackage;
		String javaPackage;

		AutoloadClass(Package pnutsPackage, String javaPackage) {
			this.pnutsPackage = pnutsPackage;
			this.javaPackage = javaPackage;
		}

		public void load(String name, Context context) {
			try {
				String className = javaPackage + "." + name;
				Class cls = Pnuts.loadClass(className, context);
				pnutsPackage.set(name, cls, context);
			} catch (Exception e) {
				if (context.isVerbose()) {
					Runtime.printError(e, context);
				}
			}
		}
	}

	private final static String SYMBOL_ERROR = "ERROR".intern();

	private final static String SYMBOL_EXPORTS = "EXPORTS".intern();

	/**
	 * Defines ERROR and EXPORTS, and then call execute(Context).
	 * 
	 * @param context
	 *            the context
	 */
	public Object run(Context context) {
		Package pkg = getPackage(context);
		pkg.set(SYMBOL_ERROR, errorFunction(), context);
		pkg.set(SYMBOL_EXPORTS, exports(pkg), context);
		String[] subModules = getSubModules();
		boolean refreshed = false;
		if (subModules != null){
		    for (int i = 0; i < subModules.length; i++){
			String module = subModules[i];
			context.usePackage(module);
		    }
		    context.clearPackages();
		    refreshed = true;
		}
		String[] requiredModules = getRequiredModules();
		if (requiredModules != null){
		    if (!refreshed){
			context.clearPackages();
		    }
		    for (int i = 0; i < requiredModules.length; i++){
			String module = requiredModules[i];
			context.usePackage(module);
		    }
		}
		return execute(context);
	}

	/**
	 * Subclasses should override this method, instead of run(Context), to
	 * define the initialization process.
	 *
	 * If neither getSubModules() nor getRequiredModules() are redefined
	 * to return non-null value, execute() method should be implemented
	 * as the following steps.
	 *  1. Call context.usePackage() to use the modules that this module provides
	 *  2. Call context.clearPackages()
	 *  3. Call context.usePackage() to use the module that this module requires
	 *  4. Define symbols (functions)
	 *
	 * @param context
	 *            the context
	 */
	protected Object execute(Context context) {
		return null;
	}

	/**
	 * This method is supposed to be redefined in a subclass to
	 * define a set of modules that are required to implement this module.
	 *
	 * If this method returns an array of module names (non-null),
	 * Context.clearPackages() is called,  then the modules are used()'d,
	 * before calling execute() method.
	 */
	protected String[] getRequiredModules() {
		return null;
	}

	/**
	 * This method is supposed to be redefined in a subclass to
	 * define a set of modules that this module provides in the caller's
	 * context.
	 *
	 * If this method returns an array of module names (non-null),
	 * they are use()'d, then Context.clearPackages() is called, before
	 * calling execute() method.
	 */
	protected String[] getSubModules() {
	    return null;
	}


	static class ExportProperty implements Property, Serializable {
		Package pkg;

		ExportProperty(Package pkg) {
			this.pkg = pkg;
		}

		public Object get(String name, Context context) {
			pkg.export(name);
			return null;
		}

		public void set(String name, Object value, Context context) {
			pkg.set(name, value, context);
			pkg.export(name);
		}
	}

	static Property exports(final Package pkg) {
		return new ExportProperty(pkg);
	}

	/*
	 * function ERROR(errorID, arg1, ... )
	 */
	static PnutsFunction errorFunction() {
		return new PnutsFunction() {
			public boolean defined(int nargs) {
				return nargs > 0;
			}

			protected Object exec(Object[] args, Context context) {
				int nargs = args.length;
				String key = (String) args[0];
				String[] param = null;
				if (nargs > 1) {
					param = new String[nargs - 1];
					for (int i = 0; i < param.length; i++) {
						param[i] = (String) args[i + 1];
					}
				}
				Package pkg = context.getCurrentPackage();
				String rsrc = pkg.getName().replace('.', '/') + "/errors";
				ResourceBundle bundle = ResourceBundle.getBundle(rsrc);
				String msg = bundle.getString(key);
				if (param != null && param.length > 0) {
					throw new PnutsException(MessageFormat.format(msg, param),
							context);
				} else {
					throw new PnutsException(msg, context);
				}
			}
		};
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		cache = new MemoryCache();
	}
}
