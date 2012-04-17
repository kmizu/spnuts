/*
 * @(#)DefaultFunctionSerializer.java 1.2 05/06/21
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.*;
import java.util.*;
import pnuts.lang.*;
import pnuts.compiler.Compiler;
import org.pnuts.lang.*;

class DefaultFunctionSerializer implements Runtime.FunctionSerializer {

	public void serialize(PnutsFunction pnutsFunction, ObjectOutputStream s)
		throws IOException
	{
		Function[] functions = pnutsFunction.functions;
		s.writeUTF(pnutsFunction.pkg.getName());
		int n = functions.length;
		s.writeInt(n);
		for (int i = 0; i < n; i++) {
			Function f = functions[i];
			SimpleNode node = null;
			if (f != null){
				node = f.node;
				if (node != null){     // the function was written in Pnuts
					s.writeObject(f);
				} else {               // the function was written in Java
					node = f.getNode();
					if (node != null){
					    s.writeObject(node);  // nodes was deserialized properly
					} else {
					    s.writeObject(f.unparse(null)); // nodes was reconstracted from the script
					}
					f.writeAttributes(s);
				}
			} else {
				s.writeObject(null);
			}
		}
	}

	public void deserialize(PnutsFunction pnutsFunction, ObjectInputStream s)
		throws IOException, ClassNotFoundException
	{
		String pkgName = s.readUTF();
		Context threadContext = Runtime.getThreadContext();
		int n = s.readInt();
		Function[] functions = new Function[n];
		pnutsFunction.functions = functions;
		boolean[] compile = new boolean[n];
		boolean compileString = false;
		boolean compileNode = false;
		StringBuffer sbuf = new StringBuffer();
		List nodes = new ArrayList();
		Package pkg = null;

		for (int i = 0; i < n; i++) {
			Object f = s.readObject();
			if (f instanceof Function){  // AST interpreter
				functions[i] = (Function)f;
			} else if (f instanceof SimpleNode){
				SimpleNode ss = new SimpleNode(PnutsParserTreeConstants.JJTSTARTSET);
				SimpleNode node = (SimpleNode)f;
				Function func = new Function();
				functions[i] = func;
				compile[i] = true;
				compileNode = true;
				func.node = node.jjtGetChild(1);
				func.readAttributes(s);
				func.setPackage(Package.find(func.pkgName, threadContext));
				NodeUtil.setPackage(func.pkgName, ss);
				String[] imports = func.importEnv.list();
				for (int j = 0; j < imports.length; j++){
					NodeUtil.addImportNode(imports[j], ss);
				}
				NodeUtil.addFunction(node, ss);
				nodes.add(ss);

			} else if (f instanceof String){
				String unparsed = (String)f;
				try {
					Function func = new Function();
					func.readAttributes(s);
					func.setPackage(Package.find(func.pkgName, threadContext));
					sbuf.append("package(\"");
					sbuf.append(func.pkgName);
					sbuf.append("\")\n");
					String[] imports = func.importEnv.list();
					for (int j = 0; j < imports.length; j++){
						sbuf.append("import " + imports[j] + "\n");
					}
					sbuf.append((String)f);
					sbuf.append("\n");
					PnutsParser parser = Pnuts.getParser(new StringReader(unparsed));
					SimpleNode node = parser.FunctionStatement(null);
					func.node = node.jjtGetChild(1);
					func.function = pnutsFunction;
					functions[i] = func;
					compile[i] = true;
					compileString = true;
				} catch (ParseException pe){
					pe.printStackTrace();
				}
			}
		}
		pnutsFunction.pkg = Package.find(pkgName, threadContext);
		Compiler compiler = null;
		if (compileString || compileNode){
			compiler = new pnuts.compiler.Compiler();
			pkg = new Package(null, null);
		}
		if (compileString){
			Context c2 = (threadContext != null) ? (Context)threadContext.clone() : new Context();
			c2.setCurrentPackage(pkg);
			Pnuts compiled = compiler.compile(sbuf.toString(), c2);
			PnutsFunction pf = (PnutsFunction)compiled.run(c2);
			pf.pkg = Package.find(pkgName, threadContext);
			for (int i = 0; i < functions.length; i++){
				Function f = functions[i];
				if (f != null && compile[i]){
					Function f2 = pf.functions[i];
					f2.moduleList = f.moduleList;
					f2.config = f.config;
					Package p = Package.getPackage(f.pkg.getName(), c2);
					for (Enumeration e = f.pkg.bindings(); e.hasMoreElements();){
						Binding binding = (Binding)e.nextElement();
						p.set(binding.getName(), binding.get());
					}
					f2.pkg = p;
					f2.pkgName = f.pkgName;
					f2.file = null;
					functions[i] = f2;
				}
			}
		}
		if (compileNode){
			Context c2 = (threadContext != null) ? (Context)threadContext.clone() : new Context();
			if (pkg != null){
				c2.setCurrentPackage(pkg);
			}
			for (int j = 0, sz = nodes.size(); j < sz; j++){
				SimpleNode ss = (SimpleNode)nodes.get(j);
	
				Pnuts compiled = compiler.compile(new P(ss), c2);
				PnutsFunction pf = (PnutsFunction)compiled.run(c2);
				for (int i = 0; i < pf.functions.length; i++){
					Function f = functions[i];
					if (f != null && compile[i]){
						Function f2 = pf.functions[i];
						if (f2 == null){
							continue;
						}
						f2.moduleList = f.moduleList;
						f2.config = f.config;
						f2.pkg = f.pkg;
						f2.pkgName = f.pkgName;
						f2.file = f.file;
						functions[i] = f2;
					}
				}
			}
		}
	}

	static class P extends Pnuts {
		public P(SimpleNode ss){
			this.startNodes = ss;
		}
	}
}
