/*
 * @(#)CallableMethod.java 1.3 05/05/25
 *
 * Copyright (c) 2004,2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.io.Serializable;
import pnuts.lang.Callable;
import pnuts.lang.Context;
import pnuts.lang.Runtime;

/**
 * Convert a set of methods to a callable object without specifying target
 * object.
 */
public class CallableMethod implements Callable, Serializable {

	static final long serialVersionUID = -3161644634782634511L;

	/**
	 * @serial
	 */
	private Class cls;

	/**
	 * @serial
	 */
	private String name;

	private transient Object target;

	protected CallableMethod() {
	}

	/**
	 * Constructor
	 */
	public CallableMethod(Class cls, String name) {
		this.cls = cls;
		this.name = name;
	}

	/**
	 * Constructor
	 */
	public CallableMethod(Object target, String name) {
		this.target = target;
		this.name = name;
		this.cls = target.getClass();
	}

	CallableMethod(Class cls, Object target, String name) {
		this.cls = cls;
		this.name = name;
		this.target = target;
	}

	public void setTargetObject(Object target){
		this.target = target;
	}

	public Object getTargetObject(){
		return target;
	}

	public Object call(Object[] args, Context context) {
		return Runtime.callMethod(context, cls, name, args, null, target);
	}

	public String toString() {
		return "method " + name;
	}
}
