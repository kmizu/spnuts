/*
 * @(#)ScriptPackage.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lib;

import pnuts.lang.Pnuts;
import pnuts.lang.PnutsImpl;
import pnuts.lang.Implementation;
import pnuts.lang.Runtime;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.AbstractData;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Package that is used in user's scripts as an hashtable.
 * Unlike pnuts.lang.Package, this class implements AbstractData, so that
 * expression like <code>scriptPackage.fname(args...)</code> is interpreted as
 * a call of function 'fname' of the script package.
 *
 * When <code>scriptPackage.print</code> is a zero-arg function,
 * ScriptPackage.toString() method calls the function to obtain the string
 * representation of the object.
 */
public class ScriptPackage extends Package implements AbstractData {

	private static Implementation pureImpl = new PnutsImpl();

	public ScriptPackage(){
	}

	public ScriptPackage(String name){
		this(name, null);
	}

	public ScriptPackage(String name, Package parent){
		super(name, parent, null);
	}

	public Object invoke(String name, Object args[], Context context){
		Object o = get(name, context);
		if (o instanceof PnutsFunction){
			return ((PnutsFunction)o).call(args, context);
		} else if (o instanceof Class){
			return context.getConfiguration().callConstructor(context, (Class)o, args, null);
		} else {
			throw new PnutsException("funcOrType.expected", new Object[]{Pnuts.format(o)}, context);
		}
	}

	/**
	 * Defines a special function to create <a href="../../../doc/script_package.html">script packages</a>.
	 */
	public static class Function extends PnutsFunction implements AbstractData {

		private Package prototype;

		public Function(){
			this("$");
		}

		public Function(String name){
			this(name, new ScriptPackage(null, null));
		}

		public Function(String name, Package prototype){
			super(name);
			this.prototype = prototype;
		}

		/**
		 * @return null
		 */
		public Object get(String name, Context context){
			return null;
		}

		/**
		 * Do nothing.
		 */
		public void set(String name, Object value, Context context){
		}

		/**
		 * Provides these methods:
		 *<pre>
		 * $.set(pkg, "name", value)
		 * $.get(pkg, "name")
		 * $.defined(pkg, "name")
		 * $.clear(pkg, "name")
		 * $.keys(pkg)
		 * $.save(pkg, stream)
		 * $.clone(pkg)
		 *</pre>
		 */
		public Object invoke(String name, Object[] args, Context context){
			if ("call".equals(name)){
				return call(args, context);
			} else if ("set".equals(name)){
				if (args.length == 3){
					Package pkg = (Package)args[0];
					String sym = (String)args[1];
					Object value = args[2];
					pkg.set(sym.intern(), value, context);
					return null;
				}
			} else if ("get".equals(name)){
				if (args.length == 2){
					Package pkg = (Package)args[0];
					String sym = (String)args[1];
					return pkg.get(sym.intern(), context);
				} 
			} else if ("clear".equals(name)){
				if (args.length == 2){
					Package pkg = (Package)args[0];
					String sym = (String)args[1];
					pkg.clear(sym.intern(), context);
					return null;
				}
			} else if ("defined".equals(name)){
				if (args.length == 2){
					Package pkg = (Package)args[0];
					String sym = (String)args[1];
					boolean b = pkg.defined(sym.intern(), context);
					return b ? Boolean.TRUE : Boolean.FALSE;
				}
			} else if ("keys".equals(name)){
				if (args.length == 1){
					Package pkg = (Package)args[0];
					return pkg.keys();
				}
			} else if ("clone".equals(name)){
				if (args.length == 1){
					Package pkg = (Package)args[0];
					return pkg.clone();
				}
			} else if ("save".equals(name)){
				if (args.length == 2){
					Package pkg = (Package)args[0];
					try {
						ObjectOutputStream o =
							new ObjectOutputStream((OutputStream)args[1]);
						o.writeObject(pkg);
						return null;
					} catch (IOException e){
						throw new PnutsException(e, context);
					}
				}
			}
			throw new PnutsException("method.notFound", 
									 new Object[]{name,
												  Function.this.toString(), 
												  ""+Pnuts.format(args)},
									 context);
		}

		public boolean defined(int nargs){
			return nargs >= -1;
		}

		/**
		 * Creates a <a href="../../../doc/script_package.html">script package</a>.
		 */
		protected Object exec(Object[] args, Context context){
			if (args.length == 0){
				return prototype.clone();
			} else if (args.length == 1 && args[0] instanceof String){
				Context c = new Context(context);
				c.setImplementation(pureImpl);
				Package p = (Package)prototype.clone();
				c.setCurrentPackage(p);
				Pnuts.eval((String)args[0], c);
				return p;
			} else if (args.length == 1 && args[0] instanceof Object[]){
				Object[] list = (Object[])args[0];
				Package sp = (Package)prototype.clone();
				for (int i = 0; i < list.length; i++){
					Object[] pair = (Object[])list[i];
					String name = (String)pair[0];
					sp.set(name.intern(), pair[1], context);
				}
				return sp;
			} else {
				Package sp = (Package)prototype.clone();
				for (int i = 0; i < args.length; i++){
					Object arg = args[i];
					if (arg instanceof PnutsFunction){
						String name = ((PnutsFunction)arg).getName();
                                                if (name != null){
                                                    int idx = name.lastIndexOf('.');
                                                    if (idx >= 0){
							name = name.substring(idx + 1);
                                                    }
                                                    sp.set(name.intern(), arg, context);
                                                }
					}
				}
				return sp;
			}
		}
	}

	private final static String PRINT_FUNCTION = "print".intern();

	public PnutsFunction getPrintFunction() {
		Object o = get(PRINT_FUNCTION);
		if (o instanceof PnutsFunction){
			PnutsFunction f = (PnutsFunction)o;
			if (f.defined(0)){
				return f;
			}
		}
		return null;
	}

	public String toString(){
		PnutsFunction printFunction = getPrintFunction();
		if (printFunction != null){
			Context context = new Context();
			StringWriter sw = new StringWriter();
			context.setWriter(sw);
			printFunction.call(new Object[]{}, context);
			return sw.toString();
		} else {
			return "<script package>";
		}
	}
}
