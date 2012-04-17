/*
 * @(#)Java2Configuration.java 1.3 05/05/09
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import pnuts.lang.Package;

/**
 * This class define the interface of runtime configuration, such as how to find
 * method/field candidates, how to get the field value, how to get indexed
 * elements, and so on. This class also provides the default implmentation for
 * Java2 of this interface.
 * 
 * @see pnuts.lang.Configuration
 */
class Java2Configuration extends JavaBeansConfiguration {

	private final static boolean DEBUG = false;

	static final long serialVersionUID = -4966352283575106111L;
	
	Java2Configuration() {
	}

	Java2Configuration(Class stopClass) {
		super(stopClass);
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
		} else if (target instanceof Generator) {
			return fieldGenerator((Generator) target, name, context);
		} else {
			return super.getField(context, target, name);
		}
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
	public void putField(final Context context, Object target, String name,
			final Object value) {
		if (target instanceof Context) {
			((Context) target).set(name, value);
		} else if (target instanceof Property) {
			((Property) target).set(name, value, context);
		} else if (target instanceof Map) {
			((Map) target).put(name, value);
		} else if (target instanceof Generator) {
			Generator g = (Generator) target;
			final String fieldName = name;
			g.apply(new PnutsFunction() {
				protected Object exec(Object[] args, Context c) {
					putField(context, args[0], fieldName, value);
					return null;
				}
			}, context);
		} else {
			super.putField(context, target, name, value);
		}
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
	public void putStaticField(Context context, Class clazz, String name,
			Object value) {
		try {
			Field field = getField(clazz, name);
			Class type = field.getType();
			if (type.isArray() && value != null && Runtime.isArray(value)) {
				if (!type.isInstance(value)) {
//					value = Runtime.transform(Runtime.getBottomType(type), value);
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
			if (target instanceof AbstractData) {
				return ((AbstractData) target).invoke(name, args, context);
			}
			return super.callMethod(context, c, name, args, types, target);
		} catch (PnutsException e1) {
			throw e1;
		} catch (Throwable e2) {
			throw new PnutsException(e2, context);
		}
	}

	static class ObjectArrayEnum implements Enumeration {
		Object[] array;
		int idx;
		int len;
		
		ObjectArrayEnum(Object[] array) {
			this.array = array;
			this.idx = 0;
			this.len = array.length;
		}

		public boolean hasMoreElements() {
			return idx < len;
		}

		public Object nextElement() {
			return array[idx++];
		}
	}
	
	static class ArrayEnum implements Enumeration {
		Object array;

		int idx;

		int len;

		ArrayEnum(Object array) {
			this.array = array;
			this.idx = 0;
			this.len = Array.getLength(array);
		}

		public boolean hasMoreElements() {
			return idx < len;
		}

		public Object nextElement() {
			return Array.get(array, idx++);
		}
	}

	static class ItrEnum implements Enumeration {
		Iterator itr;

		ItrEnum(Iterator itr) {
			this.itr = itr;
		}

		public boolean hasMoreElements() {
			return itr.hasNext();
		}

		public Object nextElement() {
			return itr.next();
		}
	}

	static class StringEnum implements Enumeration {
		String str;

		int len;

		int pos = 0;

		StringEnum(String str) {
			this.str = str;
			this.len = str.length();
		}

		public boolean hasMoreElements() {
			return len > pos;
		}

		public Object nextElement() {
			return new Character(str.charAt(pos++));
		}
	}

	/**
	 * Convert an object to Enumeration. This method is used by foreach
	 * statements. Subclasses can override this method to customize the behavior
	 * of foreach statements.
	 */
	public Enumeration toEnumeration(Object obj) {
		if (obj instanceof Enumeration) {
			return (Enumeration) obj;
		} else if (obj instanceof Iterator) {
			return new ItrEnum((Iterator) obj);
		} else if (obj instanceof Object[]){
			return new ObjectArrayEnum((Object[])obj);
		} else if (obj instanceof Collection) {
			return Collections.enumeration((Collection) obj);
		} else if (obj instanceof Map) {
			return new ItrEnum(((Map) obj).entrySet().iterator());
		} else if (Runtime.isArray(obj)) {
			return new ArrayEnum(obj);
		} else if (obj instanceof String) {
			return new StringEnum((String) obj);
		} else {
			return null;
		}
	}

	public Callable toCallable(Object obj) {
		return null;
	}


	public Object getElement(Context context, final Object target, Object key) {
		if (target instanceof Object[]) {
			if (key instanceof Number) {
				return ((Object[]) target)[((Number) key).intValue()];
			} else if (key instanceof PnutsFunction) {
				return filterGenerator((Object[]) target, (PnutsFunction) key,
						context);
			}
		} else if (target instanceof Indexed) {
			if (key instanceof Number) {
				return ((Indexed) target).get(((Number) key).intValue());
			} else if (key instanceof String) {
				return getBeanProperty(context, target, (String) key);
			}
		} else if (target instanceof Package) {
			return ((Property) target).get(((String) key).intern(), context);
		} else if (target instanceof Property) {
			return ((Property) target).get((String) key, context);
		} else if (target instanceof Map) {
			return ((Map) target).get(key);
		} else if (target instanceof Context) {
			return ((Context) target).get(((String) key).intern());
		} else if (target instanceof Generator) {
			if (key instanceof Number) {
				return generateNth((Generator) target, ((Number) key)
						.intValue(), context);
			} else if (key instanceof PnutsFunction) {
				return filterGenerator((Generator) target, (PnutsFunction) key,
						context);
			}
		} else {
			if (key instanceof PnutsFunction) {
				if (target instanceof Collection) {
					return filterGenerator((Collection) target,
							(PnutsFunction) key, context);
				} else if ((target instanceof int[])
						|| (target instanceof byte[])
						|| (target instanceof short[])
						|| (target instanceof char[])
						|| (target instanceof long[])
						|| (target instanceof float[])
						|| (target instanceof double[])
						|| (target instanceof boolean[])) {
					return filterGenerator((Object) target,
							(PnutsFunction) key, context);
				}
			} else if (key instanceof String) {
				return getBeanProperty(context, target, (String) key);
			} else if (key instanceof Number) {
				if (target instanceof List) {
					return ((List) target).get(((Number) key).intValue());
				} else if (target instanceof String) {
					return new Character(((String) target)
							.charAt(((Number) key).intValue()));
				} else if (target instanceof int[]) {
					return new Integer(((int[]) target)[((Number) key)
							.intValue()]);
				} else if (target instanceof byte[]) {
					return new Byte(
							((byte[]) target)[((Number) key).intValue()]);
				} else if (target instanceof char[]) {
					return new Character(((char[]) target)[((Number) key)
							.intValue()]);
				} else if (target instanceof float[]) {
					return new Float(((float[]) target)[((Number) key)
							.intValue()]);
				} else if (target instanceof double[]) {
					return new Double(((double[]) target)[((Number) key)
							.intValue()]);
				} else if (target instanceof boolean[]) {
					return Boolean.valueOf(((boolean[]) target)[((Number) key).intValue()]);
				} else if (target instanceof long[]) {
					return new Long(
							((long[]) target)[((Number) key).intValue()]);
				} else if (target instanceof short[]) {
					return new Short(((short[]) target)[((Number) key)
							.intValue()]);
				}
			}
		}
		throw new IllegalArgumentException(String.valueOf(key));
	}

	public void setElement(Context context, Object target, Object key,
			Object expr) {
		if (target instanceof Object[]) {
			Object[] array = (Object[])target;
			int idx = ((Number)key).intValue();
			if (idx < 0 && idx >= -array.length){
			    idx += array.length;
			}
			array[idx] = expr;
		} else if (target instanceof Indexed) {
			if (key instanceof Number) {
				((Indexed) target).set(((Number) key).intValue(), expr);
			} else if (key instanceof String) {
				setBeanProperty(context, target, (String) key, expr);
			} else {
				throw new IllegalArgumentException(String.valueOf(key));
			}
		} else if (target instanceof Package) {
			((Property) target).set(((String) key).intern(), expr, context);
		} else if (target instanceof Property) {
			((Property) target).set((String) key, expr, context);
		} else if (target instanceof Map) {
			((Map) target).put(key, expr);
		} else if (target instanceof Context) {
			((Context) target).set(((String) key).intern(), expr);
		} else {
			int idx = ((Number)key).intValue();
			if (target instanceof List) {
				List list = (List)target;
				int sz = list.size();
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				list.set(idx, expr);
			} else if (target instanceof int[]) {
				int[] array = (int[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Number) expr).intValue();
			} else if (target instanceof byte[]) {
				byte[] array = (byte[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Number) expr).byteValue();
			} else if (target instanceof char[]) {
				char[] array = (char[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Character) expr).charValue();
			} else if (target instanceof float[]) {
				float[] array = (float[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Number) expr).floatValue();
			} else if (target instanceof double[]) {
				double[] array = (double[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Number) expr).doubleValue();
			} else if (target instanceof boolean[]) {
				boolean[] array = (boolean[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Boolean) expr).booleanValue();
			} else if (target instanceof long[]) {
				long[] array = (long[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Number) expr).longValue();
			} else if (target instanceof short[]) {
				short[] array = (short[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}
				array[idx] = ((Number) expr).shortValue();
			} else {
				if (key instanceof String) {
					setBeanProperty(context, target, (String) key, expr);
				} else {
					throw new IllegalArgumentException(String.valueOf(key));
				}
			}
		}
	}

	static void setElements(Object target, int from, int to, Object expr) {
		if (target instanceof Object[]) {
			Object[] array = (Object[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = expr;
			}
		} else if (target instanceof Indexed) {
			Indexed indexed = (Indexed) target;
			for (int i = from; i <= to; i++) {
				indexed.set(i, expr);
			}
		} else if (target instanceof List) {
			List list = (List) target;
			for (int i = from; i <= to; i++) {
				list.set(i, expr);
			}
		} else if (target instanceof int[]) {
			int[] array = (int[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Number) expr).intValue();
			}
		} else if (target instanceof byte[]) {
			byte[] array = (byte[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Number) expr).byteValue();
			}
		} else if (target instanceof char[]) {
			char[] array = (char[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Character) expr).charValue();
			}
		} else if (target instanceof float[]) {
			float[] array = (float[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Number) expr).floatValue();
			}
		} else if (target instanceof double[]) {
			double[] array = (double[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Number) expr).doubleValue();
			}
		} else if (target instanceof boolean[]) {
			boolean[] array = (boolean[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Boolean) expr).booleanValue();
			}
		} else if (target instanceof long[]) {
			long[] array = (long[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Number) expr).longValue();
			}
		} else if (target instanceof short[]) {
			short[] array = (short[]) target;
			for (int i = from; i <= to; i++) {
				array[i] = ((Number) expr).shortValue();
			}
		} else {
			for (int i = from; i <= to; i++) {
				Array.set(target, i, expr);
			}
		}
	}

	/**
	 * Defines the semantices of an expression like:
	 * 
	 * <pre>
	 * target[idx1..idx2]
	 * </pre>
	 * 
	 * @param context
	 *            the context
	 * @param target
	 *            the target object
	 * @param idx1
	 *            the start index
	 * @param idx2
	 *            the end index, which can be null
	 */
	public Object getRange(Context context, Object target, Object idx1,
			Object idx2) {
		int from = ((Number) idx1).intValue();
		int to = -1;
		if (idx2 != null) {
			to = ((Number) idx2).intValue();
		}
		if (target instanceof String) {
			String s = (String) target;
			int len = s.length();
			if (from > len - 1) {
				return "";
			}
			if (idx2 != null) {
				if (from > to || to < 0) {
					return "";
				}
				if (from < 0) {
					from = 0;
				}
				if (to > len - 1) {
					to = len - 1;
				}
				return s.substring(from, to + 1);
			} else {
				if (from < 0) {
					from = 0;
				}
				return s.substring(from);
			}
		} else if (Runtime.isArray(target)) {
			Class c = target.getClass().getComponentType();
			int len = Runtime.getArrayLength(target);
			if (from > len - 1) {
				return Array.newInstance(c, 0);
			}
			if (idx2 == null) {
				to = len - 1;
			} else {
				if (from > to || to < 0) {
					return Array.newInstance(c, 0);
				}
				if (to > len - 1) {
					to = len - 1;
				}
			}
			if (from < 0) {
				from = 0;
			}
			int size = to - from + 1;
			if (size < 0) {
				size = 0;
			} else if (from + size > len) {
				size = len - from;
			}
			Object ret = Array.newInstance(c, size);
			if (size > 0) {
				System.arraycopy(target, from, ret, 0, size);
			}
			return ret;
		} else if (target instanceof List) {
			List list = (List) target;
			try {
				int size = list.size();
				if (from < 0) {
					from = 0;
				} else if (from > size) {
					from = size;
				}
				if (idx2 == null) {
					to = size;
				} else {
					if (from > to || to < 0) {
						to = from;
					} else {
						to++;
						if (to > size) {
							to = size;
						}
					}
				}
			} catch (Exception e){
				// allow size() to throw an exception
			}
			return list.subList(from, to);
		} else if (target instanceof Generator) {
			if (idx2 != null) {
				if (from > to || to < 0) {
					from = -1;
					to = -1;
				} else if (from < 0) {
					from = 0;
				}
			} else {
				to = -1;
				if (from < 0) {
					from = 0;
				}
			}
			return new Runtime.RangeGenerator((Generator) target, from, to);
		} else {
			throw new PnutsException("illegal.type", new Object[]{target},
					context);
		}
	}

	/**
	 * Defines the semantices of an expression like:
	 * <p>
	 * 
	 * <pre>
	 * target[idx1..idx2] = expr
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param context
	 *            the context in which the assignment is done
	 * @param target
	 *            the target object
	 * @param idx1
	 *            the start index
	 * @param idx2
	 *            the end index, which can be null
	 * @param expr
	 *            the new value of the indexed element
	 */
	public Object setRange(Context context, Object target, Object idx1,
			Object idx2, Object expr) {
		int from = ((Number) idx1).intValue();
		int to = -1;
		if (idx2 != null) {
			to = ((Number) idx2).intValue();
			if (from > to || to < 0) {
				return target;
			}
		}
		if (target instanceof String) {
			StringBuffer s = new StringBuffer((String) target);
			int len = s.length();
			if (from > len - 1) {
				return target;
			}
			if (idx2 == null) {
				to = len - 1;
			} else {
				if (to > len - 1) {
					to = len - 1;
				}
			}
			if (from < 0) {
				from = 0;
			}
			if (expr instanceof Character) {
				char c = ((Character) expr).charValue();
				for (int i = from; i < to + 1; i++) {
					s.setCharAt(i, c);
				}
			} else {
				replace(s, from, to + 1, String.valueOf(expr));
			}
			String val = s.toString();
			return val;
		} else if (Runtime.isArray(target)) {
			int len = Runtime.getArrayLength(target);
			if (from > len - 1) {
				return target;
			}
			if (idx2 == null) {
				to = len - 1;
			} else {
				if (to > len - 1) {
					to = len - 1;
				}
			}
			if (from < 0) {
				from = 0;
			}
			int size = to - from + 1;
			if (from + size > len) {
				size = len - from;
			}
			if (target instanceof char[] && expr instanceof String) {
				String str = (String) expr;
				int end = str.length();
				if (end > to - from + 1) {
					end = to - from + 1;
				}
				str.getChars(0, end, (char[]) target, from);
			} else {
				setElements(target, from, to, expr);
			}
			return target;
		} else if (target instanceof List) {
			List list = (List) target;
			try {
				int size = list.size();
				if (from < 0) {
					from = 0;
				} else if (from > size) {
					from = size;
				}
				if (idx2 == null) {
					to = size;
				} else {
					if (from > to || to < 0) {
						to = from;
					} else {
						to++;
						if (to > size) {
							to = size;
						}
					}
				}
			} catch (Exception e){
				// allow size() to throw an exception
			}
			for (int i = from; i < to; i++) {
				list.set(i, expr);
			}
			return target;
		} else {
			throw new PnutsException("illegal.type", Runtime.NO_PARAM,
					context);
		}
	}

	Object reInvoke(IllegalAccessException t, final Method method,
			Object target, Object[] args) throws IllegalAccessException,
			InvocationTargetException {
		if (DEBUG) {
			System.out.println("setAccessible " + method.getName());
		}
		method.setAccessible(true);
		return method.invoke(target, args);
	}

	void replace(StringBuffer buf, int from, int to, String str) {
		buf.replace(from, to, str);
	}

	protected ClassLoader getInitialClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	Generator fieldGenerator(final Generator g, String name, Context context) {
		final String fieldName = name;
		return new Generator() {
			public Object apply(final PnutsFunction closure,
					final Context context) {
				g.apply(new PnutsFunction() {
					protected Object exec(Object[] args, Context ctx) {
						closure.call(new Object[] { Runtime.getField(context,
								args[0], fieldName) }, context);
						return null;
					}
				}, context);
				return null;
			}
		};
	}

	static Generator filterGenerator(Object array, PnutsFunction pred, Context context) {
//		return filterGenerator((Object[])Runtime.transform(Object.class, array), pred,context);
		return filterGenerator((Object[])Runtime.transform(Object.class, array, context), pred,context);
	}

	static Generator filterGenerator(final Object[] array,
			final PnutsFunction pred, final Context context) {
		return new Generator() {
			public Object apply(final PnutsFunction closure, final Context c) {
				int len = array.length;
				for (int i = 0; i < len; i++) {
					Object[] args = new Object[] { array[i] };
					if (((Boolean) pred.call(args, /*context*/c)).booleanValue()) {
						closure.call(args, c);
					}
				}
				return null;
			}
		};
	}

	static Generator filterGenerator(final Collection collection,
			final PnutsFunction pred, final Context context) {
		return new Generator() {
			public Object apply(final PnutsFunction closure, final Context c) {
				for (Iterator it = collection.iterator(); it.hasNext();) {
					Object[] args = new Object[] { it.next() };
					if (((Boolean) pred.call(args, c)).booleanValue()) {
						closure.call(args, c);
					}
				}
				return null;
			}
		};
	}

	static Generator filterGenerator(final Generator g,
			final PnutsFunction pred, Context context) {
		return new Generator() {
			public Object apply(final PnutsFunction closure,
					final Context context) {
				g.apply(new PnutsFunction() {
					protected Object exec(Object[] args, Context ctx) {
						if (((Boolean) pred.call(args, ctx)).booleanValue()) {
							closure.call(args, ctx);
						}
						return null;
					}
				}, context);
				return null;
			}
		};
	}

	static Object generateNth(Generator g, int idx, Context context) {
		if (idx < 0) {
			return null;
		}
		Counter c = new Counter(idx);
		try {
			g.apply(c, context);
		} catch (CounterEscape esc) {
			return esc.getValue();
		}
		return null;
	}

	static class Counter extends PnutsFunction {
		int n;

		Counter(int n) {
			this.n = n;
		}

		protected Object exec(Object[] args, Context c) {
			if (n > 0) {
				n--;
			} else {
				throw new CounterEscape(args[0]);
			}
			return null;
		}
	}

        static class CounterEscape extends Escape {
	    Object value;

	    CounterEscape(Object value){
		this.value = value;
	    }

	    public Object getValue(){
		return value;
	    }
        }
}
