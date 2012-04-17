/*
 * formatDynamicPage.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.lang.*;
import java.io.*;

public class formatDynamicPage extends PnutsFunction {

	static PnutsFunction r = new readDynamicPage();

	public formatDynamicPage(){
		super("formatDynamicPage");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0 || nargs > 2){
			undefined(args, context);
		}
		Executable exec = (Executable)r.call(args, context);
		Context ctx = new Context(context);
		StringWriter sw = new StringWriter();
		ctx.setWriter(sw);
		exec.run(ctx);
		return sw.toString();
	}

	public String toString(){
		return "function formatDynamicPage((String|InputStream|Reader|File|URL) input {, encoding })";
	}
}
