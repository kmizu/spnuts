/*
 * @(#)Pnuts.java 1.11 05/05/26
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;

import org.pnuts.util.Stack;
import org.pnuts.lang.DefaultParseEnv;
import org.pnuts.lang.PnutsClassLoader;

/**
 * This class provides a set of static methods to parse/execute scripts.
 * <p>
 * This object also represents a parsed script. </p>
 * <p>
 * This class is serializable. When a Pnuts object is serialized, the syntax
 * tree is written to the object stream, along with  the attributes such as
 * line, column, and script source.</p>
 * <p>
 * When the object is deserialized, the parsed script is restored using
 * the information read from the object stream.  If the script had been
 * compiled, the script should be recompiled.</p>
 * <p>
 * Serialized objects of this class will not be compatible with
 * future releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing. </p>
 */
public class Pnuts implements Executable, Serializable {

	//    final static boolean DEBUG = true;

	static final long serialVersionUID = 3808410089278038986L;

	/**
	 * The version number
	 */
	public final static String pnuts_version = "1.2.1";

	/**
	 * "prompt" string for the command shell
	 */
	public static String prompt = "> ";

	static String compiledClassPrefix;

	static Properties defaultSettings;

	private static Stack freeParsers = new Stack();

	private static int java2 = 0;

	static String getCompiledClassPrefix() {
		if (compiledClassPrefix == null) {
			try {
				compiledClassPrefix = Runtime
						.getProperty("pnuts.compiled.script.prefix");
				if (compiledClassPrefix == null) {
					compiledClassPrefix = "";
				} else if (!"".equals(compiledClassPrefix)
						&& !compiledClassPrefix.endsWith(".")) {
					compiledClassPrefix = compiledClassPrefix + ".";
				}
			} catch (Throwable t) {
			}
		}
		return compiledClassPrefix;
	}

	static PnutsParser getParser(Reader reader) {
		synchronized (freeParsers) {
			if (freeParsers.size() > 0) {
				PnutsParser parser = (PnutsParser) freeParsers.pop();
				parser.ReInit(reader);
				return parser;
			} else {
				return new PnutsParser(reader);
			}
		}
	}

	static void recycleParser(PnutsParser parser) {
		synchronized (freeParsers) {
			if (freeParsers.size() < 3) {
				freeParsers.push(parser);
			}
		}
	}

	/**
	 * Checks if the runtime environment supports J2SE.
	 * 
	 * @return true if the runtime environment supports J2SE
	 */
	final public static boolean isJava2() {
		if (java2 == 0) {
			try {
				if (Runtime.getProperty("pnuts.jdk11.compatible") != null) {
					java2 = -1;
				} else {
					Class.forName("java.lang.Package");
					java2 = 1;
				}
			} catch (Throwable t) {
				java2 = -1;
			}
		}
		if (java2 == 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Sets properties that affect the behavior of Pnuts interpreter/compiler.
	 * 
	 * This method should be called before the classes that read the default
	 * settings, such as pnuts.lang.Configuration and pnuts.lang.PnutsImpl.
	 * Once those classes are loaded, this method call has no effect.
	 * 
	 * <pre>
	 * Pnuts.setDefaults(properties);
	 * Context c = new Context(); // this line should not precede setDefaults() call.
	 * 
	 * </pre>
	 * 
	 * @param properties
	 *            the properties that override the system properties.
	 */
	public static void setDefaults(Properties properties) {
		defaultSettings = properties;
	}

	/**
	 * Gets the properties previously set by setDefaults() method.
	 * 
	 * @return the default setting that affects the behavior of Pnuts
	 *         interpreter/compiler.
	 */
	public static Properties getDefaults() {
		return defaultSettings;
	}

	/**
	 * Loads the class by the following order.
	 * <OL>
	 * <LI>A class loader associated with Pnuts context.
	 * <LI>The class loader associated with the current Thread (J2SE).
	 * <LI>The class loader by which Pnuts classes are loaded.
	 * </OL>
	 * 
	 * @param name
	 *            the class name to be loaded
	 * @param context
	 *            the context in which the class is loaded
	 * @return the loaded class. Note that it is not initialized.
	 */
	public final static Class loadClass(String name, Context context)
			throws ClassNotFoundException {
		ClassLoader classLoader = context.getClassLoader();
		if (classLoader != null) {
			try {
				return classLoader.loadClass(name);
			} catch (ClassNotFoundException e1) {
			} catch (LinkageError e2) {
			}
		}
		if (isJava2()) {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl != null && ccl != classLoader) {
				try {
					return ccl.loadClass(name);
				} catch (ClassNotFoundException e1) {
				} catch (LinkageError e2) {
				}
			}
		}
		return Class.forName(name);
	}

	/**
	 * Get the resource URL.
	 * 
	 * @param s
	 *            the resource name
	 * @param context
	 *            the context in which the resource is read
	 */
	public final static URL getResource(String s, Context context) {
		ClassLoader classLoader = context.getClassLoader();
		if (classLoader != null) {
			URL url = classLoader.getResource(s);
			if (url != null) {
				return url;
			}
		}
		if (isJava2()) {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl != null) {
				URL url = ccl.getResource(s);
				if (url != null) {
					return url;
				}
			}
			return Pnuts.class.getResource(s);
		} else {
			return ClassLoader.getSystemResource(s);
		}
	}

