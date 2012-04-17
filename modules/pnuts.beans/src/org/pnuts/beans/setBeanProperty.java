/*
 * @(#)setBeanProperty.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import pnuts.lang.*;
import pnuts.lang.Runtime;

/*
 * setBeanProperty(bean, property_name, value)
 */
public class setBeanProperty extends PnutsFunction {

	public setBeanProperty(){
		super("setBeanProperty");
	}

	public boolean defined(int narg){
		return (narg == 3);
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 3){
			undefined(args, context);
			return null;
		}
		Object bean = args[0];
		String property = (String)args[1];
		Object value = args[2];
		Runtime.setBeanProperty(context, bean, property, value);
		return null;
	}

	public String toString(){
		return "function setBeanProperty(bean, property_name, value)";
	}
}
