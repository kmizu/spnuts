/*
 * @(#)ConfigurationAdapter.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;

import pnuts.lang.Configuration;
import pnuts.lang.Context;
import pnuts.lang.Callable;

/*
 * This class is used to customize an existing configuraion.
 */
public class ConfigurationAdapter extends Configuration {

	protected Configuration base;

	/**
	 * Constructor
	 */
	public ConfigurationAdapter() {
		this(normalConfiguration);
	}

	/**
	 * Constructor
	 * 
	 * @param base
	 *            the base configuration
	 */
	public ConfigurationAdapter(Configuration base) {
		this.base = base;
	}

	/**
	 * Returns the parent configuration
	 */
	public Configuration getParent() {
		return base;
	}

	/**
	 * Gets a field of the specified class.
	 * 
	 * @param target
	 *            the target objecgt
	 * @param name
	 *            the field name
	 * @param context
	 *            the context in which the field is read
	 */
	public Object getField(Context context, Object target, String name) {
		return base.getField(context, target, name);
	}

	/**
	 * Sets a field of the specified class.
	 * 
	 * @param context
	 *            the context in which the field is written.
	 * @param target
	 *            the target objecgt
	 * @param name
	 *            the field name
	 * @param value
	 *            the field value
	 */
	public void putField(Context context, Object target, String name,
			Object value) {
		base.putField(context, target, name, value);
	}

	/**
	 * Get the value of a static field.
	 * 
	 * @param context
	 *            the context in which the field is accessed
	 * @param clazz
	 *            the class in which the static field is defined
	 * @param name
	 *            the name of the static field
	 * @return the value
	 */
	public Object getStaticField(Context context, Class clazz, String name) {
		return base.getStaticField(context, clazz, name);
	}

	/**
	 * Sets a field of the specified class.
	 * 
	 * @param context
	 *            the context in which the field is written.
	 * @param clazz
	 *            the class in which the static field is defined
	 * @param name
	 *            the field name
	 * @param value
	 *            the field value
	 */
	public void putStaticField(Context context, Class clazz, String name,
			Object value) {
		base.putStaticField(context, clazz, name, value);
	}

	/**
	 * Call a method.
	 * 
	 * @param context
	 *            the context
	 * @param c
	 *            the class of the target object
	 * @param name
	 *            the name of the method
	 * @param args
	 *            the arguments
	 * @param types
	 *            the type information of arguments
	 * @param target
	 *            the target of the method call
	 * 
	 * @return the methods return value
	 */
	public Object callMethod(Context context, Class c, String name,
			Object args[], Class types[], Object target) {
		return base.callMethod(context, c, name, args, types, target);
	}

	/**
	 * Call a constructor.
	 * 
	 * @param args
	 *            the arguments
	 * 
	 * @return the newly created and initialized object
	 */
	public Object callConstructor(Context context, Class c, Object[] args,
			Class[] types) {
		return base.callConstructor(context, c, args, types);
	}

	/**
	 * Gets an array element
	 * 
	 * @param target
	 *            the target object (an array)
	 * @param key
	 *            a key or an index of the element
	 * @param context
	 *            the context
	 * @return the value of the element
	 */
	public Object getElement(Context context, Object target, Object key) {
		return base.getElement(context, target, key);
	}

	/**
	 * Sets an array element
	 * 
	 * @param target
	 *            the target object (an array)
	 * @param key
	 *            a key or an index of the element
	 * @param value
	 *            the new value of the element
	 * @param context
	 *            the context
	 */
	public void setElement(Context context, Object target, Object key,
			Object value) {
		base.setElement(context, target, key, value);
	}

	/**
	 * Get all public methods of the specified class.
	 * 
	 * @param cls
	 *            the class
	 * @return an array of Method objects
	 */
	public Method[] getMethods(Class cls) {
		return base.getMethods(cls);
	}

	/**
	 * Get all public constructors of the specified class.
	 * 
	 * @param cls
	 *            the class
	 * @return an array of Constructor objects
	 */
	public Constructor[] getConstructors(Class cls) {
		return base.getConstructors(cls);
	}

	/**
	 * Convert an object to Enumeration. This method is used by foreach
	 * statements. Subclasses can override this method to customize the behavior
	 * of foreach statements.
	 */
	public Enumeration toEnumeration(Object obj) {
		return base.toEnumeration(obj);
	}

	/**
	 * Convert an object to Callable. This method is used by call expression, e.g. obj(arg1, ...).
	 * Subclasses can override this method to register custom callable objects.
	 */
	public Callable toCallable(Object obj) {
		return base.toCallable(obj);
	}

	/**
	 * Handle an "not.defined" error
	 * 
	 * This method can be redefined by a subclass so that a special value (e.g.
	 * null) is returned when undefined symbol is referenced.
	 * 
	 * @param symbol
	 *            the undefined symbol
	 * @param context
	 *            the context in which the symbol is referenced
	 * @return the value to be returned
	 */
	public Object handleUndefinedSymbol(String symbol, Context context) {
		return base.handleUndefinedSymbol(symbol, context);
	}

	/**
	 * Return the value of an array expression. e.g. [a,b,c] {1,2,3}
	 * 
	 * This method can be redefined by a subclass so that array expression
	 * returns different type of object, such as java.util.List.
	 * 
	 * @param array the elements in the array expression
	 * @param context the context
	 * @return the value of the array expression
	 */
	public Object makeArray(Object[] array, Context context) {
		return base.makeArray(array, context);
	}

	/**
	 * Create a new Map object that corresponds to {key=>value} expression.
	 * 
	 * @param size
	 *            the map size
	 * @return a new Map object
	 */
	public Map createMap(int size, Context context) {
		return base.createMap(size, context);
	}

	/**
	 * String representation of an object
	 * 
	 * @param target the target object to print
	 * @return the string representation of the target object
	 */
	public String formatObject(Object target) {
		return base.formatObject(target);
	}

	/**
	 * Defines the semantices of an expression like: <blockquote>
	 * 
	 * <pre>
	 * 
	 *  target[idx1..idx2]
	 *  
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param context
	 *            the context
	 * @param target
	 *            the target object
	 * @param idx1
	 *            the start index
	 * @param idx2
	 *            the end index. null in idx2 means open-ended.
	 * @return the result
	 */
	public Object getRange(Context context, Object target, Object idx1,
			Object idx2) {
		return base.getRange(context, target, idx1, idx2);
	}

	/**
	 * Defines the semantices of an expression like: <blockquote>
	 * 
	 * <pre>
	 * 
	 *  target[idx1..idx2] = value
	 *  
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param context
	 *            the context in which the assignment is done
	 * @param target
	 *            the target object
	 * @param idx1
	 *            the start index
	 * @param idx2
	 *            the end index. null in idx2 means open-ended.
	 * @param value
	 *            the new value of the indexed element
	 * @return the result
	 */
	public Object setRange(Context context, Object target, Object idx1,
			Object idx2, Object value) {
		return base.setRange(context, target, idx1, idx2, value);
	}

}