	/**
	 * Sets a "prompt" string for the command shell
	 */
	public static void setPrompt(String str) {
		prompt = str;
	}

	/**
	 * Sets the verbose mode
	 * 
	 * @deprecated replaced by Context.setVerbose()
	 */
	public static void setVerbose(boolean b) {
		Context.defaultVerboseMode = b;
	}

	/**
	 * Check the current verbose mode
	 * 
	 * @return the current verbose mode
	 * 
	 * @deprecated replaced by Context.isVerbose()
	 */
	public static boolean isVerbose() {
		return Context.defaultVerboseMode;
	}

	/**
	 * Returns the string representation of an object. When the object is a
	 * number, a character, a boolean, or a string, it can be reconstructed by
	 * eval() function.
	 * 
	 * @param obj
	 *            the object.
	 * @return the string representation of the object
	 */
	public static String format(Object obj) {
		return Runtime.format(obj, 64);
	}

	/**
	 * Get the value of a global variable
	 * 
	 * @param str
	 *            the name of the global variable
	 * @return the value of the global variable <em>str</em>
	 * 
	 * @deprecated replaced by Context.getCurrentPackage().get(str.intern())
	 */
	public static Object get(String str) {
		return get(str, "");
	}

	/**
	 * Gets a global variable.
	 * 
	 * @param str
	 *            the name of the variable
	 * @param pkg
	 *            the package where the variable is defined
	 * @return the value of a variable "str" in the package "pkg"
	 * 
	 * @deprecated replaced by Package.getPackage(pkg, null).get(str)
	 */
	public static Object get(String str, String pkg) {
		Package p = Package.find(pkg);
		if (p != null) {
			return p.get(str.intern());
		} else {
			return null;
		}
	}

	/**
	 * set a value "val" to a global variable "str"
	 * 
	 * @deprecated replaced by context.getCurrentPackage().set(str.intern(),
	 *             val)
	 */
	public static void set(String str, Object val) {
		set(str, val, "");
	}

	/**
	 * Set a value "val" to a variable "str" in package "pkg"
	 * 
	 * @param str
	 * @param val
	 * @param pkg
	 * 
	 * @deprecated replaced by Package.getPackage(pkg, null).set(str, val)
	 */
	public static void set(String str, Object val, String pkg) {
		if (str.length() > 0 && Character.isJavaIdentifierStart(str.charAt(0))) {
			Package p = Package.find(pkg);
			if (p != null) {
				p.set(str.intern(), val);
			} else {
				throw new IllegalArgumentException(Runtime.getMessage(
						"pnuts.lang.pnuts", "package.notFound",
						new Object[] { pkg }));
			}
		} else {
			throw new IllegalArgumentException(Runtime.getMessage(
					"pnuts.lang.pnuts", "illegal.symbolForId",
					Runtime.NO_PARAM));
		}
	}

	/**
	 * Evaluates "str" in "context"
	 * 
	 * @param expr
	 *            the expression to be evaluated
	 * @param context
	 *            the context in which the expression is evaluated
	 * @return the result of the evaluation
	 */
	public static Object eval(String expr, Context context) {
		return context.pnutsImpl.eval(expr, context);
	}

