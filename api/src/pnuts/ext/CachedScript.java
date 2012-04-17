/*
 * CachedScript.java
 * 
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import pnuts.lang.Pnuts;
import pnuts.lang.Runtime;
import pnuts.lang.Visitor;
import pnuts.lang.Implementation;
import pnuts.lang.ParseException;
import pnuts.lang.PnutsException;
import pnuts.lang.Context;
import pnuts.compiler.Compiler;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Executable script that is automatically recompiled
 */
public class CachedScript extends Pnuts {
    protected URL scriptURL;
    protected long parsedTime;
    protected Pnuts script;
    protected String encoding;

    /**
     * Constructor
     *
     * @param scriptURL the URL of the script
     */
    public CachedScript(URL scriptURL) throws IOException, ParseException{
	this(scriptURL, null, null);
    }

    /**
     * Constructor
     *
     * @param scriptURL the URL of the script
     * @param encoding the character encoding of the script.  If null, the default encoding is used.
     * @param context the context in which the script is first parsed/compiled.
     */
    public CachedScript(URL scriptURL, String encoding, Context context)
	throws IOException, ParseException
    {
	this.scriptURL = scriptURL;
	this.encoding = encoding;
	update(context);
    }

    public String unparse(){
	return script.unparse();
    }

    public Object run(Context c){
	try {
	    if (needToUpdate()){
		update(c);
	    }
	} catch (PnutsException pe){
	    throw pe;
	} catch (Exception e){
	    throw new PnutsException(e, c);
	}
	return script.run(c);
    }

    public Object accept(Visitor v, Context c){
	try {
	    if (needToUpdate()){
		update(c);
	    }
	} catch (PnutsException pe){
	    throw pe;
	} catch (Exception e){
	    throw new PnutsException(e, c);
	}
	return script.accept(v, c);
    }

    /**
     * Determin if the script should be recompiled
     *
     * @return true if the script should be recompiled
     */
    protected boolean needToUpdate(){
	long modified = lastModified(scriptURL);
	return modified < 0 || (modified > parsedTime);
    }

    static long lastModified(URL scriptURL){
	try {
	    final URLConnection conn = scriptURL.openConnection();
	    final InputStream in = conn.getInputStream();
	    try {
		return conn.getLastModified();
	    } finally {
		in.close();
	    }
	} catch (IOException e){
	    return -1L;
	}
    }

    /**
     * Returns a compiler. If this method returns null,
     * script won't be compiled. 
     */
    protected Compiler getCompiler(){
	Compiler compiler = new Compiler(null, false, true);
	compiler.setConstantFolding(true);
	return compiler;
    }

    /**
     * Parse/compile the script and update the timestamp.
     *
     * @param context the context in which the script is compiled.
     */
    protected void update(Context context) throws IOException, ParseException {
	Reader reader;
	if (encoding != null){
	    reader = new InputStreamReader(scriptURL.openStream(), encoding);
	} else {
	    if (context != null){
		reader = Runtime.getScriptReader(scriptURL.openStream(), context);
	    } else {
		reader = new InputStreamReader(scriptURL.openStream());
	    }
	}
	this.script = Pnuts.parse(reader);
	script.setScriptSource(scriptURL);
	if (context == null){
	    context = new Context();
	}
	Compiler compiler = getCompiler();
	this.script = compiler.compile(script, context);
	this.parsedTime = System.currentTimeMillis();
    }
}
