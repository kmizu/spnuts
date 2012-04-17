/*
 * PnutsImpl.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * This class defines an abstract interface of script interpreter's
 * implementation, It also gives the default implementation, which is a pure
 * interpreter.
 *
 * @see pnuts.lang.Context#setImplementation(Implementation)
 * @see pnuts.lang.Context#getImplementation()
 */
public class PnutsImpl extends Runtime implements Implementation {
    
    static PnutsImpl defaultPnutsImpl = _getInstance();
    protected Properties properties = new Properties();
    
    static PnutsImpl _getInstance(){
        return _getInstance(Runtime.getProperty("pnuts.lang.defaultPnutsImpl"));
    }
    
    static PnutsImpl _getInstance(String className){
        return _getInstance(className, Pnuts.defaultSettings);
    }
    
    static PnutsImpl _getInstance(String className, Properties properties){    
        PnutsImpl impl = null;
        if (className != null){
            try {
                Class cls = Class.forName(className);
                impl = (PnutsImpl) cls.newInstance();
            } catch (Exception e){
            }
        }
        if (impl == null){
            impl = new PnutsImpl();
        }
        if (properties != null){
            impl.setProperties(properties);
        }
        return impl;
    }
    /**
     * Returns the default PnutsImpl object
     *
     * @return the default PnutsImpl object
     */
    public static PnutsImpl getDefault() {
        return defaultPnutsImpl;
    }
    
    static PnutsImpl getDefault(Properties properties){
        try {
            return _getInstance(properties.getProperty("pnuts.lang.defaultPnutsImpl"), properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PnutsImpl();
    }
    
    public void setProperties(Properties properties){
        for (Iterator it = properties.entrySet().iterator(); it.hasNext();){
            Map.Entry entry = (Map.Entry)it.next();
            setProperty((String)entry.getKey(), (String)entry.getValue());
        }
    }
    
    public void setProperty(String key, String value){
        this.properties.setProperty(key, value);
    }
    
    public String queryProperty(String key){
        return this.properties.getProperty(key);
    }
    
    /**
     * Evaluate an expreesion
     *
     * @param expr
     *            the expression to be evaluated
     * @param context
     *            the context in which the expression is evaluated
     * @return the result of the evaluation
     */
    public Object eval(String expr, Context context) {
	    try {
		return Pnuts.parse(expr).accept(PnutsInterpreter.getInstance(), context);
	    } catch (ParseException e) {
		context.beginLine = e.getErrorLine();
		checkException(context, e);
		return null;
	    } finally {
		context.eval = false;
	    }
    }
    
    /**
     * Load a script file from local file system
     *
     * @param filename
     *            the file name of the script
     * @param context
     *            the context in which the expression is evaluated
     * @return the result of the evaluation
     */
    public Object loadFile(String filename, Context context)
    throws FileNotFoundException {
        URL scriptURL = null;
        try {
            File f = new File(filename);
            if (!f.exists()) {
                throw new FileNotFoundException(filename);
            }
            scriptURL = Runtime.fileToURL(f);
        } catch (IOException e1) {
            throw new FileNotFoundException(filename);
        }
        return load(scriptURL, context);
    }
    
    /**
     * Load a script file using classloader
     *
     * @param file
     *            the name of the script
     * @param context
     *            the context in which the script is executed
     * @return the result of the evaluation
     */
    public Object load(String file, Context context)
    throws FileNotFoundException {
        URL url = Runtime.getScriptURL(file, context);
        if (url == null) {
            throw new FileNotFoundException(file);
        }
        provide(file, context);
        boolean completed = false;
        try {
            Object result = load(url, context);
            completed = true;
            return result;
        } finally {
            if (!completed){
                revoke(file, context);
            }
        }
    }
    
    /**
     * Load a script file from a URL
     *
     * @param scriptURL
     *            the URL of the script
     * @param context
     *            the context in which the script is executed
     * @return the result of the evaluation
     */
    public Object load(URL scriptURL, Context context) {
        InputStream in = null;
        Object value = null;
        int depth = Pnuts.enter(context);
        Throwable error = null;
        try {
            pushFile(scriptURL, context);
            in = scriptURL.openStream();
            value = accept(
                    Pnuts.parse(Runtime.getScriptReader(in, context)).startNodes,
                    context);
            context.onExit(value);
            return value;
        } catch (ParseException e) {
            context.beginLine = e.getErrorLine();
            error = e;
            checkException(context, e);
            error = null;
            return null;
        } catch (Jump jump) {
            context.onExit(value);
            return jump.getValue();
        } catch (Escape esc) {
            context.onExit(value);
            if (context.depth > 1) {
                throw esc;
            }
            return esc.getValue();
        } catch (Throwable e) {
            error = e;
            checkException(context, e);
            error = null;
            return null;
        } finally {
            if (error != null) {
                context.onError(error);
            }
            popFile(context);
            context.depth = depth;
            if (in != null) {
                try {
                    in.close();
                } catch (IOException io) {
                }
            }
        }
    }

    protected Object acceptNode(SimpleNode node, Context context) {
	return node.accept(PnutsInterpreter.getInstance(), context);
    }
    
    public Object accept(SimpleNode node, Context context) {
        Context old = getThreadContext();
	if (context != old){
	    setThreadContext(context);
	}
        try {
            return acceptNode(node, context);
        } finally {
	    if (old != context){
		setThreadContext(old);
	    }
        }
    }
    
    /**
     * Tell the context that it's started processing the script file.
     */
    protected void pushFile(Object file, Context context) {
        context.pushFile(file);
    }
    
    /**
     * Tell the context that the current script file has been completed.
     */
    protected void popFile(Context context) {
        context.popFile();
    }
    
    protected void provide(String file, Context context) {
        if (file.endsWith(".pnut")) {
            file = file.substring(0, file.length() - 5);
        }
        context.provide(file);
    }
    
    protected void revoke(String file, Context context){
        if (file.endsWith(".pnut")) {
            file = file.substring(0, file.length() - 5);
        }
        context.revoke(file);
    }
}