	/**
	 * Loads a local script "file" in "context"
	 * 
	 * @param file
	 *            the script file to be loaded.
	 * @param context
	 *            the context in which the file is loaded.
	 */
	public static Object loadFile(String file, Context context)
			throws FileNotFoundException
	{
		Thread th = Thread.currentThread();
		ClassLoader ccl = th.getContextClassLoader();
		try {
			return context.pnutsImpl.loadFile(file, context);
		} finally {
			th.setContextClassLoader(ccl);
		}
	}

	/**
	 * Loads a script "file" in "context"
	 * 
	 * @param name
	 *            the name of the script to be loaded
	 * @param context
	 *            the context in which the script is loaded.
	 */
	public static Object load(String name, Context context)
			throws FileNotFoundException
	{
		Thread th = Thread.currentThread();
		ClassLoader ccl = th.getContextClassLoader();
		String file = name;
		if (!name.endsWith(".pnut")) {
			file = name + ".pnut";

			final Executable rt = Runtime.getCompiledScript(name, context);

			if (rt != null) {
				if (context.verbose) {
					System.out.println("[loading "
							+ Pnuts.format(rt.getClass()) + "]");
				}
				int depth = enter(context);
				context.pushFile(rt);
				boolean completed = false;
				Context old = Runtime.getThreadContext();
				try {
					context.provide(name);
					Runtime.setThreadContext(context);
					Object ret = rt.run(context);
					completed = true;
					return ret;
				} catch (Jump jump) {
					completed = true;
					return jump.getValue();
				} finally {
					if (!completed){
						context.revoke(name);
					}
					th.setContextClassLoader(ccl);
					context.popFile();
					context.depth = depth;
					Runtime.setThreadContext(old);
				}
			}
		}
		try {
		    return context.pnutsImpl.load(file, context);
		} finally {
		    th.setContextClassLoader(ccl);
		}
	}

	/**
	 * Loads a script specifed as a URL.
	 * 
	 * @param url
	 *            the URL
	 * @param context
	 *            the context in which the script is loaded.
	 */
	public static Object load(URL url, Context context) {
	    Thread th = Thread.currentThread();
	    ClassLoader ccl = th.getContextClassLoader();
	    try {
		return context.pnutsImpl.load(url, context);
	    } finally {
		th.setContextClassLoader(ccl);
	    }
	}

	/**
	 * Loads a script from InputStream "in" in "context"
	 * 
	 * @param in
	 *            the input stream from which the script can be read.
	 * @param context
	 *            the context in which the script is loaded.
	 */
	public static Object load(InputStream in, Context context) {
		return load(Runtime.getScriptReader(in, context), context);
	}

	/**
	 * Load a script from an InputStream in the specified Context.
	 * 
	 * @param in
	 *            an InputStream from which the interpreter reads an input
	 * @param interactive
	 *            <ul>
	 *            <li>When "interactive" is true, the greeting message, the
	 *            prompt, and the results of evaluations are displayed. When an
	 *            exception is thrown and not caught by any exception handler,
	 *            it is caught at the top level of the interpreter, display an
	 *            error message, and resume the interactive session. If the
	 *            exception is caught by a handler that is registered at the top
	 *            level, the result of the handler becomes the return value of
	 *            the last expression.
	 * 
	 * <li>When "interactive" is false, exceptions are caught at the top level
	 * of the interpreter and exits this function. If the exception thrown is
	 * caught by a handler that is registered at the top level, the result of
	 * the handler becomes the return value of this method.
	 * </ul>
	 * @param context
	 *            a Context in which the interpretation is taken place.
	 * @return the result of the last expression
	 */
	public static Object load(InputStream in, boolean interactive,
			Context context) {
		return load(Runtime.getScriptReader(in, context), interactive, context);
	}

