/*
 * @(#)JavaBeansConfiguration.java 1.3 05/05/09
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.pnuts.lang.*;

/**
 * This is a configuration for JavaBeans. Only methods in method descriptors can
 * be called. Field access expression reads/writes property of the Beans.
 */
abstract class JavaBeansConfiguration extends Configuration {

	private Class stopClass;

	/**
	 * Constructor
	 */
	public JavaBeansConfiguration() {
	}

	/**
	 * Constructor
	 */
	public JavaBeansConfiguration(Class stopClass) {
		this.stopClass = stopClass;
	}

	/*
	 * Gets the Introspector's "stopClass"
	 * 
	 * @see java.beans.Introspector
	 */
	protected Class getStopClass() {
		return stopClass;
	}

	/**
	 * Collects the Bean methods for the specified class.
	 */
	public Method[] getMethods(Class cls) {
	    return ObjectDescFactory.getDefault().create(cls, stopClass).getMethods();
	}

	/**
	 * Get all public constructors of the specified class.
	 * 
	 * @param cls the class
	 * @return an array of Constructor objects
	 */
	public Constructor[] getConstructors(final Class cls) {
		return (Constructor[]) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return cls.getConstructors();
					}
				});
	}

	/**
	 * Gets a Bean property of the specified bean.
	 * 
	 * @param context
	 *            the context in which the property is read
	 * @param target
	 *            the target bean
	 * @param name
	 *            the Bean property name
	 */
	public Object getField(Context context, Object target, String name) {
		return getBeanProperty(context, target, name);
	}

	/**
	 * Gets a Bean property of the specified bean.
	 * 
	 * @param context
	 *            the context in which the property is read
	 * @param target
	 *            the target bean
	 * @param name
	 *            the Bean property name
	 */
	protected Object getBeanProperty(Context context, Object target, String name) {
		try {
			return context.runtime.getBeanProperty(target, name, stopClass);
		} catch (InvocationTargetException e1) {
			throw new PnutsException(e1.getTargetException(), context);
		} catch (IllegalAccessException e2) {
			throw new PnutsException(e2, context);
		}
	}

	/**
	 * Sets a Bean property of the specified bean.
	 * 
	 * @param context
	 *            the context in which the property is read
	 * @param target
	 *            the target bean
	 * @param name
	 *            the Bean property name
	 * @param value
	 *            the new property value
	 */
	public void putField(Context context, Object target, String name,
			Object value) {
		setBeanProperty(context, target, name, value);
	}

	/**
	 * Sets a Bean property of the specified bean.
	 * 
	 * @param context
	 *            the context in which the property is read
	 * @param target
	 *            the target bean
	 * @param name
	 *            the Bean property name
	 * @param value
	 *            the new property value
	 */
	protected void setBeanProperty(Context context, Object target, String name,
			Object value) {
		try {
			context.runtime.setBeanProperty(target, name, value, stopClass);
		} catch (InvocationTargetException e1) {
			throw new PnutsException(e1.getTargetException(), context);
		} catch (IllegalAccessException e2) {
			throw new PnutsException(e2, context);
		}
	}

	/**
	 * Calls a method
	 * 
	 * @param context
	 *            the contexct
	 * @param c
	 *            the class of the method
	 * @param name
	 *            the name of the method
	 * @param args
	 *            arguments
	 * @param types
	 *            type information of each arguments
	 * @param target
	 *            the target object of the method call
	 * @return the result of the method call
	 */
	public Object callMethod(Context context, Class c, String name,
			Object args[], Class types[], Object target) {
		try {
			return context.runtime._callMethod(context, c, name, args, types,
							   target);
		} catch (PnutsException pe){
			PnutsException.TraceInfo trace =
				new PnutsException.TraceInfo(target,
							     name,
							     args,
							     context.getScriptSource(),
							     context.beginLine,
							     context.beginColumn);
			pe.backtrace(trace);
			throw pe;
		}
	}

	/**
	 * Calls a constructor
	 * 
	 * @param context
	 *            the context
	 * @param c
	 *            class of the constructor
	 * @param args
	 *            the arguments
	 * @param types
	 *            type information of each arguments
	 * @return the result
	 */
	public Object callConstructor(Context context, Class c, Object[] args,
			Class[] types) {
		try {
			return context.runtime._callConstructor(context, c, args, types);
		} catch (PnutsException pe){
			PnutsException.TraceInfo trace =
				new PnutsException.TraceInfo(c,
							     args,
							     context.getScriptSource(),
							     context.beginLine,
							     context.beginColumn);
			pe.backtrace(trace);
			throw pe;
		}
	}
}
