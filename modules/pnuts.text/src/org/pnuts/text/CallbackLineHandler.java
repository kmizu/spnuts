/*
 * @(#)CallbackLineHandler.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import pnuts.lang.*;
import java.lang.reflect.*;

/**
 * A LineHandler implementation that calls a PnutsFunction.
 */
class CallbackLineHandler implements LineHandler {

	protected PnutsFunction func;
	protected Context context;
	protected AbstractLineReader lineReader;

	public CallbackLineHandler(PnutsFunction func, Context context){
		this.func = func;
		this.context = context;
	}

	public void process(char[] cb, int offset, int length){
		func.call(new Object[]{new String(cb, offset, length)}, context);
	}
}