	/**
	 * This method loads a script
	 * 
	 * @param reader
	 *            the Reader from which the script is loaded
	 * @param context
	 *            the context in which the script is loaded
	 * @return the result of the last expression
	 */
	public static Object load(Reader reader, Context context) {
		PnutsParser parser = getParser(reader);
		synchronized (context.namespaceRefreshed) {
			context.namespaceRefreshed[0] = false;
		}
		Thread th = Thread.currentThread();
		ClassLoader ccl = th.getContextClassLoader();

		int depth = enter(context);
		Object value = null;
		try {
			while (true) {
				try {
					SimpleNode start = parser.Start(DefaultParseEnv
							.getInstance());
					switch (start.toplevel) {
					case -1:
						start.toplevel = 0;
						return value;
					case 0:
						reset(parser);
						break;
					case 1:
						value = context.pnutsImpl.accept(start, context);
						if (start.toplevel == -1) {
							return start.value;
						}
						reset(parser);
						context.onExit(value);
						break;
					}
				} catch (ParseException p) {
					Runtime.checkException(context, p);
				} catch (Escape esc) {
					reset(parser);
					throw esc;
				} catch (Throwable t) {
					if (t instanceof PnutsException) {
						throw (PnutsException) t;
					} else if (t instanceof ThreadDeath) {
						throw (ThreadDeath) t;
					} else {
						throw new PnutsException(t, context);
					}
				}
			}
		} catch (Escape esc) {
			context.onExit(value);
			flush(context);
			return esc.getValue();
		} catch (PnutsException pe){
			context.onError(pe);
			throw pe;
		} catch (Throwable t) {
			PnutsException e = new PnutsException(t, context);
			context.onError(t);
			throw e;
		} finally {
			th.setContextClassLoader(ccl);
			Executable hook = context.exitHook;
			if (hook != null) {
				hook.run(context);
			}
			context.depth = depth;
			recycleParser(parser);
		}
	}

	/**
	 * all public entry points must go through this to ensure the correct
	 * behavior of evalDepth method.
	 * 
	 * @param context
	 *            the context in which the execution is taken place.
	 * @return the last value of context.depth.
	 */
	static int enter(Context context) {
		int depth = context.depth;
		if (depth < 0x7fffffff) {
			context.depth++;
		}
		return depth;
	}

	/**
	 * Get the depth of evaluation.
	 * 
	 * This value increases when load(), loadFile(), or eval() is called.
	 * 
	 * @param context
	 *            the context of the evaluation.
	 */
	public static int evalDepth(Context context) {
		return context.depth;
	}

	/**
	 * This method loads a script
	 * 
	 * @param reader
	 *            the Reader from which the script is loaded
	 * @param interactive
	 *            specifies if the execution is in interactive mode.
	 * @param context
	 *            the context in which the script is loaded
	 * @return the result of the last expression
	 */
	public static Object load(Reader reader, boolean interactive,
			Context context) {
		if (interactive) {
			return session(reader, context);
		} else {
			return load(reader, context);
		}
	}

	/**
	 * Parses a script from InputStream and return a Pnuts object
	 * 
	 * @return the Pnuts object including a parsed syntax tree
	 * @param in
	 *            the InputStream
	 * @deprecated replaced by parse(Reader)
	 */
	public static Pnuts parse(InputStream in) throws ParseException, IOException {
		return parse(new InputStreamReader(in));
	}

	/**
	 * parse a script from Reader and return a Pnuts object
	 * 
	 * @return the Pnuts object including a parsed syntax tree
	 * @param reader
	 *            the Reader
	 * @since Pnuts 1.0beta3
	 */
	public static Pnuts parse(Reader reader) throws ParseException, IOException {
		return parse(reader, DefaultParseEnv.getInstance());
	}

	/**
	 * parse a script from Reader and return a Pnuts object
	 */
	public static Pnuts parse(Reader reader, ParseEnvironment env)
			throws ParseException, IOException {
		PnutsParser parser = getParser(reader);
		Pnuts p = new Pnuts();
		try {
			p.startNodes = parser.StartSet(env);
		} catch (ParseException e) {
			env.handleParseException(e);
		} finally {
			recycleParser(parser);
		}
		return p;
	}

	/**
	 * parse a script from Reader and return a Pnuts object
	 * 
	 * @return the Pnuts object including a parsed syntax tree
	 * @param reader
	 *            the Reader
	 * @param scriptSource
	 *            the script source
	 */
	public static Pnuts parse(Reader reader, Object scriptSource,
			Context context) throws IOException {
		return parse(reader, scriptSource, context,
			     DefaultParseEnv.getInstance(scriptSource));
	}

