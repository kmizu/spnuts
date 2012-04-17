/*
 * @(#)init.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.regex;

import pnuts.lang.Pnuts;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import pnuts.ext.ModuleBase;
import java.io.InputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Properties;

/**
 * Initialization of the pnuts.regex module
 */
public class init extends ModuleBase {

	static String regexAPI;

	static void useRegexModule(Context context){
		if (regexAPI != null){
			context.usePackage(regexAPI);
			return;
		}
		String prop = null;
		try {
			prop = System.getProperty("pnuts.regex.module");
			if (context.usePackage(prop, true)){
				regexAPI = prop;
				return;
			}
		} catch (Exception e0){
			// ignore
		}
		if (prop == null){
			try {
				Properties properties = new Properties();
				InputStream in = init.class.getResource("regex.properties").openStream();
				try {
					properties.load(in);
				} finally {
					in.close();
				}
				prop = (String)properties.get("pnuts.regex.modules");
			} catch (IOException e){
				/* ignore */
			}
		}
		if (prop != null){
			Vector v = new Vector();
			StringTokenizer st = new StringTokenizer(prop, ";,");
			while (st.hasMoreTokens()){
				v.addElement(st.nextToken());
			}
			for (Enumeration e = v.elements(); e.hasMoreElements(); ){
				String modname = (String)e.nextElement();
				try {
					if (context.usePackage(modname, true)){
						regexAPI = modname;
						break;
					}
				} catch (Exception ex){
					// ignore
				}
			}
		} else {
			throw new PnutsException("no.regex.modules", context);
		}
	}

	public Object execute(Context context){
		useRegexModule(context);
		return null;
	}
}
