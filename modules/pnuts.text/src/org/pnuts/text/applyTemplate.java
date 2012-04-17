/*
 * @(#)applyTemplate.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.text;

import pnuts.text.Template;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Runtime;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import java.util.Map;
import java.io.Writer;
import java.io.IOException;

/*
 * applyTemplate(template, map)
 */
public class applyTemplate extends PnutsFunction {

	public applyTemplate(){
		super("applyTemplate");
	}

	public boolean defined(int nargs){
		return (nargs == 2);
	}

	public static PnutsFunction getResult(final Template t, final Map def){
		return new PnutsFunction(){
				public boolean defined(int nargs){
					return nargs < 2;
				}
				protected Object exec(Object[] args, Context context){
					int nargs = args.length;
					Writer writer;
					if (nargs == 0){
						writer = context.getWriter();
					} else if (nargs == 1){
						writer = (Writer)args[0];
					} else {
						undefined(args, context);
						return null;
					}
					try {
						t.format(def, writer, context);
					} catch (IOException e){
						throw new PnutsException(e, context);
					}
					return null;
				}
			};
	}

	protected Object exec(Object args[], Context context){
		if (args.length == 2){
			return getResult((Template)args[0], (Map)args[1]); 
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function applyTemplate(Template, Map)";
	}
}
