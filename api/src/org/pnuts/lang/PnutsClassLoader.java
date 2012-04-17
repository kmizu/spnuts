/*
 * PnutsClassLoader.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.lang.reflect.*;
import pnuts.lang.Pnuts;
import pnuts.lang.Context;
import pnuts.lang.SimpleNode;
import pnuts.lang.Runtime;
import pnuts.lang.ParseException;
import pnuts.lang.PnutsParser;
import pnuts.lang.ParseEnvironment;
import pnuts.compiler.Compiler;
import pnuts.compiler.ClassFile;

/**
 * A ClassLoader to compile and load classes written in Pnuts
 */
public class PnutsClassLoader extends ClassLoader {
    private final static boolean DEBUG = false;
    private final static String DEFAULT_PREFIX = "";
    private final static String DEFAULT_SUFFIX = ".pnc";

    private String prefix = DEFAULT_PREFIX;
    private String suffix = DEFAULT_SUFFIX;
    private String encoding;
    private Context context;
    private Map table = new HashMap();

    /**
     * Constructor
     *
     * @param context the context in which the scripts are executed
     */
    public PnutsClassLoader(Context context){
	this.context = context;
    }

    /**
     * Constructor
     *
     * @param parent the parent class loader
     * @param context the context in which the scripts are executed
     */
    public PnutsClassLoader(ClassLoader parent, Context context){
	super(parent);
	this.context = context;
    }

    /**
     * Get the file name extension
     *
     * @return the file name extension (the default=.pnc)
     */
    public String getSuffix(){
	return this.suffix;
    }

    /**
     * Set the file name extension
     *
     * @param suffix the file name extension (the default=.pnc)
     */
    public void setSuffix(String suffix){
	this.suffix = suffix;
    }


    /**
     * Get the prefix
     *
     * @return prefix the file name extension
     */
    public String getPrefix(){
	return this.prefix;
    }

    /**
     * Set the prefix
     *
     * @param prefix the file name extension (the default=/)
     */
    public void setPrefix(String prefix){
	this.prefix = prefix;
    }

    /**
     * Get the resource name for the specified class name
     */
    protected String getScriptResourceName(String className){
	int idx = className.lastIndexOf('.');
	if (idx < 0){
	    return prefix + className + suffix;
	} else {
	    return prefix + className.replace('.', '/') + suffix;
	}
    }

    /**
     * Get the character encoding of the parser
     */
    public String getEncoding(){
	return this.encoding;
    }

    /**
     * Set the character encoding of the parser
     *
     * @param enc the character encoding
     */
    public void setEncoding(String enc){
	this.encoding = enc;
    }

    protected SimpleNode parse(InputStream in, URL url) throws IOException, ParseException {
	Reader reader;
	if (encoding != null){
	    reader = new BufferedReader(new InputStreamReader(in, encoding));
	} else {
	    reader = new BufferedReader(new InputStreamReader(in));
	}
	return parse(reader, url);
    }

    /**
     * Parse the specified script
     */
    public static SimpleNode parse(Reader reader, URL url)
	throws IOException, ParseException
    {
	ParseEnvironment env = DefaultParseEnv.getInstance(url);
	return new PnutsParser(reader).ClassScript(env);
    }

    private static void dump(String name, byte[] code) {
	try {
	    FileOutputStream fout = new FileOutputStream("c:/tmp/" + name + ".class");
	    fout.write(code);
	    fout.close();
	} catch (IOException e){
	    e.printStackTrace();
	}
    }

    void attachContext(Class cls){
	try{
	    Method m = cls.getMethod("attach", new Class[]{Context.class});
	    m.invoke(null, new Object[]{new Context(context)});
	} catch (Exception e){
	    e.printStackTrace();
	}	
    }

    /**
     * Compile a script
     *
     * @param className the class name
     * @param url the location of the script
     */
    protected List compile(String className, URL url)
	throws IOException, ParseException
    {
	InputStream in = url.openStream();
	if (in != null){
	    try {
		if (context.isVerbose()){
		    PrintWriter writer = context.getErrorWriter();
		    writer.println("compiling " + url);
		}
		return compile(className, parse(in, url), url);
	    } finally {
		try {
		    in.close();
		} catch (IOException e){
		    /* skip */
		}
	    }
	}
	return null;
    }

    /**
     * Compile a script
     *
     * @param className the class name
     * @param node the syntax tree to be compiled
     * @param scriptSource the location of the script
     */
    protected List compile(String className, SimpleNode node, Object scriptSource)
	throws IOException
    {
	List/*<ClassFile>*/ helperClassFiles = new ArrayList();
	ClassFile classFile =
	    new Compiler(className, false, true).compileClassScript(node, scriptSource, helperClassFiles);

	List classes = new ArrayList();
	for (int i = 0, n = helperClassFiles.size(); i < n; i++){
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    ClassFile cf = (ClassFile)helperClassFiles.get(i);
	    cf.write(bout);
	    byte[] bytecode = bout.toByteArray();
	    try {
		Class cls = defineClass(cf.getClassName(), bytecode, 0, bytecode.length);
		classes.add(cls);
	    } catch (Throwable e){
		if (DEBUG){
		    e.printStackTrace();
		}
		return null;
	    }
	}

	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	classFile.write(bout);
	byte[] bytecode = bout.toByteArray();
	try {
	    Class cls = defineClass(className, bytecode, 0, bytecode.length);
	    if (context != null && context != Runtime.getThreadContext()){
		attachContext(cls);
	    }
	    table.put(className, cls);
	    classes.add(cls);
	} catch (Throwable e){
//	    if (DEBUG){
		e.printStackTrace();
//	    }
	    return null;
	} finally {
	    if (DEBUG){
		dump(className, bytecode);
		System.out.println("classes are " + classes);
	    }
	}

	return classes;
    }

    /**
     * Get the protection domain
     */
    protected ProtectionDomain getProtectionDomain(){
	return null;
    }

    /**
     * Specifies how to handle parse exceptions
     */
    protected void handleParseException(ParseException e){
	e.printStackTrace();
    }

    protected Class findClass(String className) throws ClassNotFoundException {
	Class cls = (Class)table.get(className);
	if (cls != null){
	    return cls;
	}
	try {
	    return super.findClass(className);
	} catch (ClassNotFoundException cfe){
	    // skip
	}
	String resource = getScriptResourceName(className);
	URL url = getResource(resource);
	if (url != null){
	    try {
		List classes = compile(className, url);
		if (classes != null && classes.size() > 0){
		    return (Class)classes.get(classes.size() - 1);
		}
	    } catch (IOException e1){
		return super.findClass(className);
	    } catch (ParseException e2){
		handleParseException(e2);
		return super.findClass(className);
	    }
	}
	return super.findClass(className);
    }
}
