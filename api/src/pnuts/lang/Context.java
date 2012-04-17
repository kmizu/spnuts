/*
 * Context.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import org.pnuts.util.Cell;

/**
 * <em>Context</em> represents an internal state of a particular script
 * execution. A Context is created when start executing a script and passed
 * around during the execution.
 *
 * A pnuts.lang.Context object contains the following information.
 * <ol>
 * <li>Current Package (which Pnuts-package being used)
 * <li>Imported Java-package list
 * <li>Writer to which print() write data
 * <li>Writer to which error() write message
 * <li>ClassLoader
 * <li>Modules added with use() function.
 * <li>Units
 * <li>Environments (accessed by Context.get() and set())
 * <li>Stack frame (for the pure interpreter)
 * <li>Encoding
 * </ol>
 *
 * A clone is created when eval(), load(), or loadFile() is called in a script.
 * When a clone is created, (1) and (2) of the clone are reset to the default
 * value.
 *
 */
public class Context implements Cloneable {
    private static final boolean DEBUG = false;
    
    public final static PrintWriter defaultOutputStream = new PrintWriter(
            System.out, false);
    
    public final static PrintWriter defaultTerminalStream = defaultOutputStream;
    
    public final static PrintWriter defaultErrorStream = new PrintWriter(
            System.err, true);
    
    /**
     * internal name for undefined state.
     */
    private final static Object UNDEF = new Object[0];
    
    static String exceptionHandlerTableSymbol = Runtime.EXCEPTOIN_FIELD_SYMBOL;
    
    static String finallyFunctionSymbol = "!finally".intern();
    
    private ImportEnv defaultImports;
    
    static Configuration defaultConfig = Configuration.getDefault();
    
    static boolean defaultVerboseMode = false;
    static {
        try {
            String vb = Runtime.getProperty("pnuts.verbose");
            if (vb != null) {
                defaultVerboseMode = true;
            }
        } catch (Throwable t) {
        }
    }
    
    private static Runtime defaultRuntime = Runtime.getDefaultRuntime();
    
    private static PnutsImpl defaultPnutsImpl = PnutsImpl.getDefault();
    
    Implementation pnutsImpl = defaultPnutsImpl;

        /*
         * A flag to tell if this context is created with eval() builtin function
         */
    boolean eval = false;
    
        /*
         * parent context from which this context is created
         */
    Context parent;
    
    // eval depth
    protected int depth = 0;
    
    // caller
    
    Function frame;
    
    // the stack frame
    StackFrame stackFrame;
    Cell evalFrameStack;
    
    // the stack which tracks load/loadFile chain
    protected Cell loadingResource;
    
    // line information
    protected int beginLine = -1;
    
    // line information
    protected int endLine = -1;
    
    // column information
    protected int beginColumn = -1;
    
    // import() state
    protected ImportEnv importEnv;
    
    // list of added modules
    protected ModuleList moduleList;
    
    // the module list copied from moduleList
    ModuleList localModuleList;
    
    private Set pendingModules;
    
    // list of loaded files
    protected SymbolTable provideTable = new SymbolTable();
    
    // set of unit symbols
    protected Hashtable unitTable;
    
    // context-local variables
    protected SymbolTable environment;
    
    // script encoding
    String encoding;
    
    static SymbolTable globals = new SymbolTable();
    static {
        globals.set("pnuts_version".intern(), Pnuts.pnuts_version);
        globals.set(Runtime.INT_SYMBOL, int.class);
        globals.set(Runtime.SHORT_SYMBOL, short.class);
        globals.set(Runtime.CHAR_SYMBOL, char.class);
        globals.set(Runtime.BYTE_SYMBOL, byte.class);
        globals.set(Runtime.LONG_SYMBOL, long.class);
        globals.set(Runtime.FLOAT_SYMBOL, float.class);
        globals.set(Runtime.DOUBLE_SYMBOL, double.class);
        globals.set(Runtime.BOOLEAN_SYMBOL, boolean.class);
        globals.set(Runtime.VOID_SYMBOL, void.class);
        PnutsFunction[] builtins = PnutsFunction.primitives;
        for (int i = 0; i < builtins.length; i++) {
            globals.set(builtins[i].getName().intern(), builtins[i]);
        }
    }
    
    // streams for println()
    private PrintWriter outputWriter;
    
    private OutputStream outputStream;
    
    // stream for error reporting
    private PrintWriter errorWriter;
    
    // stream for terminal (read-eval-print)
    private PrintWriter terminalWriter;
    
    // the name of the context
    private String name;
    
    // class loader for class resolution
    ClassLoader classLoader[];

    // class loader for class definition
    ClassLoader codeLoader;
    
    boolean namespaceRefreshed[] = { false }; // shared by all clones
    
    Runtime runtime = defaultRuntime;
    
    boolean inGeneratorClosure; // for AST interpreter
    
    BinaryOperator _add; // +
    BinaryOperator _subtract; // -
    BinaryOperator _multiply; // *
    BinaryOperator _mod; // %
    BinaryOperator _divide; // /
    BinaryOperator _shiftArithmetic; // >>>
    BinaryOperator _shiftLeft; // <<
    BinaryOperator _shiftRight; // >>
    BinaryOperator _and; // &
    BinaryOperator _or; // |
    BinaryOperator _xor; // ^
    UnaryOperator _add1; // ++
    UnaryOperator _subtract1; // --
    UnaryOperator _not; // ~
    UnaryOperator _negate; // -
    BooleanOperator _eq; // ==
    BooleanOperator _lt; // <
    BooleanOperator _le; // <=
    BooleanOperator _gt; // >
    BooleanOperator _ge; // >=
    
