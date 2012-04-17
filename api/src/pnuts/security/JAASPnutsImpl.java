/*
 * @(#)JAASPnutsImpl.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.security;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Set;

import javax.security.auth.Subject;

import pnuts.lang.Implementation;
import pnuts.lang.PnutsImpl;

/**
 * A SecurePnutsImpl subclass that executes scripts with permissions constructed from
 * codesource-based policy and subject-based policy.
 *
 * <pre>e.g.
 *  context.setImplementation(new JAASPnutsImpl(context.Implementation(), codesource, subject))
 * </pre>
 */
public class JAASPnutsImpl extends SecurePnutsImpl {

	private Subject subject;

	/**
	 * A Constructor
	 *
	 * @param impl a PnutsImpl object
	 *
	 * @deprecated replaced by JAASPnutsImpl(Implementation)
	 */
	public JAASPnutsImpl(PnutsImpl impl){
		this(impl, null);
	}

	/**
	 * A Constructor
	 *
	 * @param impl a Implementation object
	 */
	public JAASPnutsImpl(Implementation impl){
		this(impl, null);
	}

	/**
	 * A Constructor
	 *
	 * @param impl the base implementation
	 * @param codeSource a CodeSource object which indicates the source of the expression
	 *		execute by eval(String, Context).
	 *
	 * @deprecated replaced byte JAASPnutsImpl(Implementation, CodeSource)
	 */
	public JAASPnutsImpl(PnutsImpl impl, CodeSource codeSource){
		this(impl, codeSource, null);
	}

	/**
	 * A Constructor
	 *
	 * @param impl the base implementation
	 * @param codeSource a CodeSource object which indicates the source of the expression
	 *		execute by eval(String, Context).
	 */
	public JAASPnutsImpl(Implementation impl, CodeSource codeSource){
		this(impl, codeSource, null);
	}


	/**
	 * A Constructor
	 *
	 * @param impl the base implementation
	 * @param codeSource a CodeSource object which indicates the source of the expression
	 *		execute by eval(String, Context).
	 * @param subject a Subject
	 *
	 * @deprecated replaced by JAASPnutsImpl(Implementation, CodeSource, Subject)
	 */
	public JAASPnutsImpl(PnutsImpl impl, CodeSource codeSource, Subject subject){
		super(impl, codeSource);
		this.subject = subject;
	}

	/**
	 * A Constructor
	 *
	 * @param impl the base implementation
	 * @param codeSource a CodeSource object which indicates the source of the expression
	 *		execute by eval(String, Context).
	 * @param subject a Subject
	 */
	public JAASPnutsImpl(Implementation impl, CodeSource codeSource, Subject subject){
		super(impl, codeSource);
		this.subject = subject;
	}

	protected PermissionCollection getPermissions(final CodeSource codesource){
		if (subject != null){
			PermissionCollection perms = (PermissionCollection)
				AccessController.doPrivileged(new PrivilegedAction() {
						public Object run() {
							Policy policy = Policy.getPolicy();
							if (policy != null) {
								Set s = subject.getPrincipals();
								Principal[] principals = new Principal[s.size()];
								s.toArray(principals);
								return policy.getPermissions(new ProtectionDomain(codesource, null, null, principals));
							} else {
								return null;
							}
						}
					});
			if (perms == null){
				perms = new Permissions();
			}
			return perms;
		} else {
			return super.getPermissions(codesource);
		}
	}
	
	public String toString(){
		return getClass().getName() + "[" + getBaseImpl() + ", " + getCodeSource() + "]";
	}
}