	/**
	 * parse a script from Reader and return a Pnuts object
	 * 
	 * @return the Pnuts object including a parsed syntax tree
	 * @param reader
	 *            the Reader
	 * @param scriptSource
	 *            the script source
	 * @param env
	 */
	public static Pnuts parse(Reader reader, Object scriptSource,
			Context context, ParseEnvironment env) throws IOException {
		Function frame = context.frame;
		context.frame = null;

		PnutsParser parser = getParser(reader);
		try {
			Pnuts p = new Pnuts();
			p.startNodes = parser.StartSet(env);
			p.setScriptSource(scriptSource);
			return p;
		} catch (ParseException e) {
			throw new PnutsException(e, context);
		} finally {
			context.frame = frame;
			recycleParser(parser);
		}
	}

	/**
	 * Parses a script and return a Pnuts object
	 * 
	 * @return the Pnuts object including a parsed syntax tree
	 * @param expr
	 *            the script
	 */
	public static Pnuts parse(String expr) throws ParseException {
	    try {
		Pnuts p = parse(new StringReader(expr));
		p.scriptSource = expr;
		return p;
	    }  catch (IOException e){
		throw new InternalError();
	    }
	}

	/**
	 * Loads a script "file" only if the script has not been read. It is
	 * guaranteed that the script runs at most once in this context.
	 * 
	 * @param file
	 *            the script file, which must be an intern'ed String.
	 * @param context
	 *            the context in which the script is loaded
	 */
	public static void require(String file, Context context)
			throws FileNotFoundException
        {
            require(file, context, false);
	}

        public static void require(String file, Context context, boolean checkForUpdate)
			throws FileNotFoundException
        {
 		if (file.endsWith(".pnut")) {
			file = file.substring(0, file.length() - 5);
		}
		file = file.intern();
		context.require(file, checkForUpdate);           
        }
        
	static Object session(Reader r, Context context) {
		BufferedReader br = new BufferedReader(r);
		StringBuffer sbuf = new StringBuffer();
		ParseEnvironment parseEnv = DefaultParseEnv.getInstance();
		Object value = null;
		while (true) {
			PnutsParser parser = null;
			try {
				PrintWriter term = null;
				flush(context);

				term = context.getTerminalWriter();
				if (term != null) {
					term.print(prompt);
					term.flush();
				}
				String line = br.readLine();
				if (line == null) {
					return value;
				}
				sbuf.append(line);
				StringReader sr = new StringReader(sbuf.toString());
				parser = getParser(sr);
				try {
					SimpleNode start = parser.Start(parseEnv);
					sbuf.setLength(0);
					try {
						value = context.pnutsImpl.accept(start, context);
					} catch (Jump jump) {
						value = jump.getValue();
					} finally {
						context.onExit(value);
					}

					try {
						term.println(context.getConfiguration().formatObject(
								value));
						term.flush();
					} catch (ThreadDeath td) {
						throw td;
					} catch (Throwable t) {
						Runtime.checkException(context, t);
					}

				} catch (ParseException pe) {
					Token t = pe.currentToken;
					Token t2 = t.next;
					while (t != null) {
						t2 = t;
						t = t.next;
					}
					if (t2.kind != PnutsParserConstants.EOF) {
						throw pe;
					}
					sbuf.append('\n');
				} finally {
					recycleParser(parser);
				}
			} catch (ParseException p) {
				sbuf.setLength(0);
				if (context.depth > 1) {
					Runtime.checkException(context, p);
				} else {
					int line = context.beginLine;
					try {
						context.beginLine = p.currentToken.next.beginLine;
						Runtime.checkException(context, p);
					} catch (Jump j) {
						value = j.getValue();
						PrintWriter term = context.getTerminalWriter();
						if (term != null) {
							term.println(value);
							term.flush();
						}
					} catch (PnutsException t) {
						Runtime.printError(t, context);
					} finally {
						context.beginLine = line;
					}
					reset(parser);
					recover(context);
					value = null;
				}
			} catch (Escape esc) {
				reset(parser);
				context.onExit(value);
				return esc.getValue();
			} catch (Throwable t) {
				if (context.depth > 1) {
					if (t instanceof PnutsException) {
						throw (PnutsException) t;
					} else if (t instanceof ThreadDeath) {
						throw (ThreadDeath) t;
					} else {
						throw new PnutsException(t, context);
					}
				}
				if (t instanceof ThreadDeath) {
					return null;
				}
				Runtime.printError(t, context);
				if (parser == null){
					break;
				}
				reset(parser);
				recover(context);
				value = null;
			}
		}
		return null;
	}

	private static void reset(PnutsParser parser) {
		parser.jjtree.reset();
	}