        /*
         * Hook to be executed at the end of a script
         */
    Executable exitHook;
    
        /*
         * the current package
         */
    Package currentPackage = Package.getGlobalPackage();
    {
        setConfiguration(defaultConfig);
        currentPackage.init(this);
    }
    
    Package rootPackage = currentPackage;
    
        /*
         * configuration of this context
         */
    Configuration config;
    
    boolean verbose = defaultVerboseMode;
    
    /**
     * Create a new context
     */
    public Context() {
        this(Package.getGlobalPackage());
    }
    
    /**
     * Creates a context.
     *
     * @param pkg
     *            the name of the package.
     */
    public Context(String pkg) {
        this(Package.getPackage(pkg, null));
    }
    
    /**
     * Creates a context.
     *
     * @param pkg
     *            the initial package of the context. If null, the global
     *            package is used.
     */
    public Context(Package pkg) {
        if (pkg != null) {
            setCurrentPackage(pkg);
        }
        this.outputWriter = defaultOutputStream;
        this.outputStream = System.out;
        this.terminalWriter = defaultTerminalStream;
        this.errorWriter = defaultErrorStream;
        this.classLoader = new ClassLoader[] { config.getInitialClassLoader() };
        this.importEnv = defaultImports;
    }
    
    /**
     * Creates a context from a template
     *
     * @param context
     *            The template
     * @since 1.0beta9
     */
    public Context(Context context) {
        this.outputWriter = context.outputWriter;
        this.errorWriter = context.errorWriter;
        this.terminalWriter = context.terminalWriter;
        this.currentPackage = context.currentPackage;
        this.pnutsImpl = context.pnutsImpl;
        this.encoding = context.encoding;
        this.setConfiguration(context.config);
        
        this.classLoader = new ClassLoader[] { context.classLoader[0] };
        this.namespaceRefreshed = new boolean[] { context.namespaceRefreshed[0] };
        
        this.importEnv = (ImportEnv) context.importEnv.clone();
//        this.importEnv = (ImportEnv)defaultImports.clone();
//        this.importEnv = Runtime.getDefaultImports(this);

        if (context.moduleList != null){
	    this.moduleList = (ModuleList) context.moduleList.clone();
	}
        if (context.localModuleList != null) {
            this.localModuleList = (ModuleList) context.localModuleList.clone();
        }

        if (context.unitTable != null) {
            this.unitTable = (Hashtable) context.unitTable.clone();
        }
        if (context.environment != null) {
            this.environment = (SymbolTable) context.environment.clone();
        }
        this.provideTable = (SymbolTable) context.provideTable.clone();
    }
    
    public Context(Properties properties) {
        setConfiguration(Configuration.getDefault(properties));
        setImplementation(PnutsImpl.getDefault(properties));
    }
    
    /**
     * Make a clone of the context
     */
    public Object clone() {
        return clone(true, true);
    }
    
    /**
     * Make a clone of the context
     *
     * @param clear_attributes
     *            If true, import() state and current package are reset to the
     *            default values.
     * @param clear_locals
     *            If true, local stack is reset.
     */
    public Object clone(boolean clear_attributes, boolean clear_locals) {
        synchronized (this) {
            if (moduleList == null) {
                moduleList = new ModuleList(currentPackage);
            }
        }
        try {
            Context ret = (Context) super.clone();
	    ret.codeLoader = null;
	    ret.importEnv = (ImportEnv)defaultImports.clone();

            if (clear_locals) {
                if (stackFrame != null) {
                    ret.stackFrame = new StackFrame();
                }
                ret.parent = this;
                if (environment != null) {
                    ret.environment = new SymbolTable(environment);
                }
            }
            if (clear_attributes) {
                ret.eval = false;
                ret.frame = null;
                ret.currentPackage = rootPackage;
                ret.beginLine = -1;
                ret.beginColumn = -1;
            }
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new PnutsException(e, this);
        }
    }
    
    /**
     * Sets the name of the context
     *
     * @param name
     *            The name of the context.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the name of the context
     *
     * @return The name of the context.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Changes the PnutsImpl object associated with this context
     *
     * @param impl
     *            The PnutsImpl object, which defines the implementation of the
     *            interpreter. eval(), load(), and loadFile() of
     *            pnuts.lang.Pnuts select an implementation (pure interpreter,
     *            on-the-fly compiler, etc.), according to the context passed to
     *            the methods.
     *
     * @deprecated replaced by setImplementation()
     */
    public void setPnutsImpl(PnutsImpl impl) {
        setImplementation(impl);
    }
    
    /**
     * Gets the PnutsImpl object associated with this context
     *
     * @deprecated replaced by getImplementation()
     */
    public PnutsImpl getPnutsImpl() {
        return (PnutsImpl) getImplementation();
    }
    
