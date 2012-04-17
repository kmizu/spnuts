/*
 * PnutsFunction.java
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A PnutsFunction represents a group of Pnuts functions with a same name.
 * <p>
 * This class is serializable, whether the function is compiled or not.
 * When an PnutsFunction object is serialized, the function definitions
 * are written to the object stream, along with its attributes such as
 * configuration, import environment, current package, and module list.
 * Note that the current package is deeply copied, but the module list are
 * written as an array of module names.</p>
 * <p>
 * When the function is deserialized, the function definition is restored
 * from the function definition read from the object stream.  If the function
 * had been compiled, the script is compiled.  If modules referenced by the
 * function are not used in the current context, the module is initialized for
 * the function in such a way that it does not affect the current context.</p>
 * <p>
 * On AST interpreter, nested functions with lexical scope can be
 * serialized/deserialized. But with bytecode compiler, only top-level
 * functions can be serialized/deserialized.</p>
 * <p>
 * Serialized objects of this class will not be compatible with
 * future releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing. </p>
 */
public class PnutsFunction extends Runtime implements Callable, Cloneable,
		Serializable {

	static final long serialVersionUID = -8114380088747362228L;

	transient Function[] functions = new Function[4];

	int count = 0; // The number of Functions

	/**
	 * the name
	 * 
	 * @serial
	 */
	protected String name;

	final static String _getContext = "getContext".intern();

	final static String _package = "package".intern();

	final static String _import = "import".intern();

	final static String _catch = "catch".intern();

	final static String _throw = "throw".intern();

	final static String _eval = "eval".intern();

	final static String _loadFile = "loadFile".intern();

	final static String _load = "load".intern();

	final static String _autoload = "autoload".intern();

	final static String _quit = "quit".intern();

	final static String _defined = "defined".intern();

	final static String _use = "use".intern();

	final static String _unuse = "unuse".intern();

	final static String _class = "class".intern();

	final static String _require = "require".intern();

	public final static PnutsFunction GET_CONTEXT = new Builtin(_getContext);

	public final static PnutsFunction PACKAGE = new Builtin(_package);

	public final static PnutsFunction IMPORT = new Builtin(_import);

	public final static PnutsFunction CATCH = new Builtin(_catch);

	public final static PnutsFunction THROW = new Builtin(_throw);

	public final static PnutsFunction EVAL = new Builtin(_eval);

	public final static PnutsFunction LOAD_FILE = new Builtin(_loadFile);

	public final static PnutsFunction LOAD = new Builtin(_load);

	public final static PnutsFunction AUTOLOAD = new Builtin(_autoload);

	public final static PnutsFunction QUIT = new Builtin(_quit);

	public final static PnutsFunction DEFINED = new Builtin(_defined);

	public final static PnutsFunction USE = new Builtin(_use);

	public final static PnutsFunction UNUSE = new Builtin(_unuse);

	public final static PnutsFunction CLASS = new Builtin(_class);

	public final static PnutsFunction REQUIRE = new Builtin(_require);

	final static PnutsFunction primitives[] = { GET_CONTEXT, PACKAGE,
			IMPORT, THROW, EVAL, LOAD_FILE, LOAD, AUTOLOAD, QUIT, DEFINED, USE,
			UNUSE, CLASS, REQUIRE };

	protected transient Package pkg = Package.globalPackage;

	private PnutsFunction parent = null;

	protected PnutsFunction() {
		this(null);
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 *            the name of the function
	 */
	protected PnutsFunction(String name) {
		this.name = name;
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 *            the name of the function
	 * @param parent
	 *            the parent function
	 */
	protected PnutsFunction(String name, PnutsFunction parent) {
		this.name = name;
		this.parent = parent;
	}

	synchronized void put(int narg, Function f) {
		int n = narg + 1;
		if (functions.length < n + 1) {
			Function func[] = new Function[(n + 1) * 2];
			System.arraycopy(functions, 0, func, 0, functions.length);
			functions = func;
		}
		functions[n] = f;
		f.function = this;
		count++;
		added(narg);
	}

	/**
	 * This method is called when a Function object is registered to this
	 * object. This method just returns. Subclass can override this as a hook
	 * method.
	 * 
	 * @param narg
	 *            the number of parameters
	 */
	protected void added(int narg) {
	}

	public final Function get(int narg) {
		int n = narg + 1;
		if (n >= functions.length) {
			if (parent != null){
				return parent.get(narg);
			} else {
				return null;
			}
		}
		Function f = functions[n];
		if (f != null){
			return f;
		}
		if (parent != null){
			return parent.get(narg);
		} else {
			return null;
		}
	}

	/**
	 * Check if the function with narg parameter is defined
	 * 
	 * @param narg the number of paramters.
	 * @return true if a function with narg, otherwise false
	 */
	public boolean defined(int narg) {
		Function f = get(narg);
		if (f != null) {
			return true;
		}
		f = functions[0];
		if (f != null){
		    if (narg >= f.nargs - 1){
			return true;
		    }
		}
		return parent != null && parent.defined(narg);
	}

	/**
	 * @return the name of functions
	 */
	public String getName() {
		return name;
	}

	/**
	 * Call a function in "context" with arguments "args". Increments the
	 * counter for Pnuts.evalDepth() during the execution.
	 * 
	 * @return the result of the call
	 */
	public final Object call(Object[] args, Context context) {
		return exec(args, context);
	}

	/**
	 * Call a function in "context" with arguments "args". Subclasses of this
	 * class should override this method.
	 * 
	 * @return the result of the call
	 */
	protected Object exec(Object[] args, Context context) {
		Function f = null;
		try {
			f = functions[args.length + 1];
		} catch (IndexOutOfBoundsException e) {
		}
		if (f == null) {
			f = functions[0];
			if (f == null) {
				if (parent != null) {
					return parent.exec(args, context);
				} else {
					undefined(args, context);
					return null;
				}
			} else {
			    int n = f.nargs;
			    int len = args.length;
			    if (len < n - 1){
				undefined(args, context);
			    }
			    if (n == 1){
				args = new Object[]{args};
			    } else {
				Object[] newargs = new Object[n];
				for (int i = 0; i < n - 1; i++){
				    newargs[i] = args[i];
				}			    
				Object[] rests = new Object[len - n + 1];
				for (int i = 0; i < len - n + 1; i++){
				    rests[i] = args[i + n - 1];
				}
				newargs[n - 1] = rests;
				args = newargs;
			    }
			}
		}
		Function caller = context.frame;
		int line = context.beginLine;
		ImportEnv saved = context.importEnv;
		Package outerPackage = context.currentPackage;
//		boolean eval = context.eval;
		Configuration config = context.config;
		ModuleList moduleList = context.moduleList;
		ModuleList localModuleList = context.localModuleList;
		context.importEnv = f.importEnv;
		context.currentPackage = f.pkg;
//		context.eval = false;
		context.frame = f;
		context.config = f.config;
		context.moduleList = f.moduleList;
		context.localModuleList = null;
		try {
			return f.exec(args, context);
		} catch (Jump jump) {
			return jump.getValue();
		} catch (Escape esc) {
			throw esc;
		} catch (ThreadDeath td) {
			throw td;
		} catch (Throwable t) {
			PnutsException p = null;
			if (t instanceof PnutsException) {
				p = (PnutsException) t;
			} else {
				p = new PnutsException(t, context);
			}
			throw p;
		} finally {
			context.frame = caller;
			context.importEnv = saved;
//			context.eval = eval;
			context.currentPackage = outerPackage;
			context.config = config;
			context.moduleList = moduleList;
			context.localModuleList = localModuleList;
		}
	}

	protected void undefined(Object args[], Context context) {
		throw new PnutsException("function.notDefined", new Object[] { name,
				new Integer(args.length) }, context);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		int i = 0;
		while (i < functions.length) {
			Function f = functions[i++];
			if (f != null) {
				buf.append(f.toString());
				break;
			}
		}
		while (i < functions.length) {
			Function f = functions[i++];
			if (f != null) {
				buf.append(",");
				buf.append(f.paramString());
			}
		}
		if (buf.length() < 1) {
			if (name != null){
				buf.append("function " + name);
			} else {
				buf.append(getClass().getName() + "@" + Integer.toHexString(hashCode()));
			}
		}
		if (parent == null) {
			return buf.toString();
		} else {
			return buf + ", ...";
		}
	}

	/**
	 * call a function "name" in "context" with arguments "args"
	 */
	public static Object call(String name, Object args[], Context context) {
		Object o = context.resolveSymbol(name.intern());
		if (o instanceof PnutsFunction) {
			return exec((PnutsFunction) o, args, context);
		} else {
			throw new PnutsException("function.notDefined", new Object[] {
					name, new Integer(args.length) }, context);
		}
	}

	protected static Object exec(PnutsFunction func, Object args[],
			Context context) {
		return func.call(args, context);
	}

	/**
	 * Retrieve the symbolic definition of the function.
	 * 
	 * @param narg
	 *            the number of paramters. -1 means a arbitrary length
	 *            parameter.
	 * @return the function definition
	 */
	public String unparse(int narg) {
		Function f = get(narg);
		if (f != null) {
			return f.unparse(null);
		}
		return null;
	}

	/**
	 * @return Package in which the function is defined
	 *
	 * @deprecated
	 */
	public Package getPackage() {
		return pkg;
	}

       /*
	 * experimental
	 */
	public void setPackage(Package pkg) {
		this.pkg = pkg;
		for (Enumeration e = elements(); e != null && e.hasMoreElements();){
		    Function f  = (Function)e.nextElement();
		    f.setPackage(pkg);
		}
	}


	/**
	 * @param narg
	 *            the number of paramters. -1 means a arbitrary length
	 *            parameter.
	 * @return imports of the function (array of Class or String)
	 */
	public String[] getImportEnv(int narg) {
		Function f = get(narg);
		if (f != null) {
			return f.importEnv.list();
		}
		return null;
	}

	public boolean isBuiltin() {
		return false;
	}

	/**
	 * @param narg
	 *            the number of paramters. -1 means a arbitrary length
	 *            parameter.
	 */
	public Object accept(int narg, Visitor visitor, Context context) {
		Function f = get(narg);
		if (f != null) {
			return f.accept(visitor, context);
		}
		return null;
	}

	public Object clone() {
		try {
			PnutsFunction f = (PnutsFunction) super.clone();
			f.functions = (Function[]) functions.clone();
			return f;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	class Enum implements Enumeration {
		int idx = 0;

		int size = functions.length;

		public boolean hasMoreElements() {
			while (!defined(idx - 1) && idx < size) {
				idx++;
			}
			return idx < size;
		}

		public Object nextElement() {
			if (idx < size) {
				while (!defined(idx - 1)) {
					idx++;
				}
				return get(idx++ - 1);
			} else {
				throw new NoSuchElementException(PnutsFunction.this.toString());
			}
		}
	}

	protected Enumeration elements() {
		if (count > 0) {
			return new Enum();
		} else {
			return null;
		}
	}

	private void writeObject(ObjectOutputStream s)
			throws IOException {
		s.defaultWriteObject();
		Runtime.serializePnutsFunction(this, s);
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		Runtime.deserializePnutsFunction(this, s);
	}
}
