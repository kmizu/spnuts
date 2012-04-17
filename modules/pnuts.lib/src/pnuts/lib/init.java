/*
 * @(#)init.java 1.5 05/06/14
 *
 * Copyright (c) 2001-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.ext.ModuleBase;
import org.pnuts.lib.*;

/**
 * Initialization of the pnuts.lib module.
 */
public class init extends ModuleBase {

	static String[] files  = {
		"pnuts/lib/console",
		"pnuts/lib/package",
		"pnuts/lib/array",
		"pnuts/lib/i18n",
		"pnuts/lib/system",
		"pnuts/lib/collection",
		"org/pnuts/lib/calendar",
		"org/pnuts/lib/modifiers"
	};

	static String[][] functions = {
		// pnuts/lib/console
		{
			"getWriter",
			"getErrorWriter",
			"error",
		},
		// pnuts/lib/package
		{
			"createPackage",
			"$"
		},
		// pnuts/lib/array
		{
			"rsort",
			"collect",
		},
		// pnuts/lib/i18n
		{
			"formatMessage",
			"getResourceBundle",
			"getLocalizedResource"
		},
		// pnuts/lib/system
		{
			"setVerbose",
			"addShutdownHook",
			"removeShutdownHook"
		},
		// pnuts/lib/collection
		{
			"vector",
			"hashTable"
		},
		// pnuts/lib/calendar
		{
			"getYear", "getMonth", "getWeekOfYear", "getDayOfYear", "getDayOfMonth",
			"getDayOfWeek", "getHour", "getMinute", "getSecond",
			"getMillisecond", "getMaxDayOfYear", "getMaxDayOfMonth",
			"addYear", "addMonth", "addWeek", "addDay", "addHour", "addMinute",
			"addSecond", "addMillisecond", "diffYear", "diffMonth",
			"diffWeek", "diffDay", "diffHour", "diffMinute", "diffSecond", "diffMillisecond"
		},
		{
			"isPublic",
			"isProtected",
			"isPrivate",
			"isAbstract",
			"isInterface",
			"isStatic"
		}
	};

	static String[] javaFunctions = {
		"print",
		"println",
		"exit",
		"arraycopy",
		"string",
		"hex",
		"include",
		"includeFile",
		"format",
		"isFunction",
		"isGenerator",
		"iterable",
		"isArray",
		"random",
		"memcache", "LRUcache",
		"applyFunction",
		"call",
		"mapFunction",
		"project",
		"rootScope",
		"filter",
		"reduce",
		"size",
		"count",
		"list",
		"set",
		"map",
		"reverse",
		"push",
		"pop",
		"shift",
		"unshift",
		"contains",
		"mapget",
		"mapput",
		"isEmpty",
		"getFile",
		"basename",
		"dirname",
		"getURL",
		"getURI",
		"write",
		"flush",
		"sort",
		"compile",
		"parse",
		"unparse",
		"run",
		"date",
		"setFormatLocale",
		"getResource",
		"isCompiled",
		"makeProxy",
		"subclass",
		"javaAdapter",
		"beanclass",
		"currentTimeMillis",
		"printAll",
		"getTimeZone",
		"setTimeZone",
		"getMaxDayOfMonth",
		"getMaxDayOfYear",
		"getLocale",
		"setLocale",
		"formatDate",
		"formatTime",
		"formatDateTime",
		"isJava2",
		"classGenerator",
		"setFinalizer",
		"getPackage",
		"findPackage",
		"removePackage",
		"aggregateMode",
		"constant",
		"join",
		"generator",
		"identical",
		"binarySearch",
		"schedule",
		"range",
		"formatNumber",
		"formatCurrency",
		"formatPercent",
		"toLowerCase",
		"toUpperCase",
		"getClassPath",
		"setClassPath",
		"addClassPath"
	};

	protected String getPrefix(){
		return "org";
	}

	public Object execute(Context context){
		context.clearPackages();
		for (int i = 0; i < files.length; i++){
			autoload(functions[i], files[i], context);
		}
	
		for (int i = 0; i < javaFunctions.length; i++){
			autoloadFunction(javaFunctions[i], context);
		}
		Package pkg = getPackage(context);
		String FINALLY = "finally".intern();
		pkg.set(FINALLY, new _finally(), context);
		pkg.export(FINALLY);

		String INTERFACE = "interface".intern();
		pkg.set(INTERFACE, new _interface(), context);
		pkg.export(INTERFACE);

		return null;
	}
}
