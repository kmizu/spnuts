/*
 * @(#)Configuration.java 1.7 05/06/27
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.AbstractList;
import java.util.RandomAccess;
import java.util.Arrays;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Properties;
import org.pnuts.util.*;
import org.pnuts.lang.*;

/**
 * This class defines the interface of runtime configuration, such as how to
 * find method/field candidates, how to get the field value, how to get indexed
 * elements, and so on.
 */
public abstract class Configuration implements Serializable {

	static final long serialVersionUID = 8561037106846360504L;

	/**
	 * The normal configuration, which is the fall-back of the default
	 * configuration.
	 */
	protected final static Configuration normalConfiguration;

	/**
	 * The default configuration, which can be configured with the system
	 * property "pnuts.lang.defaultConfiguration" at start-up time
	 */
	private static Configuration defaultConfiguration;

	static {
		normalConfiguration = ConfigurationConstants.NORMAL_CONFIGURATION;
                defaultConfiguration = getInstance(Runtime.getProperty("pnuts.lang.defaultConfiguration"));
	}

	/**
	 * object1 + object2
	 */
	protected BinaryOperator _add;

	/**
	 * object1 - object2
	 */
	protected BinaryOperator _subtract;

	/**
	 * object1 * object2
	 */
	protected BinaryOperator _multiply;

	/**
	 * object1 % object2
	 */
	protected BinaryOperator _mod;

	/**
	 * object1 / object2
	 */
	protected BinaryOperator _divide;

	/**
	 * object1 >>> object2
	 */
	protected BinaryOperator _shiftArithmetic;

	/**
	 * object1 < < object2
	 */
	protected BinaryOperator _shiftLeft;

	/**
	 * object1 >> object2
	 */
	protected BinaryOperator _shiftRight;

	/**
	 * object1 & object2
	 */
	protected BinaryOperator _and;

	/**
	 * object1 | object2
	 */
	protected BinaryOperator _or;

	/**
	 * object1 ^ object2
	 */
	protected BinaryOperator _xor;

	/**
	 * object++, ++object
	 */
	protected UnaryOperator _add1;

	/**
	 * object--, object--
	 */
	protected UnaryOperator _subtract1;

	/**
	 * ~object
	 */
	protected UnaryOperator _not;

	/**
	 * - object
	 */
	protected UnaryOperator _negate;

	/**
	 * object1 == object2
	 */
	protected BooleanOperator _eq;

	/**
	 * object1 < object2
	 */
	protected BooleanOperator _lt;

	/**
	 * object1 <= object2
	 */
	protected BooleanOperator _le;

	/**
	 * object1 > object2
	 */
	protected BooleanOperator _gt;

	/**
	 * object1 >= object2
	 */
	protected BooleanOperator _ge;

	/**
	 * Default imports
	 */
	String[] _imports;

	private final static String[] DEFAULT_IMPORTS = new String[]{"*", "java.lang.*"};

	/**
	 * Returns the default Configuration object.
	 * 
	 * @return the current default configuration
	 */
	public static Configuration getDefault() {
		return defaultConfiguration;
	}

	static Configuration getDefault(Properties properties) {
		String property = properties.getProperty("pnuts.lang.defaultConfiguration");
		return getInstance(property);
	}
        
        static Configuration getInstance(String className){
            try {
                if (className != null && !className.equals("pnuts.lang.Configuration")) {
                    Class cls = Class.forName(className);
                    return (Configuration) cls.newInstance();
                }
            } catch (Exception e2) {
                /* ignore */
            }
            return normalConfiguration;
        }
        
        
	public Configuration() {
		initializeOperators();
	}

	/**
	 * Subclasses may redefines default imports
	 *
	 * @return an array of imported classes, package, or statics
	 */
	protected String[] getDefaultImports(){
		return DEFAULT_IMPORTS;
	}