	private static void recover(Context context) {
		context.resetStackFrame();
		context.loadingResource = null;
	}

	private static void flush(Context context) {
		PrintWriter out = context.getWriter();
		if (out != null) {
			out.flush();
		}
	}

	/**
	 * Create a classloader that can compile scripted classes
	 * with the current thread's context classloader as its parent classloader
	 *
	 * @param context the context in which scripts are compiled
	 * @return the classloader
	 */
	public static ClassLoader createClassLoader(Context context){
	    return createClassLoader(context, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Create a classloader that can compile scripted classes
	 *
	 * @param context the context in which scripts are compiled
	 * @param parent the parent classloader
	 * @return the classloader
	 */
	public static ClassLoader createClassLoader(Context context, ClassLoader parent){
	    return new PnutsClassLoader(parent, context);
	}

	/**
	 * Parsed scripts
	 * 
	 * @serial
	 */
	protected SimpleNode startNodes = null;

	/**
	 * The script source, from where the script came. It is usually a URL
	 * object, but not limitted to. If this variable is not null, error message
	 * would include the positional information such as the line number and the
	 * file name.
	 */
	protected Object scriptSource;

	protected Pnuts() {
	}

	/**
	 * Executes a Pnuts object with the specified Context
	 * 
	 * @param context
	 *            the Context
	 * @return the result
	 */
	public Object run(Context context) {
		Thread th = Thread.currentThread();
		ClassLoader ccl = th.getContextClassLoader();
		int depth = enter(context);
		context.pushFile(scriptSource);
		Function frame = context.frame;
		context.frame = null;
		try {
			return accept(context);
		} finally {
			th.setContextClassLoader(ccl);
			context.depth = depth;
			context.frame = frame;
			context.popFile();
		}
	}

	/**
	 * Associates a script source with this parsed (compiled) expression.
	 * 
	 * @param src
	 *            the script source to be associated with.
	 */
	public void setScriptSource(Object src) {
		this.scriptSource = src;
	}

	/**
	 * Gets the script source associated with this parsed (compiled) expression
	 * 
	 * @return the script source to be associated with.
	 */
	public Object getScriptSource() {
		return this.scriptSource;
	}

	/**
	 * traverse the parsed tree with the specified Visitor and Context
	 * 
	 * @param context
	 *            the Context
	 * @return the result
	 * @since Pnuts 1.0beta3
	 */
	public Object accept(Visitor visitor, Context context) {
		int depth = enter(context);
		Object value = null;
		Context old = Runtime.getThreadContext();
		Runtime.setThreadContext(context);
		try {
			value = startNodes.accept(visitor, context);
			context.onExit(value);
			return value;
		} catch (Escape esc) {
			context.onExit(value);
			Object val = esc.getValue();
			flush(context);
			return val;
		} catch (PnutsException pe) {
			context.onError(pe);
			throw pe;
		} catch (Throwable t) {
			PnutsException e = new PnutsException(t, context);
			context.onError(t);
			throw e;
		} finally {
			context.depth = depth;
			Runtime.setThreadContext(old);
		}
	}

	/**
	 * Obtain the script code from a parsed object
	 * 
	 * @return the script code
	 */
	public String unparse() {
		return Runtime.unparse(startNodes, null);
	}

	/**
	 * Obtain the script code from a parsed object and write it to the specified
	 * Writer.
	 * 
	 * @param writer
	 *            the Writer to which the script code is written
	 */
	public void unparse(Writer writer) throws IOException {
		writer.write(unparse());
	}

	/**
	 * Executes the parsed script
	 * 
	 * @param context
	 *            the context in which the script is executed
	 * @return the result
	 */
	protected Object accept(Context context) {
		Object value = null;
		Thread th = Thread.currentThread();
		ClassLoader ccl = th.getContextClassLoader();
		try {
			value = context.pnutsImpl.accept(startNodes, context);
			context.onExit(value);
			return value;
		} catch (Escape esc) {
			context.onExit(value);
			Object val = esc.getValue();
			flush(context);
			context.onExit(val);
			return val;
		} catch (PnutsException pe) {
			context.onError(pe);
			throw pe;
		} catch (Throwable t) {
			PnutsException e = new PnutsException(t, context);
			context.onError(t);
			throw e;
		} finally {
		    th.setContextClassLoader(ccl);
		}
	}
}
