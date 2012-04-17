/*
 * @(#)NonPublicMemberAccessor.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Vector;

import pnuts.lang.Configuration;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import pnuts.lang.PnutsException;

/**
 * when -a option is given to the pnuts command, this class is used so that
 * non-public members can be accessed.
 */
public class NonPublicMemberAccessor extends PublicMemberAccessor {

	public NonPublicMemberAccessor() {
	}

	public NonPublicMemberAccessor(Configuration conf) {
		super(conf);
	}

	public Method[] getMethods(Class cls) {
		Vector vec = new Vector();
		Class c = cls;
		while (c != null) {
			Method[] declared = c.getDeclaredMethods();
			for (int i = 0; i < declared.length; i++) {
				Method m = declared[i];
				m.setAccessible(true);
				vec.addElement(m);
			}
			c = c.getSuperclass();
		}
		Method[] ret = new Method[vec.size()];
		vec.copyInto(ret);
		return ret;
	}

	public Constructor[] getConstructors(Class cls) {
		Vector vec = new Vector();
		Class c = cls;
		while (c != null) {
			Constructor[] declared = c.getDeclaredConstructors();
			for (int i = 0; i < declared.length; i++) {
				Constructor cons = declared[i];
				cons.setAccessible(true);
				vec.addElement(cons);
			}
			c = c.getSuperclass();
		}
		Constructor[] ret = new Constructor[vec.size()];
		vec.copyInto(ret);
		return ret;
	}

	protected Field getField(final Class cls, final String name)
			throws NoSuchFieldException {
		Class c = cls;
		while (c != null) {
			Field fields[] = c.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field f = fields[i];
				if (f.getName().equals(name)) {
					f.setAccessible(true);
					return f;
				}
			}
			c = c.getSuperclass();
		}
		throw new NoSuchFieldException(name);
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
		return getObjectField(context, target, name);
	}

	/**
	 * Sets a field value of the specified object.
	 * 
	 * @param context
	 *            the context in which the field is written.
	 * @param target
	 *            the target object
	 * @param name
	 *            the field name
	 * @param value
	 *            the field value
	 */
	public void putField(Context context, Object target, String name,
			Object value) {
		putObjectField(context, target, name, value);
	}

	protected Object getObjectField(Context context, Object target, String name) {
		try {
			return getField(target.getClass(), name).get(target);
		} catch (NoSuchFieldException e1) {
			if (target instanceof Class) {
				return getStaticField(context, (Class) target, name);
			}
			throw new PnutsException("field.notFound", new Object[] { name,
					target.getClass() }, context);
		} catch (PnutsException pe) {
			throw pe;
		} catch (Exception e) {
			throw new PnutsException(e, context);
		}
	}

	protected void putObjectField(Context context, Object target, String name,
			Object value) {
		try {
			Field field = getField(target.getClass(), name);
			Class type = field.getType();
			if (Runtime.isArray(value) && type.isArray()) {
				if (!type.isInstance(value)) {
					value = Runtime.transform(type, value, null);
				}
			}
			field.set(target, value);
		} catch (NoSuchFieldException e1) {
			throw new PnutsException("field.notFound", new Object[] { name,
					target.getClass() }, context);
		} catch (PnutsException pe) {
			throw pe;
		} catch (Exception e) {
			throw new PnutsException(e, context);
		}
	}


	public Object callMethod(Context context, Class c, String name,
			Object args[], Class types[], Object target) {
		return invokeMethod(context, c, name, args, types, target);
	}
}
