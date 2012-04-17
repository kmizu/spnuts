/*
 * @(#)ThreadAdapter.java 1.4 05/04/04
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.multithread;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Escape;

/**
 * An adapter class between PnutsFunction and Runnable
 */
public class ThreadAdapter implements Runnable {
	PnutsFunction func;
	Context context;
	
	public ThreadAdapter(PnutsFunction func){
		this(func, new Context());
	}

	public ThreadAdapter(PnutsFunction func, Context context){
		this.func = func;
		this.context = context;
	}

	public void run(){
		try {
			func.call(new Object[]{}, new Context(context));
		} catch (Escape e){
			// skip
		}
	}
}
