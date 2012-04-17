/*
 * @(#)PnutsThreadGroup.java 1.1 05/04/04
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import pnuts.lang.Runtime;
import pnuts.lang.Context;

class PnutsThreadGroup extends ThreadGroup {
	Context context;

	public PnutsThreadGroup(String name) {
		super(name);
	}

	public PnutsThreadGroup(ThreadGroup parent, String name) {
		super(parent, name);
	}

	public PnutsThreadGroup(Context context){
		super("");
		this.context = context;
	}

	public void uncaughtException(Thread t, Throwable e) {
		Runtime.printError(e, context);
	}			
}