    /**
     * Changes the Implementation object associated with this context
     *
     * @param impl
     *            The Implementation object, which defines the implementation of
     *            the interpreter. eval(), load(), and loadFile() of
     *            pnuts.lang.Pnuts select an implementation (pure interpreter,
     *            on-the-fly compiler, etc.), according to the context passed to
     *            the methods.
     */
    public void setImplementation(Implementation impl) {
        if (impl != null) {
            pnutsImpl = impl;
        }
    }
    
    /**
     * Gets the Implementation object associated with this context
     */
    public Implementation getImplementation() {
        return pnutsImpl;
    }
    
    /**
     * Gets an environemnt variable associated with this context.
     *
     * @param symbol
     *            the name of the variable, which must be intern'ed.
     * @return the value of the variable
     */
    public Object get(String symbol) {
        if (environment != null) {
            Value v = environment.lookup(symbol);
            if (v != null) {
                return v.get();
            }
        }
        return null;
    }
    
    /**
     * Defines an environemnt variable associated with this context
     *
     * To access those environment variables, Context.get(String) should be
     * called. Note that those variables can not be accessed just by specifying
     * their names in Pnuts interpreter.
     *
     * Since the environment varariables are bound to the executing context,
     * they are accessible from various modules that the script uses. Therefore,
     * the name of environment variables should have prefixes so that name
     * conflict is unlikely to occur. The name that starts with "pnuts." is
     * reserved.
     *
     * @param symbol
     *            the name of the variable, which must be intern'ed.
     * @param value
     *            the value of the variable
     * @since 1.0beta8
     */
    public void set(String symbol, Object value) {
        synchronized (this) {
            if (environment == null) {
                environment = new SymbolTable();
            }
        }
        environment.set(symbol, value);
    }
    
    /**
     * Returns an enumeration of the keys in the environment of this context.
     */
    public Enumeration keys() {
        synchronized (this) {
            if (environment == null) {
                environment = new SymbolTable();
            }
        }
        return environment.keys();
    }
    
    /**
     * set output stream of the context
     *
     * @deprecated replaced by setTerminalWriter(Writer, boolean)
     */
    public void setOutputStream(Object out, boolean autoFlush) {
        if (out instanceof PrintWriter || out == null) {
            this.outputWriter = (PrintWriter) out;
            this.outputStream = null;
        } else if (out instanceof OutputStream) {
            this.outputStream = (OutputStream) out;
            this.outputWriter = new PrintWriter((OutputStream) out, autoFlush);
        } else if (out instanceof Writer) {
            this.outputStream = null;
            this.outputWriter = new PrintWriter((Writer) out, autoFlush);
        } else {
            throw new IllegalArgumentException(Runtime.getMessage(
                    "pnuts.lang.pnuts", "illegal.streamType",
                    Runtime.NO_PARAM));
        }
    }
    
    /**
     * set output stream of the context
     *
     * @deprecated replaced by setTerminalWriter(Writer)
     */
    public void setOutputStream(Object outputStream) {
        setOutputStream(outputStream, false);
    }
    
    /**
     * Set the specified OutputStream as the standard output stream of the
     * context, to which write() writes data. A PrintWriter is created from the
     * specified OutputStream, which is returned by getWriter(). If null is
     * specified, both getOutputStream() and getWriter() return null.
     *
     * @param out
     *            the OutputStream
     */
    public void setOutputStream(OutputStream out) {
        this.outputStream = out;
        if (out == null) {
            this.outputWriter = null;
        } else {
            this.outputWriter = new PrintWriter(out, false);
        }
    }
    
    /**
     * Set the specified Writer as the standard writer of the context.
     * PrintWriter is created from the specified Writer if the Writer is not an
     * instance of PrintWriter. If this method has been called,
     * getOutputStream() returns null. If null is specifed to this method, both
     * getWriter() and getOutputStream() return null.
     *
     * @param out
     *            the Writer
     */
    public void setWriter(Writer out) {
        setWriter(out, false);
    }
    
    /**
     * Set the specified Writer as the standard writer of the context.
     * PrintWriter is created from the specified Writer if the Writer is not an
     * instance of PrintWriter. If this method has been called,
     * getOutputStream() returns null. If null is specifed to this method, both
     * getWriter() and getOutputStream() return null.
     *
     * @param out
     *            the Writer
     * @param autoFlush
     *            A boolean; if true, the PrintWriter.println() methods will
     *            flush the output buffer
     */
    public void setWriter(Writer out, boolean autoFlush) {
        this.outputStream = null;
        if (out instanceof PrintWriter || out == null) {
            this.outputWriter = (PrintWriter) out;
        } else {
            this.outputWriter = new PrintWriter(out, autoFlush);
        }
    }
    
    /**
     * Get the standard output stream of the context, to which write() writes
     * data. This method returns the OutputStream previously set by
     * setOutputStream(). If setWriter() has been called, getOutputStream()
     * returns null.
     *
     * @return the standard output stream of the context
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }
    
    /**
     * Get the standard writer of the context, to which print()/println() write
     * messages.
     *
     * @return the standard writer of the context
     */
    public PrintWriter getWriter() {
        return outputWriter;
    }
    
