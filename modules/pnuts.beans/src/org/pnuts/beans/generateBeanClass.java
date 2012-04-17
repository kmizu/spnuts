/*
 * @(#)generateBeanClass.java 1.3 05/03/29
 *
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import pnuts.lang.*;
import org.pnuts.lib.BeanClassGenerator;
import org.pnuts.lib.PathHelper;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/*
 * function generateBeanClass(typeMap, className {, superClass {, interfaces {, OutputStream|File|String }}})
 */
public class generateBeanClass extends PnutsFunction {

	public generateBeanClass(){
		super("generateBeanClass");
	}

	public boolean defined(int nargs){
		return nargs > 1 && nargs < 6;
	}

	protected Object exec(Object[] args, Context context){
		Map typeMap;
		String className;
		String superClassName = null;
		String interfaces[] = null;
		try {
			int nargs = args.length;
			if (nargs > 2){
				Object a2 = args[2];
				if (a2 instanceof String){
					superClassName = (String)a2;
				} else if (a2 instanceof Class){
					superClassName = ((Class)a2).getName();
				} else if (a2 != null){
					throw new IllegalArgumentException(String.valueOf(a2));
				}
			}
			if (nargs > 3){
				Object[] a3 = (Object[])args[3];
				if (a3 instanceof String[]){
					interfaces = (String[])a3;
				} else if (a3 != null) {
					interfaces = new String[a3.length];
					for (int i = 0; i < a3.length; i++){
						Object a3i = a3[i];
						if (a3i instanceof String){
							interfaces[i] = (String)a3i;
						} else if (a3i instanceof Class){
							interfaces[i] = ((Class)a3i).getName();
						} else {
							throw new IllegalArgumentException(String.valueOf(a3i));
						}
					}
				}
			}
			if (nargs > 1){
				className = (String)args[1];
				typeMap = (Map)args[0];
			} else {
				undefined(args, context);
				return null;
			}
			if (nargs == 5){
				Object arg4 = args[4];
				boolean needToClose = false;
				OutputStream output = null;
				if (arg4 instanceof OutputStream){
					output = (OutputStream)arg4;
				} else if (arg4 instanceof File){
					output = new FileOutputStream((File)arg4);
					needToClose = true;
				} else if (arg4 instanceof String){
					File file = PathHelper.getFile((String)arg4, context);
					output = new FileOutputStream(file);
					needToClose = true;
				} else {
					throw new IllegalArgumentException();
				}
				if (needToClose){
					try {
						BeanClassGenerator.generate(typeMap,
									    className,
									    superClassName,
									    interfaces,
									    output);
					} finally {
						try {
							output.close();
						} catch (IOException e){
							// skip
						}
					}
				} else {
					BeanClassGenerator.generate(typeMap,
								    className,
								    superClassName,
								    interfaces,
								    output);
				}
				return null;
			} else {
				ClassLoader classLoader = context.getClassLoader();
				if (classLoader == null){
					classLoader = Thread.currentThread().getContextClassLoader();
				}
				return BeanClassGenerator.generate(typeMap,
								   className,
								   superClassName,
								   interfaces,
								   classLoader);
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function generateBeanClass(typeMap, className {, superClass {, interfaces {, OutputStream|File|String }}})";
	}
}
