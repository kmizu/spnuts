/*
 * @(#)SecurePackageFactory.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.security;

import pnuts.lang.Package;
import pnuts.lang.PackageFactory;

/**
 * A Package Factory that creates SecurePackage
 */
public class SecurePackageFactory implements PackageFactory {
	public Package createPackage(String pkgName, Package parent){
		return new SecurePackage(pkgName, parent);
	}
}