    /**
     * Set the terminal stream of the context, in which the prompt is shown.
     *
     * @deprecated replaced by setTerminalWriter(Writer, boolean)
     */
    public void setTerminalStream(Object str, boolean autoFlush) {
        if (str == null) {
            this.terminalWriter = null;
        } else if (str instanceof OutputStream) {
            this.terminalWriter = new PrintWriter((OutputStream) str, autoFlush);
        } else if (str instanceof Writer) {
            this.terminalWriter = new PrintWriter((Writer) str, autoFlush);
        } else {
            throw new IllegalArgumentException(Runtime.getMessage(
                    "pnuts.lang.pnuts", "illegal.streamType",
                    Runtime.NO_PARAM));
        }
    }
    
    /**
     * Set the terminal stream of the context
     *
     * @deprecated replaced by setTerminalWriter(Writer)
     */
    public void setTerminalStream(Object stream) {
        if (stream == null) {
            this.terminalWriter = null;
        } else if (stream instanceof PrintWriter) {
            this.terminalWriter = (PrintWriter) stream;
        } else if (stream instanceof Writer) {
            this.terminalWriter = new PrintWriter((Writer) stream, true);
        } else if (stream instanceof OutputStream) {
            this.terminalWriter = new PrintWriter((OutputStream) stream, true);
        } else {
            throw new IllegalArgumentException(String.valueOf(stream));
        }
    }
    
    /**
     * Set the terminal writer of the context
     *
     * @param w
     *            the Writer
     */
    public void setTerminalWriter(Writer w) {
        if (w instanceof PrintWriter || w == null) {
            this.terminalWriter = (PrintWriter) w;
        } else {
            this.terminalWriter = new PrintWriter(w);
        }
    }
    
    /**
     * Set the terminal writer of the context
     *
     * @param w
     *            the Writer
     */
    public void setTerminalWriter(Writer w, boolean autoFlush) {
        if (w == null) {
            this.terminalWriter = null;
        } else {
            this.terminalWriter = new PrintWriter(w, autoFlush);
        }
    }
    
    /**
     * get terminal-output-stream of the context
     *
     * @deprecated replaced by getTerminalWriter(Writer)
     */
    public PrintWriter getTerminalStream() {
        return terminalWriter;
    }
    
    /**
     * get terminal-output-stream of the context
     */
    public PrintWriter getTerminalWriter() {
        return terminalWriter;
    }
    
    /**
     * Set an OutputStream or a Writer to which error() write messages If
     * errorStream is null, exception is thrown out of eval loop.
     *
     * @deprecated replaced by setErrorWriter(Writer, boolean)
     */
    public void setErrorStream(Object errorStream, boolean autoFlush) {
        if (errorStream == null) {
            this.errorWriter = null;
        } else if (errorStream instanceof OutputStream) {
            this.errorWriter = new PrintWriter((OutputStream) errorStream,
                    autoFlush);
        } else if (errorStream instanceof Writer) {
            this.errorWriter = new PrintWriter((Writer) errorStream, autoFlush);
        } else {
            throw new IllegalArgumentException(Runtime.getMessage(
                    "pnuts.lang.pnuts", "illegal.streamType",
                    Runtime.NO_PARAM));
        }
    }
    
    /**
     * Set ar PrintWriter to which error() write messages
     *
     * @deprecated replaced by setErrorWriter(Writer)
     */
    public void setErrorStream(Object errorStream) {
        if (errorStream == null) {
            this.errorWriter = null;
        } else if (errorStream instanceof OutputStream) {
            this.errorWriter = new PrintWriter((OutputStream) errorStream,
                    false);
        } else if (errorStream instanceof PrintWriter) {
            this.errorWriter = (PrintWriter) errorStream;
        } else if (errorStream instanceof Writer) {
            this.errorWriter = new PrintWriter((Writer) errorStream, false);
        } else {
            throw new IllegalArgumentException(String.valueOf(errorStream));
        }
    }
    
    /**
     *
     */
    public void setErrorWriter(Writer w, boolean autoFlush) {
        if (w instanceof PrintWriter || w == null) {
            this.errorWriter = (PrintWriter) w;
        } else {
            this.errorWriter = new PrintWriter(w, autoFlush);
        }
    }
    
    public void setErrorWriter(Writer w) {
        if (w instanceof PrintWriter || w == null) {
            this.errorWriter = (PrintWriter) w;
        } else {
            setErrorWriter(w, false);
        }
    }
    
    /**
     * Get an OutputStream or a Writer to which error() write messages
     *
     * @deprecated replaced by getErrorWriter
     */
    public PrintWriter getErrorStream() {
        return errorWriter;
    }
    
    /**
     * Get an PrintWriter to which error() write messages
     */
    public PrintWriter getErrorWriter() {
        return errorWriter;
    }
    
    /**
     * get the current package
     */
    public Package getCurrentPackage() {
        return currentPackage;
    }
    
    /**
     * set the current package
     */
    public void setCurrentPackage(Package pkg) {
        pkg.init(this);
        this.currentPackage = pkg;
        Package root = pkg.root;
        if (root != null) {
            this.rootPackage = root;
        }
    }
    
    /**
     * Changes the current class loader for this context.
     *
     * The initial value is set to
     * Thread.currentThread().getContextClassLoader() when the instance is
     * created.
     *
     * @param loader
     *            the class loader
     */
    public void setClassLoader(ClassLoader loader) {
        classLoader[0] = loader;
        synchronized (namespaceRefreshed) {
            namespaceRefreshed[0] = true;
        }
    }

