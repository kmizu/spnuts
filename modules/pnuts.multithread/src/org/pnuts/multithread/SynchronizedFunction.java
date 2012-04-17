/*
 * @(#)SynchronizedFunction.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import pnuts.lang.*;
import pnuts.lang.Package;

/**
 * This class is used in Pnuts script to execute a function within a synchronized block.
 */
public class SynchronizedFunction extends PnutsFunction {
	private PnutsFunction function;
	private String name;
	private Object lock;

	public SynchronizedFunction(PnutsFunction function, Object lock){
		this(function, lock, function.getName());
	}

	public SynchronizedFunction(PnutsFunction function, Object lock, String name){
		this.function = function;
		this.lock = lock;
		this.name = name;
	}

	public boolean defined(int nargs){
		return function.defined(nargs);
	}

	public String unparse(int nargs){
		return function.unparse(nargs);
	}

	public Package getPackage(){
		return function.getPackage();
	}

	public String[] getImportEnv(int narg){
		return function.getImportEnv(narg);
	}

	public boolean isBuiltin(){
		return function.isBuiltin();
	}

	public Object accept(int narg, Visitor visitor, Context context){
		return function.accept(narg, visitor, context);
	}
	
	public String getName(){
		return name;
	}

	public String toString(){
		return "synchronized " + function.toString();
	}

	protected void added(int narg){
		throw new RuntimeException("unsupported operation");
	}

	protected Object exec(Object[] args, Context context){
		synchronized(lock){
			return PnutsFunction.exec(function, args, context);
		}
	}
}
