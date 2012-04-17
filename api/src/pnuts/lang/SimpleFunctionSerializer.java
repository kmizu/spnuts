/*
 * @(#)SimpleFunctionSerializer.java 1.1 05/06/21
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.*;

class SimpleFunctionSerializer implements Runtime.FunctionSerializer {

	public void serialize(PnutsFunction pnutsFunction, ObjectOutputStream s)
		throws IOException
	{
		Function[] functions = pnutsFunction.functions;
		int n = functions.length;
		s.writeInt(n);
		for (int i = 0; i < n; i++) {
			Function f = functions[i];
			if (f != null && f.node == null){
				s.writeObject(f.unparse(null));
				f.writeAttributes(s);
//				s.writeObject(f.pkg);
			} else {
				s.writeObject(f);
			}
		}
	}

	public void deserialize(PnutsFunction pnutsFunction, ObjectInputStream s)
		throws IOException, ClassNotFoundException
	{
		int n = s.readInt();
		Function[] functions = new Function[n];
		pnutsFunction.functions = functions;
		for (int i = 0; i < n; i++) {
			Object f = s.readObject();
			if (f instanceof Function){
				functions[i] = (Function)f;
			} else if (f instanceof SimpleNode){
				SimpleNode node = (SimpleNode)f;
				Function func = new Function();
				func.readAttributes(s);
//				func.pkg = (Package)s.readObject();				
				func.node = node.jjtGetChild(1);
				func.function = pnutsFunction;
				functions[i] = func;
			} else if (f instanceof String){
				try {
					Function func = new Function();
					func.readAttributes(s);
//					func.pkg = (Package)s.readObject();
					PnutsParser parser = Pnuts.getParser(new StringReader((String)f));
					SimpleNode node = parser.FunctionStatement(null);
					func.node = node.jjtGetChild(1);
					func.function = pnutsFunction;
					functions[i] = func;
				} catch (ParseException pe){
					pe.printStackTrace();
				}
			}
		}
	}

}