    /**
     * Gets the current class loader.
     *
     * The initial value is set to
     * Thread.currentThread().getContextClassLoader() when the instance is
     * created.
     *
     * @return the class loader
     */
    public ClassLoader getClassLoader() {
        return classLoader[0];
    }
    
    /**
     * Gets the current class loader for class geneartion
     *
     * The initial value is null.  Expressions that generate class
     * create a classloader to load generated classes based on the current
     * class loader.
     *
     * @return the class loader
     */
    public ClassLoader getCodeLoader() {
	return this.codeLoader;
    }

    /**
     * Sets the current class loader for class geneartion
     *
     * Expressions that generate class create a classloader to load generated
     * classes based on the current class loader.
     *
     * @param loader the class loader
     */
    public void setCodeLoader(ClassLoader loader) {
	this.codeLoader = loader;
    }
    
    /**
     * Changes the configuration for this context.
     *
     * @param config
     *            the configuration
     */
    public void setConfiguration(Configuration config) {
        if (config == null){
            return;
        }
        this.config = config;
        this.defaultImports = Runtime.getDefaultImports(this);
        if (this.importEnv == null){
            this.importEnv = this.defaultImports;
        }
        
        // copy operators
        this._add = config._add;
        this._add1 = config._add1;
        this._subtract = config._subtract;
        this._subtract1 = config._subtract1;
        this._multiply = config._multiply;
        this._mod = config._mod;
        this._divide = config._divide;
        this._shiftArithmetic = config._shiftArithmetic;
        this._shiftLeft = config._shiftLeft;
        this._shiftRight = config._shiftRight;
        this._and = config._and;
        this._or = config._or;
        this._xor = config._xor;
        this._not = config._not;
        this._negate = config._negate;
        this._eq = config._eq;
        this._lt = config._lt;
        this._le = config._le;
        this._gt = config._gt;
        this._ge = config._ge;
    }
    
        /*
         * Gets the current configuration
         */
    public Configuration getConfiguration() {
        return config;
    }
    
    /**
     * Sets a hook to be executed at the end of a script. The default value is
     * null.
     *
     * @param hook
     *            the hook
     */
    public void setExitHook(Executable hook) {
        exitHook = hook;
    }
    
    /**
     * Gets the hook to be executed at the end of a script
     *
     */
    public Executable getExitHook() {
        return exitHook;
    }
    
    void addClassToImport(String className) {
        synchronized (this) {
            if (importEnv == defaultImports) {
                importEnv = (ImportEnv) importEnv.clone();
            }
        }
        importEnv.addClass(className);
    }
    
    void addPackageToImport(String pkgName) {
        synchronized (this) {
            if (importEnv == defaultImports) {
                importEnv = (ImportEnv) importEnv.clone();
            }
        }
        importEnv.addPackage(pkgName);
    }
    
    void addStaticMembers(String name, boolean wildcard) {
        synchronized (this) {
            if (importEnv == defaultImports) {
                importEnv = (ImportEnv) importEnv.clone();
            }
        }
        importEnv.addStaticMembers(name, wildcard, this);
    }
    
        /*
         * Declares explicitly that the context is reading from a script source
         * (usually a URL). Must be followed by popFile() call.
         */
    synchronized void pushFile(Object file) {
        Cell cell = new Cell();
        cell.object = file;
        cell.next = loadingResource;
        loadingResource = cell;
    }
    
        /*
         * Declares that the current script source is no longer used.
         */
    synchronized void popFile() {
        if (exitHook != null) {
            exitHook.run(this);
        }
        if (loadingResource != null) {
            loadingResource = loadingResource.next;
        }
    }
    
    public synchronized boolean unusePackage(Package pkg) {
        if (moduleList != null) {
            if (moduleList.remove(pkg)) {
		localModuleList = null;
                return true;
            }
        }
        return false;
    }
    
    public boolean usePackage(Package pkg, boolean checkException) {
        synchronized (this) {
            if (moduleList == null) {
                moduleList = new ModuleList(currentPackage);
            }
            if (pendingModules == null){
                pendingModules = new HashSet();
            }
        }
        boolean pending = pendingModules.contains(pkg);
        try {
            if (!pending && !moduleList.contains(pkg)) {
                if (pkg.usedAsModule) {
                    Context ctx = (Context) clone();
                    ctx.setCurrentPackage(pkg);
                    for (Enumeration e = pkg.providedModuleNames.elements();
                    e.hasMoreElements();) {
                        String m = (String) e.nextElement();
                        if (DEBUG) {
                            System.out.println("! " + pkg.getName()
                            + " provides " + m);
                        }
                        ctx.usePackage(m);
                    }
                    
                    if (pkg.requiredModuleNames.size() > 0) {
                        ctx = (Context) ctx.clone();
                        ctx.clearPackages();
                    }
                    
                    for (Enumeration e = pkg.requiredModuleNames.elements(); e
                            .hasMoreElements();) {
                        String m = (String) e.nextElement();
                        if (DEBUG) {
                            System.out.println(pkg.getName() + " requires " + m);
                        }
                        ctx.usePackage(m);
                    }
                } else {
                    pkg.initializeModule();
                }
                String name = pkg.getName();
                synchronized (pkg.moduleIntializationLock) {
                    pendingModules.add(pkg);
                    try {
                        if (!pkg.initialized) {
                            if (name != null) {
                                loadModule(name, pkg);
                            }
                            pkg.initialized = true;
                        }
                    } finally {
                        pendingModules.remove(pkg);
                    }
                    if (name != null && moduleList != null) {
                        if (currentPackage.usedAsModule) {
                            if (moduleList.basePackage != currentPackage) {
                                if (DEBUG) {
                                    System.out.println(currentPackage.getName()
                                    + " provides... " + name);
                                }
                                currentPackage.providedModuleNames
                                        .addElement(name);
                            } else {
                                currentPackage.requiredModuleNames
                                        .addElement(name);
                            }
                        }
                    }
                }
            }
            if (!pending){
                moduleList.add(pkg);
            }
	    localModuleList = null;
            return true;
        } catch (Throwable t) {
            if (checkException) {
                Runtime.checkException(this, t);
            } else {
                if (verbose && terminalWriter != null) {
                    t.printStackTrace(terminalWriter);
                }
            }
        }
        return false;
    }
    
