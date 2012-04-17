/*
 * @(#)CompositePackage.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import java.util.Hashtable;

import pnuts.lang.Context;
import pnuts.lang.NamedValue;
import pnuts.lang.Package;

/**
 * Composed hierarchical names space based on a Package.
 * 
 * A read access first looks for the symbol in the local symbol table, if not
 * found, then looks for the base package.
 * 
 * A write access never affect the base package.
 */
public class CompositePackage extends Package {

	private Package pkg;

	public CompositePackage() {
		this(Package.getGlobalPackage());
	}

	/**
	 * Constructor
	 * 
	 * @param base
	 *            the base package
	 */
	public CompositePackage(Package base) {
		this.pkg = base;
		Package p = base;
		if (p.getParent() == null) {
			this.root = this;
			this.parent = null;
			this.packages = new Hashtable();
		} else {
			this.parent = new CompositePackage(p.getParent());
			while (p.getParent() != null) {
				p = getParent();
			}
			this.root = new CompositePackage(p);
		}
	}

	/**
	 * Creates a sub-package
	 * 
	 * @param name
	 *            the name of the sub-package
	 */
	public Package newInstance(String name) {
		return new CompositePackage(pkg.newInstance(name));
	}

	/**
	 * Gets the base package
	 */
	public Package getBasePackage() {
		return pkg;
	}

	/**
	 * First looks for the symbol in the local symbol table. If not found looks
	 * for the symbol in the base package.
	 * 
	 * @param symbol
	 *            the symbol to look for
	 * @param context
	 *            the context
	 * @return the pnuts.lang.NamedValue object that holds the value of the
	 *         symbol if it exists. null, otherwise.
	 */
	public NamedValue lookup(String symbol, Context context) {
		NamedValue v = super.lookup(symbol, context);
		if (v != null) {
			return v;
		}
		return pkg.lookup(symbol, context);
	}

	/**
	 * Looks for a name-value binding in the symbol table chain.
	 * 
	 * @param symbol the name of the variable, which must be intern'd
	 * @return the value
	 */
	public NamedValue lookup(String symbol) {
		NamedValue v = super.lookup(symbol);
		if (v != null) {
			return v;
		}
		return pkg.lookup(symbol);
	}
}