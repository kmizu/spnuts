/*
 * @(#)SecurePnutsImpl.java 1.3 05/05/09
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import pnuts.ext.ImplementationAdapter;
import pnuts.lang.Context;
import pnuts.lang.Implementation;
import pnuts.lang.PnutsImpl;
import pnuts.lang.Runtime;
import pnuts.lang.SimpleNode;

/**
 * A PnutsImpl subclass that execute scripts in an access control context in 
 * Java2 Security.
 *
 * <pre>e.g.
 *  context.setImplementation(new SecurePnutsImpl(new CompilerPnutsImpl(), codesource))
 * </pre>
 */
public class SecurePnutsImpl extends ImplementationAdapter {

	private CodeSource codeSource;

	/**
	 * A Constructor
	 *
	 * @param impl a PnutsImpl object
	 *
	 * @deprecated replaced by SecurePnutsImpl(Implementation)
	 */
	public SecurePnutsImpl(PnutsImpl impl){
		this(impl, null);
	}

	/**
	 * A Constructor
	 *
	 * @param impl a PnutsImpl object
	 */
	public SecurePnutsImpl(Implementation impl){
		this(impl, null);
	}

	/**
	 * A Constructor
	 *
	 * @param impl the base implementation
	 * @param codeSource a CodeSource object which indicates the source of the expression
	 *		execute by eval(String, Context).
	 *
	 * @deprecated replaced by SecurePnutsImpl(Implementation, CodeSource)
	 */
	public SecurePnutsImpl(PnutsImpl impl, CodeSource codeSource){
		super(impl);
		this.codeSource = codeSource;
	}

	/**
	 * A Constructor
	 *
	 * @param impl the base implementation
	 * @param codeSource a CodeSource object which indicates the source of the expression
	 *		execute by eval(String, Context).
	 */
	public SecurePnutsImpl(Implementation impl, CodeSource codeSource){
		super(impl);
		this.codeSource = codeSource;
	}

	/**
	 * Evaluate an expreesion
	 *
	 * @param expr the expression to be evaluated
	 * @param context the context in which the expression is evaluated
	 * @return the result of the evaluation
	 */
	public Object eval(final String expr, final Context context) {
		CodeSource cs = codeSource;
		if (cs == null){
			cs = new CodeSource(null, (Certificate[])null);
		}
		AccessControlContext acc = getAccessControlContext(cs);
		return AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					return SecurePnutsImpl.super.eval(expr, context);
				}
			}, acc);
	}

	/**
	 * Evaluate a parsed script
	 *
	 * @param node the parsed script
	 * @param context the context in which the script is evaluated
	 * @return the result of the evaluation
	 */
	public Object accept(final SimpleNode node, final Context context){
		CodeSource cs = codeSource;
		if (cs == null){
			cs = new CodeSource(null, (Certificate[])null);
		}
		AccessControlContext acc = getAccessControlContext(cs);
		return AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					return SecurePnutsImpl.super.accept(node, context);
				}
			}, acc);
	}

	/**
	 * Load a script file using classloader
	 *
	 * @param file the name of the script
	 * @param context the context in which the script is executed
	 * @return the result of the evaluation
	 */
	public Object load(final String file, final Context context) throws FileNotFoundException {
		URL url = (URL)AccessController.doPrivileged(new PrivilegedAction(){
				public Object run(){
					return Runtime.getScriptURL(file, context);
				}
			});
		if (url == null){
			throw new FileNotFoundException(file);
		}
		boolean completed = false;
		try {
			provide(file, context);
			Object ret = load(url, context);
			completed = true;
			return ret;
		} finally {
			if (!completed){
				revoke(file, context);
			}
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
	public Object loadFile(String filename, Context context) throws FileNotFoundException {
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
	 * Load a script file from a URL
	 *
	 * @param scriptURL the URL of the script
	 * @param context the context in which the script is executed
	 */
	public Object load(final URL scriptURL, final Context context) {
		CodeSource cs = codeSource;
		if (cs == null){
			cs = getCodeSource(scriptURL);
		}
		AccessControlContext acc = getAccessControlContext(cs);
		return AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					return SecurePnutsImpl.super.load(scriptURL, context);
				}
			}, acc);
	}

	private static CodeSource getCodeSource(URL scriptURL){
		final URL url = scriptURL;
		return (CodeSource)AccessController.doPrivileged(new PrivilegedAction(){
				public Object run(){
					try {
						URLConnection con = url.openConnection();
						if (con instanceof JarURLConnection){
							JarURLConnection jcon = (JarURLConnection)con;
							return new CodeSource(jcon.getJarFileURL(), jcon.getCertificates());
						}
					} catch (IOException e){
					}
					return new CodeSource(url, (Certificate[])null);
				}
			});
	}

	/**
	 * Gets permission declared in the policy file
	 *
	 * @param codesource the CodeSource of the script
	 * @return the permissions for the script
	 */
	protected PermissionCollection getPolicyPermissions(final CodeSource codesource){
		PermissionCollection perms = (PermissionCollection)
			AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						Policy policy = Policy.getPolicy();
						if (policy != null) {
							return policy.getPermissions(codesource);
						} else {
							return null;
						}
					}
				});
		if (perms == null) {
			perms = new Permissions();
		}
		return perms;
	}

	/**
	 * Add Applet sand-box permissions to the specified PermissionCollection.
	 *
	 * @param codebase the codebase of the script
	 * @param perms the base PermissionCollection
	 */
	protected void addSandBoxPermissions(URL codebase, PermissionCollection perms){
		Permission p = null;
		try {
			if (codebase != null){
				p = codebase.openConnection().getPermission();
			}
		} catch (java.io.IOException ioe) {
		}

		if (p instanceof FilePermission) {
			String path = p.getName();
			int endIndex = path.lastIndexOf(File.separatorChar);
			perms.add(p);

			if (endIndex != -1) {
				path = path.substring(0, endIndex + 1);
				if (path.endsWith(File.separator)) {
					path += "-";
				}
				perms.add(new FilePermission(path, "read"));
			}
		} else if (codebase != null){
			String host = codebase.getHost();
			if (host == null){
				host = "localhost";
			}
			perms.add(new SocketPermission(host, "connect, accept"));
			if (p != null){
				perms.add(p);
			}
		}
	}

	/**
	 * Returns permissions from policy file, plus Applet's sand-box permissions.
	 * A subclass may override this method to define a custom security policy.
	 *
	 * @param codesource the CodeSource of the script
	 * @return the PermissionCollection for the script
	 */
	protected PermissionCollection getPermissions(CodeSource codesource){
		PermissionCollection perms = getPolicyPermissions(codesource);
		addSandBoxPermissions(codesource.getLocation(), perms);
		return perms;
	}

	private AccessControlContext getAccessControlContext(CodeSource codesource){
		PermissionCollection perms = getPermissions(codesource);
		ProtectionDomain domain = new ProtectionDomain(codesource, perms);
		return new AccessControlContext(new ProtectionDomain[] {domain});
	}

	CodeSource getCodeSource(){
		return codeSource;
	}

	public String toString(){
		return getClass().getName() + "[" + getBaseImpl() + ", " + codeSource + "]";
	}
}
