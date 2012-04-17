/*
 * PublicMemberAccessor.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import pnuts.lang.Configuration;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import pnuts.lang.Property;
import pnuts.lang.Runtime;
import org.pnuts.util.*;

public class PublicMemberAccessor extends pnuts.lang.PublicMemberAccessor {

	private transient Cache fieldCache = new MemoryCache();

	public PublicMemberAccessor() {
	}

	public PublicMemberAccessor(Configuration conf) {
		super(conf);
	}

	/**
	 * Gets a field value of the target object.
	 * 
	 * @param context
	 *            the context in which the field is read
	 * @param target
	 *            the target object
	 * @param name
	 *            the field name
	 * @return the field value
	 */
	public Object getField(Context context, Object target, String name) {
		if (target instanceof Context) {
			return ((Context) target).get(name);
		} else if (target instanceof Property) {
			return ((Property) target).get(name, context);
		} else if (target instanceof Map) {
			return ((Map) target).get(name);
		} else {
			return super.getField(context, target, name);
		}
	}

	/**
	 * Sets a field value of the specified object.
	 * 
	 * @param context  the context in which the field is written.
	 * @param target  the target object
	 * @param name  the field name
	 * @param expr  the field value
	 */
	public void putField(Context context, Object target, String name, Object expr) {
		if (target instanceof Context) {
			((Context) target).set(name, expr);
		} else if (target instanceof Property) {
			((Property) target).set(name, expr, context);
		} else if (target instanceof Map) {
			((Map) target).put(name, expr);
		} else {
			super.putField(context, target, name, expr);
		}
	}


	public Object getStaticField(Context context, Class clazz, String name) {
		try {
			return getField(clazz, name).get(null);
		} catch (PnutsException p) {
			throw p;
		} catch (NoSuchFieldException f) {
			throw new PnutsException("field.notFound", new Object[] { name,
					clazz }, context);
		} catch (Throwable t) {
			throw new PnutsException(t, context);
		}
	}

	public void putStaticField(Context context, Class clazz, String name,
			Object value) {
		try {
			Field field = getField(clazz, name);
			Class type = field.getType();
			if (Runtime.isArray(value) && type.isArray()) {
				if (!type.isInstance(value)) {
					value = Runtime.transform(type, value, context);
				}
			}
			field.set(null, value);
		} catch (PnutsException e0) {
			throw e0;
		} catch (NoSuchFieldException f) {
			throw new PnutsException("field.notFound", new Object[] { name,
					clazz }, context);
		} catch (Throwable e) {
			throw new PnutsException(e, context);
		}
	}

	protected Field getField(final Class cls, final String name)
			throws NoSuchFieldException {
		try {
			return (Field) AccessController
					.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws Exception {
							return cls.getField(name);
						}
					});
		} catch (PrivilegedActionException e) {
			throw (NoSuchFieldException) e.getException();
		}
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
