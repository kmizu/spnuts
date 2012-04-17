/*
 * PublicMemberAccessor
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import pnuts.ext.ConfigurationAdapter;
import java.security.*;
import java.lang.reflect.*;

/**
 * This is a configuration for public field access.
 */
public class PublicMemberAccessor extends ConfigurationAdapter {

	/**
	 * Constructor
	 */
	public PublicMemberAccessor() {
	}

	public PublicMemberAccessor(Configuration base) {
	    super(base);
	}

	public Object getField(Context context, Object target, String name) {
	    return context.runtime._getField(context, target.getClass(), name, target);
	}

	public void putField(Context context, Object target, String name, Object value) {
	    context.runtime._putField(context, target.getClass(), name, target, value);
	}

	public Object getStaticField(Context context, Class cls, String name) {
	    return context.runtime._getField(context, cls, name, null);
	}

	public void putStaticField(Context context, Class cls, String name, Object value) {
	    context.runtime._putField(context, cls, name, null, value);
	}

	public Method[] getMethods(final Class cls) {
		return (Method[]) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return cls.getMethods();
			}
		});
	}

	public Constructor[] getConstructors(final Class cls) {
		return (Constructor[]) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return cls.getConstructors();
					}
				});
	}

}
