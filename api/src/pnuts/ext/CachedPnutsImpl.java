/*
 * @(#)CachedPnutsImpl.java 1.4 05/05/25
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

import org.pnuts.util.Cache;
import org.pnuts.util.MemoryCache;

import pnuts.compiler.Compiler;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsImpl;
import pnuts.lang.Runtime;

/**
 * PnutsImpl which caches parsed (compiled) scripts and reuse them. This class
 * is useful when same scripts are executed over and over, e.g. servlet scripts.
 * 
 * @see pnuts.lang.PnutsImpl
 */
public class CachedPnutsImpl extends PnutsImpl {
	private final static boolean DEBUG = false;
	private final static boolean java2 = Pnuts.isJava2();

	private boolean useCompiler;
	private boolean useDynamicProxy;
	private boolean includeLineNo;
	private Cache file_cache;

	public CachedPnutsImpl() {
		this(true);
	}

	/**
	 * @param useCompiler
	 *            true if compiler is used (default)
	 */
	public CachedPnutsImpl(boolean useCompiler) {
		this(useCompiler, true, false);
	}

	/**
	 * @param useCompiler
	 *            true if the compiler is used (default)
	 * @param useDynamicProxy
	 *            true if the compiler generates dynamic proxy.
	 * @param includeLineNo
	 *            true if the compiler generates line number information.
	 */
	public CachedPnutsImpl(boolean useCompiler, boolean useDynamicProxy,
			       boolean includeLineNo) {
		this(useCompiler, useDynamicProxy, includeLineNo, createCache());
	}

	/**
	 * @param useCompiler
	 *            true if the compiler is used (default)
	 * @param useDynamicProxy
	 *            true if the compiler generates dynamic proxy.
	 * @param includeLineNo
	 *            true if the compiler generates line number information.
	 * @param cache
	 *            a cache object to reuse compiled code
	 */
	public CachedPnutsImpl(boolean useCompiler, boolean useDynamicProxy,
			       boolean includeLineNo, Cache cache) {
		this.useCompiler = useCompiler;
		this.useDynamicProxy = useDynamicProxy;
		this.includeLineNo = includeLineNo;
		this.file_cache = cache;
	}

	public void includeLineNo(boolean flag) {
		this.includeLineNo = flag;
	}

	/**
	 * Reset the cache entries
	 */
	public void reset() {
		this.file_cache.reset();
	}

	protected ScriptCacheEntry getCachedCode(Object key) {
		return (ScriptCacheEntry) file_cache.get(key);
	}

	protected void putCachedCode(Object key, ScriptCacheEntry entry) {
		file_cache.put(key, entry);
	}

	/**
	 * Load a script file from a URL
	 * 
	 * @param scriptURL
	 *            the URL of the script
	 * @param context
	 *            the context in which the script is executed
	 */
	public Object load(URL scriptURL, Context context) {
		String protocol = scriptURL.getProtocol();
		Pnuts parsed = null;
		ScriptCacheEntry entry = null;
		InputStream in = null;
		Reader reader = null;

		Context old = getThreadContext();
		setThreadContext(context);
		try {
			if ("file".equals(protocol)) {
				String fileName = scriptURL.getFile();
				File file = new File(fileName);
				String canon = file.getCanonicalPath();
				long lastModified = file.lastModified();
				entry = getCachedCode(scriptURL);
				if (entry == null || lastModified == 0 || // coundn't get the
				    // timestamp
				    entry.lastModified < lastModified) {
					in = new FileInputStream(file);
					parsed = Pnuts.parse(Runtime.getScriptReader(in, context),
							     scriptURL, context);

					if (useCompiler) {
						Compiler compiler = new Compiler(null, false,
										 useDynamicProxy);
						compiler.includeLineNo(includeLineNo);
						compiler.setConstantFolding(true);
						if (DEBUG) {
							System.out.println("compiling " + file);
						}
						try {
							parsed = compiler.compile(parsed, context);
						} catch (ClassFormatError cfe) {
							/* ignore */
						}
					}
					if (lastModified != 0) {
						entry = new ScriptCacheEntry();
						entry.lastModified = lastModified;
						entry.parsedExpression = parsed;
						putCachedCode(scriptURL, entry);
					}
				} else {
					parsed = entry.parsedExpression;
				}
			} else {
				URLConnection conn = scriptURL.openConnection();
				long lastModified = conn.getLastModified();
				entry = getCachedCode(scriptURL);
				if (entry == null || lastModified == 0 || // unknown
				    entry.lastModified < lastModified) {
					in = conn.getInputStream();
					parsed = Pnuts.parse(Runtime.getScriptReader(in, context),
							     scriptURL, context);
					if (useCompiler) {
						Compiler compiler = new Compiler(null, false,
										 useDynamicProxy);
						compiler.includeLineNo(includeLineNo);
						compiler.setConstantFolding(true);
						if (DEBUG) {
							System.out.println("compiling " + scriptURL);
						}
						try {
							parsed = compiler.compile(parsed, context);
						} catch (ClassFormatError cfe) {
							/* ignore */
						}
					}
					if (lastModified != 0) {
						entry = new ScriptCacheEntry();
						entry.lastModified = lastModified;
						entry.parsedExpression = parsed;
						putCachedCode(scriptURL, entry);
					}
				} else {
					parsed = entry.parsedExpression;
				}
			}
			return parsed.run(context);
		} catch (Throwable t) {
			checkException(context, t);
			return null;
		} finally {
			setThreadContext(old);
			if (in != null) {
				try {
					in.close();
				} catch (IOException io) {
				}
			}
		}
	}

	public Object eval(String script, Context context) {
		Context old = getThreadContext();
		setThreadContext(context);
		try {
			ScriptCacheEntry entry = getCachedCode(script);
			if (entry == null){
				Pnuts parsed = Pnuts.parse(script);
				try {
					Compiler compiler = new Compiler(null, false,
									 useDynamicProxy);
					compiler.includeLineNo(includeLineNo);
					compiler.setConstantFolding(true);
					parsed = compiler.compile(parsed, context);
				} catch (ClassFormatError e){
					// skip
				}
				entry = new ScriptCacheEntry();
				entry.parsedExpression = parsed;
				putCachedCode(script, entry);
			}
			return entry.parsedExpression.run(context);
		} catch (Exception e){
			checkException(context, e);
			return null;
		} finally {
			setThreadContext(old);
		}
	}

	public static class ScriptCacheEntry {
		public long lastModified;

		public Pnuts parsedExpression;
	}
}