	protected void initializeOperators() {
		this._add = BinaryOperator.Add.instance;
		this._subtract = BinaryOperator.Subtract.instance;
		this._multiply = BinaryOperator.Multiply.instance;
		this._mod = BinaryOperator.Mod.instance;
		this._divide = BinaryOperator.Divide.instance;
		this._shiftArithmetic = BinaryOperator.ShiftArithmetic.instance;
		this._shiftLeft = BinaryOperator.ShiftLeft.instance;
		this._shiftRight = BinaryOperator.ShiftRight.instance;
		this._and = BinaryOperator.And.instance;
		this._or = BinaryOperator.Or.instance;
		this._xor = BinaryOperator.Xor.instance;
		this._add1 = UnaryOperator.Add1.instance;
		this._subtract1 = UnaryOperator.Subtract1.instance;
		this._not = UnaryOperator.Not.instance;
		this._negate = UnaryOperator.Negate.instance;
		this._eq = BooleanOperator.EQ.instance;
		this._lt = BooleanOperator.LT.instance;
		this._le = BooleanOperator.LE.instance;
		this._gt = BooleanOperator.GT.instance;
		this._ge = BooleanOperator.GE.instance;
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
	public abstract Object getStaticField(Context context, Class clazz,
			String name);

	/**
	 * Sets a value to the static field of the specified class.
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
	public abstract void putStaticField(Context context, Class clazz,
			String name, Object value);

	/**
	 * Gets an array element
	 * 
	 * @param context
	 *            the context
	 * @param target
	 *            the target object (an array)
	 * @param key
	 *            the key or the index of the element
	 * @return the value of the element
	 */
	public abstract Object getElement(Context context, Object target, Object key);

	/**
	 * Sets an element
	 * 
	 * @param context
	 *            the context
	 * @param target
	 *            the target object (an array)
	 * @param key
	 *            the key or the index of the element
	 * @param value
	 *            the new value of the element
	 */
	public abstract void setElement(Context context, Object target, Object key,
			Object value);

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
	public abstract Object callMethod(Context context, Class c, String name,
			Object args[], Class types[], Object target);

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
	public abstract Object callConstructor(Context context, Class c,
			Object[] args, Class[] types);

	/**
	 * Get all public methods of the specified class.
	 * 
	 * @param cls
	 *            the class
	 * @return an array of Method objects
	 */
	public abstract Method[] getMethods(Class cls);

	/**
	 * Get all public constructors of the specified class.
	 * 
	 * @param cls
	 *            the class
	 * @return an array of Constructor objects
	 */
	public abstract Constructor[] getConstructors(Class cls);

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
	public abstract Object getRange(Context context, Object target,
			Object idx1, Object idx2);

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
	public abstract Object setRange(Context context, Object target,
			Object idx1, Object idx2, Object value);

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
	public abstract Object getField(Context context, Object target, String name);

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
	public abstract void putField(Context context, Object target, String name,
			Object value);

	/**
	 * Convert an object to Enumeration. This method is used by foreach
	 * statements. Subclasses can override this method to customize the behavior
	 * of foreach statements.
	 */
	public abstract Enumeration toEnumeration(Object obj);

	/**
	 * Convert an object to Callable. This method is used by call expression, e.g. obj(arg1, ...).
	 * Subclasses can override this method to register custom callable objects.
	 */
	public abstract Callable toCallable(Object obj);

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
		throw new PnutsException("not.defined", new Object[] { symbol },
				context);
	}

	/**
	 * Return the value of an array expression. e.g. [a,b,c] {1,2,3}
	 * 
	 * This method can be redefined by a subclass so that array expression
	 * returns different type of object, such as java.util.List.
	 * 
	 * @param parameters
	 *            the elements in the array expression
	 * @return the value of the array expression
	 */
	public Object makeArray(Object[] parameters, Context context) {
		return parameters;
	}

	/**
	 * Create a new Map object that corresponds to {key=>value} expression.
	 * 
	 * @param size
	 *            the map size
	 * @return a new Map object
	 */
	public Map createMap(int size, Context context) {
		return new HashMap(size);
	}

	public List createList() {
	    return new ComparableArrayList();
	}

	/**
	 * Defines how objects are printed in the interactive shell.
	 * 
	 * @param obj
	 *            the target object to print
	 * @return the string representation of the target object
	 */
	public String formatObject(Object obj) {
		return Runtime.format(obj, 64);
	}

	protected static Object invokeMethod(Context context, Class c, String name,
			Object args[], Class types[], Object target) {
		return context.runtime._callMethod(context, c, name, args, types,
				target);
	}



	/*
	 * class <-> public methods
	 */
	private transient Cache mtab = new MemoryCache();

	synchronized Method[] _getMethods(Class cls, String name) {
		Cache cache = (Cache) mtab.get(cls);
		if (cache == null) {
			cache = new MemoryCache();
			mtab.put(cls, cache);
		}
		Object v = cache.get(name);
		if (v instanceof Method[]) {
			return (Method[]) v;
		} else {
			Method m[] = getMethods(cls);
			if (m == null) { // for Bug:4137722
				throw new NoClassDefFoundError("" + cls);
			}
			int j = 0;
			for (int i = 0; i < m.length; i++) {
				String m_name = m[i].getName();
				if (m_name.equals(name) && i >= j) {
					m[j] = m[i];
					j++;
				}
			}
			Method m2[] = new Method[j];
			System.arraycopy(m, 0, m2, 0, j);
			cache.put(name, m2);
			return m2;
		}
	}

	/*
	 * class <-> Constructors
	 */
	private transient Cache ctab = new MemoryCache();

	synchronized Constructor[] _getConstructors(Class cls) {
		Object v = ctab.get(cls);
		if (v instanceof Constructor[]) {
			return (Constructor[]) v;
		} else {
			Constructor con[] = getConstructors(cls);
			ctab.put(cls, con);
			return con;
		}
	}

	Object reInvoke(IllegalAccessException t, Method method, Object target,
			Object[] args) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		return normalConfiguration.reInvoke(t, method, target, args);
	}

	protected ClassLoader getInitialClassLoader() {
		return null;
	}

	void replace(StringBuffer buf, int start, int end, String str) {
		normalConfiguration.replace(buf, start, end, str);
	}

       BigDecimal longToBigDecimal(long lval){
	   return new BigDecimal(BigInteger.valueOf(lval));
       }

	private void readObject(ObjectInputStream s)
		throws IOException, ClassNotFoundException
	{
		s.defaultReadObject();
		mtab = new MemoryCache();
		ctab = new MemoryCache();
	}
}
