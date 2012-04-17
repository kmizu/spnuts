/*
 * @(#)PackageFactory.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * The system property "pnuts.package.factory" is specified at startup time, the
 * package(..) builtin function calls its createPackage() method of the
 * specified class.
 * 
 * @see pnuts.lang.Package
 * @version 1.1
 */
public interface PackageFactory {
	public Package createPackage(String pkgName, Package parent);
}