/*
 * @(#)formatTemplate.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import pnuts.text.Template;
import pnuts.lang.*;
import java.util.*;
import java.io.*;

/*
 * formatTemplate(template, map)
 * formatTemplate(template, map, output)
 */
public class formatTemplate extends PnutsFunction {
	public formatTemplate(){
		super("formatTemplate");
	}

	public boolean defined(int nargs){
		return (nargs == 2 || nargs == 3);
	}

	protected Object exec(Object args[], Context context){
		int nargs = args.length;
		if (nargs == 2){
			PnutsFunction f = applyTemplate.getResult((Template)args[0], (Map)args[1]);
			Context c = (Context)context.clone();
			StringWriter buf = new StringWriter();
			c.setWriter(buf);
			f.call(new Object[]{}, c);
			return buf.toString();
		} else if (nargs == 3){
			PnutsFunction f = applyTemplate.getResult((Template)args[0], (Map)args[1]);
			Context c = (Context)context.clone();
			Object a = args[2];
			if (a instanceof OutputStream){
				c.setOutputStream((OutputStream)a);
			} else if (a instanceof Writer){
				c.setWriter((Writer)a);
			} else {
				throw new IllegalArgumentException(String.valueOf(a));
			}
			f.call(new Object[]{}, c);
			return null;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function formatTemplate(template, map {, output } )";
	}
}