    /**
     * Add a package to the use()'d package list.
     *
     * @param name
     *            the package name
     * @return true if successfully use()'d.
     */
    public boolean usePackage(String name) {
        return usePackage(name, false);
    }
    
    /**
     * Add a package to the use()'d package list.
     *
     * @param name
     *            the package name
     * @param checkException
     *            if false exceptions are ignored
     * @return true if successfully use()'d.
     */
    public boolean usePackage(String name, boolean checkException) {
        return usePackage(Package.getPackage(name, this), checkException);
    }
    
    /**
     * Loads a module is it has not been loaded yet.
     *
     * The initialization script is: 1) Replace :: and . with / then append
     * "/init", e.g. pnuts.lib => pnuts/lib/init 2) The 1st line in
     * META-INF/pnuts/module/ <module_name>
     *
     * @param name
     *            the name of the module
     * @param pkg
     *            the associated package (name space)
     * @exception FileNotFoundException
     *                thrown when the initialization script is not found.
     */
    protected void loadModule(String name, Package pkg) throws IOException {
        if (DEBUG) {
            System.out.println("loading " + name);
        }
        Context ctx = (Context) clone();
        
        ctx.setCurrentPackage(pkg);
        ctx.config = defaultConfig;
        
        FileNotFoundException notfound = null;
        try {
            Pnuts.load(pkg.getInitScript(), ctx);
        } catch (FileNotFoundException e) {
            notfound = e;
        }
        if (notfound != null) {
            URL url = Pnuts.getResource("META-INF/pnuts/module/" + name, this);
            if (url != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        url.openStream(), "UTF-8"));
                try {
                    String line = br.readLine();
                    if (line != null) {
                        try {
                            Pnuts.load(line.trim(), ctx);
                        } catch (FileNotFoundException e2) {
                            throw e2;
                        }
                        notfound = null;
                    }
                } finally {
                    br.close();
                }
            }
        }
        if (notfound != null) {
            throw notfound;
        }
        
        if (!pkg.exports) {
            pkg.exportFunctions();
        }
    }
    
    /**
     * Unregisteres all use()'d packages
     */
    public synchronized void clearPackages() {
        moduleList = new ModuleList(currentPackage);
        localModuleList = null;
    }

    synchronized ModuleList localModuleList() {
        ModuleList list = this.localModuleList;
        if (list == null && this.moduleList != null) {
            list = this.localModuleList = (ModuleList) this.moduleList.clone();
        }
        return list;
    }

    /**
     * Returns the list of use()'d packages
     */
    public String[] usedPackages() {
        if (moduleList == null) {
            return new String[] {};
        } else {
            return moduleList.getPackageNames();
        }
    }
    
    /**
     * Loads a script file only if it has not been loaded. It is guaranteed that
     * the script runs at most once in this context.
     *
     * @param file
     *            the script file, which must be an intern'ed String.
     */
    void require(String file, boolean checkForUpdate)
        throws FileNotFoundException
    {
        SymbolTable table = provideTable;
        Binding b;
        synchronized (table) {
            b = table.lookup0(file);
            if (b == null) {
                table.set(file, null);
                b = table.lookup0(file);
            }
        }
        synchronized (b) {
            Long timestamp = (Long)b.value;
            if (timestamp == null) {
                Pnuts.load(file, this);
            } else if (checkForUpdate){
                long m = lastModified(file);
                if (m > timestamp.longValue()){
                    Pnuts.load(file, this);
                }
            }
        }
    }
    
    long lastModified(String file){
        try {
           URL url = Runtime.getScriptURL(file + ".pnut", this);
            if (url == null){
                return -1;
            }
            URLConnection c = url.openConnection();
            return c.getLastModified();
        } catch (IOException e){
            return -1;
        }
    }
    
    void provide(String file) {
        file = file.intern();
        Binding b;
        SymbolTable table = provideTable;
        synchronized (table) {
            b = table.lookup0(file);
            if (b == null) {
                table.set(file, null);
                b = table.lookup0(file);
            }
        }
        synchronized (b) {
            b.set(new Long(System.currentTimeMillis()));
        }
    }
    
    void revoke(String file) {
        file = file.intern();
        provideTable.removeBinding(file);
    }
    
    
    /**
     * Get the source of the script.
     *
     * @return
     *
     * <pre>
     *
     *  java.net.URL object, when the script is not precompiled
     *  pnuts.lang.Runtime object, when the script is precompiled
     *
     * </pre>
     */
    protected Object getScriptSource() {
        if (frame != null) {
            return frame.file;
        } else {
            Cell c = loadingResource;
            if (c != null) {
                return c.object;
            } else {
                return null;
            }
        }
    }
    
    void updateLine(SimpleNode node) {
        updateLine(node, node.beginLine, node.beginColumn);
    }
    
    /**
     * AST interpreter calls this method when line number changes, giving AST nodes
     * and line information
     *
     * Not that compiler does not call this method.
     *
     * @param node the current AST node
     * @param beginLine the line number at which the current expression starts.
     * @param beginColumn the column number at which theh current expression ends.
     */
    protected void updateLine(SimpleNode node, int beginLine, int beginColumn) {
        updateLine(beginLine);
        updateColumn(beginColumn);
    }
    
    /**
     * Both AST interpreter and compiler call this method when line number changes.
     *
     * Subclasses may override this method to interact with
     * running script. For example, a subclass may redefine this
     * method so that it can stop the execution if Thread.interrupt()
     * has been called.
     *
     * @param line the line number
     */
    protected void updateLine(int line){
        if (line > 0) {
            this.beginLine = line;
            this.endLine = line;
            this.beginColumn = -1;
        }
    }
    
    protected void updateColumn(int column){
        if (column > 0){
            this.beginColumn = column;
        }
    }
    
    /**
     * This method is called when the excecution is terminated normally.
     */
    protected void onExit(Object arg) {
    }
    
    /**
     * This method is called when an exception is thrown.
     */
    protected void onError(Throwable t) {
    }
    
    /**
     * Gets the value of a symbol.
     *
     * @param interned
     *            a symbol (interned string)
     * @return the value of the symbol
     * @exception PnutsException
     *                if the specified symbol is not defined
     */
    public Object getId(String interned) {
        Object v = _getId(interned);
        if (v == UNDEF) {
            return undefined(interned);
        } else {
            return v;
        }
    }
    
    /**
     * Resolves the value of a symbol in the following order:
     *
     * (1) current package (2) builtin functions, primitive types, pnuts_version
     * (3) module exports (4) imported classes (5) parent packages
     *
     * @param interned
     *            a symbol (interned string)
     * @return the value of the symbol, or null if it is not defined.
     */
    public Object resolveSymbol(String interned) {
        Object v = _getId(interned);
        if (v == UNDEF) {
            return null;
        } else {
            return v;
        }
    }

    static Map primitiveTypes = new HashMap();
    static {
	primitiveTypes.put("int", int.class);
	primitiveTypes.put("short", short.class);
	primitiveTypes.put("long", long.class);
	primitiveTypes.put("byte", byte.class);
	primitiveTypes.put("char", char.class);
	primitiveTypes.put("long", long.class);
	primitiveTypes.put("boolean", boolean.class);
	primitiveTypes.put("float", float.class);
	primitiveTypes.put("double", double.class);
    }
    
        /*
         * Resolves a class using import()'ed names
         *
         * @param symbol an interned String
         * @return a Class object that represents
         * the class, or null if not found.
         */
    public Class resolveClass(String symbol) {
	Class type = (Class)primitiveTypes.get(symbol);
	if (type != null){
	    return type;
	}

	try {
	    return Pnuts.loadClass(symbol, this);
	} catch (ClassNotFoundException e) {
	    if (symbol.indexOf('.') > 0) {
		return null;
            }
        }
	NamedValue binding = currentPackage.lookup(symbol);
	if (binding != null){
	    Object value = binding.get();
	    if (value instanceof Class){
		return (Class)value;
	    }
	}
	Object obj = importEnv.get(symbol, this);
	if (obj instanceof Class){
	    return (Class)obj;
	} else {
	    obj = _getId(symbol);
	    if (obj instanceof Class){
		return (Class)obj;
	    }
	    return null;
        }
    }
    
    Object _getId(String symbol) {
        Value v = currentPackage.lookup(symbol, this);
        if (v != null) {
	    if (DEBUG){
		System.out.println("from package");
	    }
            return v.get();
        }
        v = globals.lookup0(symbol);
        if (v != null) {
	    if (DEBUG){
		System.out.println("from global package");
	    }
            return v.get();
        }
        if (moduleList != null) {
            v = moduleList.resolve(symbol, this);
            if (v != null) {
		if (DEBUG){
		    System.out.println("from moduleList");
		}
                return v.get();
            }
        }
        
        if (symbol.charAt(0) == '!') {
            return null;
        }
        
        synchronized (namespaceRefreshed) {
            if (namespaceRefreshed[0]) {
                resetImportEnv();
            }
            namespaceRefreshed[0] = false;
        }
        
        Object c = importEnv.get(symbol, this);
        if (c != null) {
	    if (DEBUG){
		System.out.println("from importEnv " + importEnv);
	    }
            return c;
        }
        
        Package parent = currentPackage.getParent();
        if (parent != null) {
            v = parent.lookupRecursively(symbol, this);
        }
        if (v != null) {
	    if (DEBUG){
		System.out.println("from currentPackage");
	    }
            return v.get();
        }
        
        return UNDEF;
    }
    
    public void resetImportEnv() {
        importEnv.reset();
        if (parent != null) {
            parent.resetImportEnv();
        }
    }
    
    /**
     * Registers an autoload script for the <em>name</em>. If <em>name</em>
     * is not defined when accessed, the registerred <em>file</em> is loaded.
     *
     * @param name
     *            variable name
     * @param file
     *            the file
     */
    public void autoload(String name, String file) {
        currentPackage.autoload(name, file, this);
    }
    
    /**
     * Registers an AutoloadHook for the <em>name</em> in the current package.
     *
     * @param name
     *            variable name
     * @param hook
     *            the AutoloadHook
     */
    public void autoload(String name, AutoloadHook hook) {
        currentPackage.autoload(name, hook);
    }
    
    Object undefined(String sym) {
        return config.handleUndefinedSymbol(sym, this);
    }
    
    /**
     * Checks if the name is defined in the context.
     */
    public boolean defined(String name) {
        return _getId(name.intern()) != UNDEF;
    }
    
    /**
     * Defines a unit.
     *
     * @param unit
     *            The unit symbol
     * @param fac
     *            A QuantityFactory object which defines what kind of object is
     *            created when a decimal number with this unit symbol is
     *            evaluated.
     */
    public void registerQuantityFactory(String unit, QuantityFactory fac) {
        synchronized (this) {
            if (unitTable == null) {
                unitTable = new Hashtable(8);
            }
        }
        if (fac != null) {
            unitTable.put(unit, fac);
        } else {
            unitTable.remove(unit);
        }
    }
    
    ////// Methods below are used by the pure interpreter
    
        /*
         * open new scope with the pure interpreter
         */
    protected void open(Function f, Object args[]) {
        String[] locals = f.locals;
        stackFrame = new StackFrame(locals, stackFrame);
        StackFrame sf = stackFrame;
        for (int i = 0; i < args.length; i++) {
            sf.bind(locals[i], args[i]);
        }
        if (f.outer != null && f.name != null) {
            sf.bind(f.name, f.function);
        }
    }
    
        /*
         * close the scope with the pure interpreter
         */
    protected void close(Function func, Object args[]) {
        stackFrame = stackFrame.parent;
    }
    
        /*
         * open for/foreach scope with the pure interpreter
         */
    void openLocal(String locals[]) {
        stackFrame.openLocal(locals);
    }
    
        /*
         * close for/foreach scope with the pure interpreter
         */
    void closeLocal() {
        stackFrame.closeLocal();
    }
    
    final void bind(String symbol, Object obj) {
        stackFrame.bind(symbol, obj);
    }
    
    void resetStackFrame() {
        if (stackFrame != null) {
            stackFrame = new StackFrame();
        }
    }
    
    
    protected Object getValue(String symbol) {
        Object val;
        
        if (stackFrame != null) {
            Binding b = (Binding) stackFrame.lookup(symbol);
            if (b != null) {
                return b.value;
            }
            Function ff = frame;
            while (ff != null) {
                SymbolTable ls = ff.lexicalScope;
                if (ls != null) {
                    Binding bb = ls.lookup0(symbol);
                    if (bb != null) {
                        return ((Binding) bb.value).value;
                    }
                }
                ff = ff.outer;
            }
        }
        val = _getId(symbol);
        if (val == UNDEF) {
            return undefined(symbol);
        } else {
            return val;
        }
    }
    
    protected void setValue(String symbol, Object obj) {
        Binding b = (Binding) stackFrame.lookup(symbol);
        if (b != null) {
            b.value = obj;
            return;
        }
        
        Function ff = frame;
        
        while (ff != null) {
            SymbolTable ls = ff.lexicalScope;
            if (ls != null) {
                Binding bb = ls.lookup0(symbol);
                if (bb != null) {
                    ((Binding) bb.value).value = obj;
                    return;
                }
            }
            ff = ff.outer;
        }
        
        if (stackFrame.parent != null) {
            stackFrame.declare(symbol, obj);
        } else {
            currentPackage.set(symbol, obj, this);
        }
    }
    
    void catchException(Class t, PnutsFunction f) {
        if (stackFrame.parent != null) {
            Runtime.TypeMap tmap = null;
            Value b = stackFrame.lookup(exceptionHandlerTableSymbol);
            if (b != null) {
                tmap = (Runtime.TypeMap) b.get();
            }
            Runtime.TypeMap newtmap = new Runtime.TypeMap(t, f, tmap);
            stackFrame.declare(exceptionHandlerTableSymbol, newtmap);
        } else {
            Runtime.catchException(t, f, this);
        }
    }
    
    void setFinallyFunction(final PnutsFunction func) {
        if (stackFrame.parent != null) {
            stackFrame.declare(finallyFunctionSymbol, func);
            if (frame != null) {
                frame.finallySet = true;
            }
        } else {
            Runtime.setExitHook(this, func);
        }
    }
    
    /**
     * Sets the verbose mode
     */
    public void setVerbose(boolean b) {
        verbose = b;
    }
    
    /**
     * Check the current verbose mode
     *
     * @return the current verbose mode
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * Changes the script encoding for the context
     *
     * @param encoding
     *            the encoding
     */
    public void setScriptEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    /**
     * Gets the current script encoding
     *
     * @return the current script encoding
     */
    public String getScriptEncoding() {
        return this.encoding;
    }
}
