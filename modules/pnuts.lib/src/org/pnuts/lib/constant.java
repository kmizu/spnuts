/*
 * @(#)constant.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Package;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/*
 * constant()
 * constant(pkg)
 */
public class constant extends PnutsFunction implements Property {

	public constant(){
		super("constant");
	}

	public boolean defined(int nargs){
		return nargs == 0 || nargs == 1;
	}

	protected Object exec(Object[] args, final Context context){
		final Package pkg;
		int nargs = args.length;
		if (nargs == 0){
			pkg = context.getCurrentPackage();
		} else if (nargs == 1){
			pkg = (Package)args[0];
		} else {
			undefined(args, context);
			return null;
		}
		return new Property(){
				public Object get(String name, Context ctx){
					return pkg.get(name, context);
				}
		
				public void set(String name, Object value, Context ctx){
					try {
						pkg.setConstant(name, value);
					} catch (IllegalStateException e){
						rethrow(name, context);
					}
				}

				public String toString(){
					return "<constant handler>";
				}
			};
	}

	static void rethrow(String symbol, Context context){
		ResourceBundle bundle = ResourceBundle.getBundle("pnuts.lang.pnuts");
		String fmt = bundle.getString("constant.modification");
		String msg = MessageFormat.format(fmt, new Object[]{symbol});
		throw new PnutsException(msg, context);
	}
	

	public Object get(String name, Context context){
		return context.getCurrentPackage().get(name, context);
	}

	public void set(String name, Object value, Context context){
		try {
			context.getCurrentPackage().setConstant(name, value);
		} catch (IllegalStateException e){
			rethrow(name, context);
		}
	}

	public String toString(){
		return "<constant handler>";
	}
}
