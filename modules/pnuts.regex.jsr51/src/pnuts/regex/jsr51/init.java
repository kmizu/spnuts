/*
 * @(#)init.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.regex.jsr51;

import pnuts.lang.Runtime;
import pnuts.lang.Package;
import pnuts.lang.Context;
import pnuts.lang.AutoloadHook;
import pnuts.lang.PnutsException;
import pnuts.ext.ModuleBase;
import java.io.Serializable;

public class init extends ModuleBase {
	
	static String[] functions = {
		"match",
		"split",
		"substitute",
		"regex",
		"getMatch",
		"getMatches",
		"getMatchStart",
		"getMatchEnd",
		"getMatchCount",
		"getNumberOfGroups",
		"whichRegexAPI",
		"matchAll",
		"formatMatch"
	};

	public Object execute(Context context){
		try {
			Class.forName("java.util.regex.Pattern");
		} catch (ClassNotFoundException e){
			throw new PnutsException(e, context);
		}
		Package pkg = Package.getPackage("pnuts.regex.jsr51", context);
		AutoloadHook ah = new HelperHook(pkg);
		context.clearPackages();
		for (int i = 0; i < functions.length; i++){
			context.autoload(functions[i], ah);
		}
		return null;
	}

	static class HelperHook implements AutoloadHook, Serializable {
		Package pkg;

		HelperHook(Package pkg){
			this.pkg = pkg;
		}

		public void load(String name, Context context){
			PatternMatchHelper helper = new PatternMatchHelper();
			helper.registerAll(pkg, context);
			pkg.export(name);
		}
	}
}
