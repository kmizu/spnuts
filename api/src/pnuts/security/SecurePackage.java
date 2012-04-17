/*
 * @(#)SecurePackage.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.security;

import pnuts.lang.Context;
import pnuts.lang.Package;

/**
 * Package that can control add/write/read operation in a security context.
 *
 * <pre>
 * pnuts -J-Djava.security.manager "-J-Dpnuts.package.factory=pnuts.security.SecurePackage\$Factory" <em>scripts</em>
 * </pre>
 */
public class SecurePackage extends Package {

	public SecurePackage(String name, Package parent){
		super(name, parent);
	}

	protected void addPackage(Package pkg, Context context){
		checkPackageAccess(pkg, "add");
		super.addPackage(pkg, context);
	}

	public void set(String symbol, Object obj, Context context){
		checkPackageAccess(this, "write");
		super.set(symbol, obj, context);
	}

	public void clear(String symbol, Context context){
		checkPackageAccess(this, "write");
		super.clear(symbol, context);
	}

	protected void removePackage(Package pkg, Context context){
		checkPackageAccess(pkg, "remove");
		super.removePackage(pkg, context);
	}

	void checkPackageAccess(Package pkg, String action){
		SecurityManager security = System.getSecurityManager();
		if (security == null) {
			return;
		}
		String name = pkg.getName();
		if (name == null){
			return;
		}
		if ("".equals(name)){
			name = "::";
		}
		security.checkPermission(new PackagePermission(name, action));
	}
}
