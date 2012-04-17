/*
 * @(#)DynamicRuntime.java 1.7 05/05/09
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.io.IOException;

import org.pnuts.util.*;
import org.pnuts.lang.*;

import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Runtime;

public class DynamicRuntime extends Runtime {
	private final static boolean DEBUG = false;
	private final static Object[] noarg = new Object[]{};

	private CodeLoader codeLoader = null;
	static boolean hasCollection = false;
	static {
		try {
			Class.forName("java.util.Collection");
			hasCollection = true;
		} catch (Exception e){
		}
	}

	/**
	 * call a method
	 */
	protected Object _callMethod(Context context,
				     Class c,
				     String name,
				     Object args[],
				     Class types[],
				     Object target)
	{
		try {
			ClassLoader cl = codeLoader;
			synchronized (this){
				if (cl == null){
					cl = Compiler.createCodeLoader(context.getClassLoader(), true);
					codeLoader = (CodeLoader)cl;
				}
			}
			return callMethod(context, c, name, args, types, target, (CodeLoader)cl);
		} catch (IllegalAccessException e0){
			throw new PnutsException(e0, context);
		} catch (InvocationTargetException e1){
			Throwable t = e1.getTargetException();
			if (t instanceof PnutsException){
				throw (PnutsException)t;
			} else {
				throw new PnutsException(t, context);
			}
		} catch (NoMemberFoundException e2){
			throw new PnutsException("method.notFound",
						 new Object[]{name, c.getName(), Pnuts.format(args)}, context);
		} catch (PnutsException e3){
			throw e3;
		} catch (Throwable e4){
			throw new PnutsException(e4, context);
		}
	}

	Object callMethod(Context context,
			  Class c,
			  String name,
			  Object args[],
			  Class types[],
			  Object target,
			  CodeLoader codeLoader)
		throws IllegalAccessException,
		       IllegalArgumentException,
		       InvocationTargetException
	{
		ProxyCache methodCache = null;

		if (name == CLONE && args.length == 0){
			if (target instanceof Object[]){
				return ((Object[])target).clone();
			} else if (target instanceof int[]){
				return ((int[])target).clone();
			} else if (target instanceof byte[]){
				return ((byte[])target).clone();
			} else if (target instanceof short[]){
				return ((short[])target).clone();
			} else if (target instanceof char[]){
				return ((char[])target).clone();
			} else if (target instanceof long[]){
				return ((long[])target).clone();
			} else if (target instanceof float[]){
				return ((float[])target).clone();
			} else if (target instanceof double[]){
				return ((double[])target).clone();
			} else if (target instanceof boolean[]){
				return ((boolean[])target).clone();
			}
		}

		ProxyCache[] m = methodFinder.getMethods(context, c, name);
		if (m == null){
			return super._callMethod(context, c, name, args, types, target);
		}

		int count = 0;
		int min = Integer.MAX_VALUE;
		Stack methods = new Stack();
		int nargs = args.length;
		cand:
		for (int i = 0; i < m.length; i++){
			Class p[] = m[i].paramTypes;
			if (p.length != nargs){
				continue;
			}
			count = 0;
			for (int j = 0; j < p.length; j++){
				Class pj = p[j];
				if (types != null){
					Class tj = types[j];
					if (tj != null && pj != tj && !pj.isAssignableFrom(tj)){
						continue cand;
					}
				}
				int t = matchType(pj, args[j]);
				if (t < 0){
					continue cand;
				}
				count += t;
			}
			if (count > min){
				continue;
			}

			if ((m[i].type == ProxyCache.STATIC) != (target == null)){
				continue;
			}

			if (count < min){
				methods.removeAllElements();
				methods.push(m[i]);
				min = count;
			} else if (count == min){
				methods.push(m[i]);
				if (DEBUG){
					System.out.println("push " + m[i]);
				}
			}
		}

		Class clazz = c;
		out:
		while (clazz != null){
			int size = methods.size();
			for (int i = 0; i < size; i++){
				methodCache = (ProxyCache)methods.pop();
				if (methodCache.declaringClass == clazz){
					break out;
				}
			}
			clazz = clazz.getSuperclass();
		}

		if (methodCache != null){
			boolean retry = false;
			while (true){
				try {
				if (methodCache.proxy == null || retry){
					methodCache.proxy =
						DynamicProxyFactory.makeProxy(name,
									      methodCache.targetClass,
									      methodCache.returnType,
									      methodCache.paramTypes,
									      methodCache.type,
									      codeLoader);
				}
					if (nargs == 0){
						return methodCache.invoke(target);
					} else {
						return methodCache.invoke(target, args);
					}
				} catch (ClassCastException cce){
					if (!retry){
						codeLoader = Compiler.createCodeLoader(getClassLoader(c), true);
						if (DEBUG){
							System.out.println("retry");
						}
						retry = true;
						continue;
					}
					if (DEBUG){
						System.out.println("use reflection");
					}
					methodFinder.useReflection(c);
					return super._callMethod(context, c, name, args, types, target);
				} catch (LinkageError err){
					if (DEBUG){
						System.out.println(err + " , " + codeLoader);
					}
					if (!retry){
						codeLoader = Compiler.createCodeLoader(getClassLoader(c), true);
						if (DEBUG){
							System.out.println("retry");
						}
						retry = true;
						continue;
					}
					if (DEBUG){
						System.out.println("use reflection");
					}
					methodFinder.useReflection(c);
					return super._callMethod(context, c, name, args, types, target);
				} catch (PnutsException pex){
					throw pex;
				} catch (Throwable t){
					if (t instanceof ClassNotFoundException){
						if (DEBUG){
							System.out.println(t);
						}
						if (DEBUG){
							System.out.println("use reflection");
						}
						methodFinder.useReflection(c);
						return super._callMethod(context, c, name, args, types, target);
					} else {
						throw new PnutsException(t, context);
					}
				}
			}
		} else {
			if (target instanceof Class){
				return callMethod(context, (Class)target, name, args, types, null, codeLoader);
			}
			throw new NoMemberFoundException();
		}
	}

	protected Object _callConstructor(Context context, Class c, Object args[], Class types[]){
		try {
			ClassLoader cl = codeLoader;
			synchronized (this){
				if (cl == null){
					cl = Compiler.createCodeLoader(context.getClassLoader(), true);
					codeLoader = (CodeLoader)cl;
				}
			}
			return _callConstructor(context, c, args, types, (CodeLoader)cl);

		} catch (InvocationTargetException e1){
			Throwable t = ((InvocationTargetException)e1).getTargetException();
			if (t instanceof PnutsException){
				throw (PnutsException)t;
			} else {
				throw new PnutsException(t, context);
			}
		} catch (NoMemberFoundException e2){
			throw new PnutsException("constructor.notFound", new Object[]{c, Pnuts.format(args)}, context);
		} catch (PnutsException e3){
			throw e3;
		} catch (Throwable e4){
			throw new PnutsException(e4, context);
		}
	}

	protected Object _callConstructor(Context context, 
					  Class c,
					  Object args[],
					  Class types[],
					  CodeLoader codeLoader)
		throws IllegalAccessException,
		       IllegalArgumentException,
		       InvocationTargetException,
		       InstantiationException
	{
		ProxyCache cs[] = methodFinder.getConstructors(context, c);
		ProxyCache cons = null;

		if (cs == null){
			return super._callConstructor(context, c, args, types);
		}

		int count = 0;
		int min = Integer.MAX_VALUE;
		cand:
		for (int i = 0; i < cs.length; i++){
			Class p[] = cs[i].paramTypes;
			if (p.length != args.length){
				continue;
			}
			count = 0;
			for (int j = 0; j < p.length; j++){
				Class pj = p[j];
				if (types != null){
					Class tj = types[j];
					if (tj != null && pj != tj && !pj.isAssignableFrom(tj)){
						continue cand;
					}
				}
				int t = matchType(pj, args[j]);
				if (t < 0){
					continue cand;
				}
				count += t;
			}
			if (count < min){
				min = count;
				cons = cs[i];
			}
		}
		if (cons != null){
			boolean retry = false;
			while (true){
				if (cons.proxy == null || retry){
					cons.proxy =
						DynamicProxyFactory.makeProxy("<init>",
									      cons.declaringClass,
									      cons.returnType,
									      cons.paramTypes,
									      ProxyCache.CONSTRUCTOR,
									      codeLoader);
				}
				try {
					if (args.length == 0){
						return cons.invoke(null);
					} else {
						return cons.invoke(null, args);
					}
				} catch (ClassCastException cce){
					if (DEBUG){
						System.out.println(cce);
					}
					if (!retry){
						codeLoader = Compiler.createCodeLoader(getClassLoader(c), true);
						if (DEBUG){
							System.out.println("retry");
						}
						retry = true;
						continue;
					}
					methodFinder.useReflection(c);
					return super._callConstructor(context, c, args, types);
				} catch (LinkageError err){
					if (DEBUG){
						System.out.println(err);
					}
					if (!retry){
						codeLoader = Compiler.createCodeLoader(getClassLoader(c), true);
						if (DEBUG){
							System.out.println("retry");
						}
						retry = true;
						continue;
					}
					methodFinder.useReflection(c);
					return super._callConstructor(context, c, args, types);
				} catch (PnutsException pex){
					throw pex;
				} catch (Throwable t){
					/* jdk1.1.8 throws ClassNotFoundException instead of NoClassDefFoundError */
					if (DEBUG){
						System.out.println(t);
					}
					if (t instanceof ClassNotFoundException || t instanceof IllegalAccessError){
						methodFinder.useReflection(c);
						return super._callConstructor(context, c, args, types);
					} else {
						throw new PnutsException(t, context);
					}
				}
			}
		} else {
			throw new NoMemberFoundException();
		}
	}

	static class ProxyCache {
		final static int DEFAULT = 0;
		final static int STATIC = 1;
		final static int CONSTRUCTOR = 2;

		Class[] paramTypes;
		Class declaringClass;
		Class targetClass;
		Class returnType;
		int type;
		DynamicProxy proxy;
		boolean hasArrayParam;

		ProxyCache(Method method, Class clazz){
			this.paramTypes = method.getParameterTypes();
			this.declaringClass = method.getDeclaringClass();
			this.returnType = method.getReturnType();
			this.type = Modifier.isStatic(method.getModifiers()) ? STATIC : DEFAULT;
			this.targetClass = clazz;
			init();
		}

		ProxyCache(Constructor cons){
			this.paramTypes = cons.getParameterTypes();
			this.declaringClass = cons.getDeclaringClass();
			this.targetClass = cons.getDeclaringClass();
			this.returnType = void.class;
			this.type = CONSTRUCTOR;
			init();
		}

		void init(){
			Class[] types = this.paramTypes;
			for (int i = 0; i < types.length; i++){
				if (types[i].isArray()){
					hasArrayParam = true;
					break;
				}
			}
		}

		Object invoke(Object target){
			return proxy.invoke(target);
		}

		Object invoke(Object target, Object[] args){
			if (hasArrayParam){
				for (int i = 0; i < paramTypes.length; i++){
					Class type = paramTypes[i];
					Object arg = args[i];
					if (arg != null && type.isArray() && !type.isInstance(arg)){
						args[i] = transform(type, arg);
					}
				}
			}
			return proxy.invoke(target, args);
		}
	}

	static abstract class MethodFinder {
		public abstract ProxyCache[] getConstructors(Context context, Class c);
		public abstract ProxyCache[] getMethods(Context context, Class c, String name);
		public abstract void useReflection(Class c);
	}

	static class Java2MethodFinder extends MethodFinder {
		private Map mtab = Runtime.createWeakMap();
		private Map ctab = Runtime.createWeakMap();

		private static java.lang.ref.SoftReference USE_REFLECTION =
			new java.lang.ref.SoftReference(Runtime.createWeakMap());

		public void useReflection(Class c){
			if (DEBUG){
				System.out.println("use reflection for " + c.getName());
			}
			mtab.put(c, USE_REFLECTION);
			ctab.put(c, USE_REFLECTION);
		}

		public ProxyCache[] getMethods(Context context, Class c, String name){
			java.lang.ref.SoftReference cache = (java.lang.ref.SoftReference)mtab.get(c);
			Map map;
			if (cache == null || (map = (Map)cache.get()) == null){
				map = Runtime.createWeakMap();
				mtab.put(c, new java.lang.ref.SoftReference(map));
			} else if (cache == USE_REFLECTION){
				return null;
			}
			Object v = map.get(name);
			if (v instanceof ProxyCache[]){
				return (ProxyCache[])v;
			} else {
				if (DEBUG){
					System.out.println(c + ":" + name);
				}
//				boolean isPublic = Modifier.isPublic(c.getModifiers());

				Method m[] = Runtime.getMethods(context, c);
				if (m == null){ // for Bug:4137722
					throw new NoClassDefFoundError("" + c);
				}
				int j = 0;
				for (int i = 0; i < m.length; i++){
					String m_name = m[i].getName();
					if (m_name.equals(name) && i >= j){
						m[j] = m[i];
						j++;
					}
				}
				ProxyCache px[] = new ProxyCache[j];
				for (int i = 0; i < j; i++){
					Method mi = findCallableMethod(c, m[i].getName(), m[i].getParameterTypes());
					if (mi != null){
						px[i] = new ProxyCache(mi, c);
					} else {
						px[i] = new ProxyCache(m[i], c);
					}
				}
				map.put(name, px);
				return px;
			}
		}

		public ProxyCache[] getConstructors(Context context, Class c){

			java.lang.ref.SoftReference cache = (java.lang.ref.SoftReference)ctab.get(c);
			ProxyCache[] pc;
			if (cache == USE_REFLECTION){
				return null;
			} else if (cache == null || (pc = (ProxyCache[])cache.get()) == null){
				Constructor con[] = Runtime.getConstructors(context, c);
				ProxyCache px[] = new ProxyCache[con.length];
				for (int i = 0; i < con.length; i++){
					px[i] = new ProxyCache(con[i]);
				}
				ctab.put(c, new java.lang.ref.SoftReference(px));
				return px;
			} else {
				return pc;
			}
		}
	}

	/**
	 * This method maps a proxy object of a Constructor to a PnutsFunction.
	 * Call of the resulting function is faster than reflection API calls.
	 *
	 * @param cons	a constructor
	 * @return	an instance the constructor creates.
	 */
	public static PnutsFunction makeProxy(Constructor cons) {
		final DynamicProxy px =
			DynamicProxyFactory.makeProxy(cons,
						      Compiler.createCodeLoader(getClassLoader(cons.getDeclaringClass()), true));

		if (cons.getParameterTypes().length == 0){
			return new PnutsFunction(){
					public Object exec(Object[] args, Context context){
						return px.invoke(null);
					}
				};
		} else {
			return new PnutsFunction(){
					public Object exec(Object[] args, Context context){
						return px.invoke(null, args);
					}
				};
		}
	}

	/**
	 * This method maps a proxy object of a Method to a PnutsFunction.
	 * Call of the resulting function is expected to be faster than
	 * reflection API calls.
	 *
	 * @param method	a method
	 * @return	the result of the 'method' call.
	 */
	public static PnutsFunction makeProxy(Method method) {
		final DynamicProxy px =
			DynamicProxyFactory.makeProxy(method,
						      Compiler.createCodeLoader(getClassLoader(method.getDeclaringClass()), true));
		final String _name = method.getName();
		boolean _static  = Modifier.isStatic(method.getModifiers());
		int nargs = method.getParameterTypes().length;

		if (_static){
			if (nargs == 0){
				return new PnutsFunction(){
						public String getName(){
							return _name;
						}
						public Object exec(Object[] args, Context context){
							return px.invoke(null);
						}
					};
			} else {
				return new PnutsFunction(){
						public String getName(){
							return _name;
						}
						public Object exec(Object[] args, Context context){
							return px.invoke(null, args);
						}
					};
			}
		} else {
			if (nargs == 0){
				return new PnutsFunction(){
						public String getName(){
							return _name;
						}
						public Object exec(Object[] args, Context context){
							return px.invoke(args[0]);
						}
					};
			} else {
				return new PnutsFunction(){
						public String getName(){
							return _name;
						}
						public Object exec(Object[] args, Context context){
							Object target = args[0];
							System.arraycopy(args, 1, args, 0, args.length - 1);
							return px.invoke(target, args);
						}
					};
			}
		}
	}

	static ClassLoader getClassLoader(final Class clazz){
		if (Compiler.hasJava2Security){
			return (ClassLoader)AccessController.doPrivileged(new PrivilegedAction(){
					public Object run(){
						return clazz.getClassLoader();
					}
				});
		} else {
			return clazz.getClassLoader();
		}
	}

	static class NoMemberFoundException extends RuntimeException { }

	static MethodFinder methodFinder = new Java2MethodFinder();

	private Map beanAccessors = Runtime.createWeakMap();
	
	static class BeanInfoParam {
		Class targetClass;

		Class stopClass;

		BeanInfoParam(Class targetClass, Class stopClass) {
			this.targetClass = targetClass;
			this.stopClass = stopClass;
		}

		public int hashCode() {
			return targetClass.hashCode() ^ stopClass.hashCode();
		}

		public boolean equals(Object that) {
			if (that instanceof BeanInfoParam) {
				BeanInfoParam p = (BeanInfoParam) that;
				return p.targetClass == this.targetClass
					&& p.stopClass == this.stopClass;
			}
			return false;
		}
	}

	private Accessor getAccessor(Class cls, Class stopClass) {
		Object key;
		if (stopClass == null) {
			key = cls;
		} else {
			key = new BeanInfoParam(cls, stopClass);
		}
		java.lang.ref.SoftReference ref = (java.lang.ref.SoftReference) beanAccessors
			.get(key);
		if (ref == null) {
			Accessor a = createBeanAccessor(cls, stopClass);
			beanAccessors.put(key, new java.lang.ref.SoftReference(a));
			return a;
		} else {
			Accessor a = (Accessor) ref.get();
			if (a == null) {
				a = createBeanAccessor(cls, stopClass);
				beanAccessors.put(key, new java.lang.ref.SoftReference(a));
			}
			return a;
		}
	}

	private Accessor createBeanAccessor(Class cls, Class stopClass){
		return new DynamicAccessor(cls, stopClass);
	}

	protected static class DynamicAccessor extends pnuts.lang.Runtime.Accessor {
		boolean isPublic;

		protected DynamicAccessor(Class cls, Class stopClass){
			super(cls, stopClass);
			this.isPublic = Modifier.isPublic(cls.getModifiers());
		}

		public void addReadMethod(String name, Object method){
			Method m = (Method)method;
			Method m0 = m;
			if (!isPublic){
				String methodName = m.getName();
				Class[] types = m.getParameterTypes();
				m = findCallableMethod(beanClass, methodName, types);
			}
			if (m != null){
				CodeLoader loader =
					Compiler.createCodeLoader(getClassLoader(m.getDeclaringClass()), true);
				DynamicProxy px = DynamicProxyFactory.makeProxy(m, loader);
				ProxyCache pc = new ProxyCache(m, beanClass);
				pc.proxy = px;
				super.addReadMethod(name, pc);
			} else {
				DynamicProxy px = createReflectionProxy(beanClass, name, stopClass);
				ProxyCache pc = new ProxyCache(m0, beanClass);
				pc.proxy = px;
				super.addReadMethod(name, pc);
				super.addWriteMethod(name, pc);
			}
		}

		public void addWriteMethod(String name, Object method){
			Method m = (Method)method;
			Method m0 = m;
			if (!isPublic){
				String methodName = m.getName();
				Class[] types = m.getParameterTypes();
				m = findCallableMethod(beanClass, methodName, types);
			}
			if (m != null){
				CodeLoader loader =
					Compiler.createCodeLoader(getClassLoader(m.getDeclaringClass()), true);
				DynamicProxy px = DynamicProxyFactory.makeProxy(m, loader);
				ProxyCache pc = new ProxyCache(m, beanClass);
				pc.proxy = px;
				super.addWriteMethod(name, pc);
			} else {
				DynamicProxy px = createReflectionProxy(beanClass, name, stopClass);
				ProxyCache pc = new ProxyCache(m0, beanClass);
				pc.proxy = px;
				super.addReadMethod(name, pc);
				super.addWriteMethod(name, pc);
			}
		}
	}

	static void createMethodMap(ObjectDesc d, final Map readMethods, final Map writeMethods){
		d.handleProperties(new PropertyHandler(){
			public void handle(String propertyName, Class type, Method readMethod, Method writeMethod){
				if (readMethod != null){
					readMethods.put(propertyName, readMethod);
				}
				if (writeMethod != null){
					writeMethods.put(propertyName, writeMethod);
				}
			}
		    });
	}

	static DynamicProxy createReflectionProxy(Class cls, String name, Class stopClass){
		ObjectDesc od = ObjectDescFactory.getDefault().create(cls, stopClass);
		Map rmap = new HashMap();
		Map wmap = new HashMap();
		createMethodMap(od, rmap, wmap);
		Method w = (Method)rmap.get(name);
		Method r = (Method)wmap.get(name);
		if (r != null){
			r.setAccessible(true);
		}
		if (w != null){
			w.setAccessible(true);
		}
		final Method readMethod = r;
		final Method writeMethod = w;
		DynamicProxy px = new DynamicProxy(){
			public Object invoke(Object target){
				try {
					return readMethod.invoke(target, noarg);
				} catch (IllegalAccessException iae){
				} catch (InvocationTargetException ite){
				}
				return null;
			}
			public Object invoke(Object target, Object[] args){
				try {
					writeMethod.invoke(target, args);
				} catch (IllegalAccessException iae){
				} catch (InvocationTargetException ite){
				}
				return null;
			}
		    };
		return px;
	}

	/**
	 * Gets a Bean property of the specified bean.
	 *
	 * @param target the target bean
	 * @param name the Bean property name
	 */
	public Object getBeanProperty(Object target, String name)
		throws IllegalAccessException
	{	
		return getBeanProperty(target, name, null);
	}

	/**
	 * Gets a Bean property of the specified bean.
	 *
	 * @param target the target bean
	 * @param name the Bean property name
	 * @param stopClass the Introspector's "stopClass"
	 */
	protected Object getBeanProperty(Object target, String name, Class stopClass)
		throws IllegalAccessException
	{
		pnuts.lang.Runtime.Accessor a = getAccessor(target.getClass(), stopClass);
		ProxyCache readMethodProxy = (ProxyCache)a.findReadMethod(name);
		if (readMethodProxy != null){
			return readMethodProxy.invoke(target);
		}
		if (target instanceof Class){
			Class cls = (Class)target;
			Field field = getField(cls, name);
			if (field != null && Modifier.isStatic(field.getModifiers())){
				return field.get(null);
			}
		}
		if (a.findWriteMethod(name) == null){
		    Class cls = target.getClass();
		    Field field = getField(cls, name);
		    if (field != null && !Modifier.isStatic(field.getModifiers())) {
			return field.get(target);
		    }
		}
		throw new IllegalArgumentException("not readable property: target=" + target + ", fieldName=" + name);
	}

	/**
	 * Sets a Bean property of the specified bean.
	 *
	 * @param target the target bean
	 * @param name the Bean property name
	 * @param value the new property value
	 */
	public void setBeanProperty(Object target, String name, Object value)
	    throws IllegalAccessException, InvocationTargetException {
		setBeanProperty(target, name, value, null);
	}

	/**
	 * Sets a Bean property of the specified bean.
	 *
	 * @param target the target bean
	 * @param name the Bean property name
	 * @param value the new property value
	 * @param stopClass the Introspector's "stopClass"
	 */
	protected void setBeanProperty(Object target, String name, Object value, Class stopClass)
	    throws IllegalAccessException, InvocationTargetException 
	{
		pnuts.lang.Runtime.Accessor a = getAccessor(target.getClass(), stopClass);
		ProxyCache writeMethodProxy = (ProxyCache)a.findWriteMethod(name);
		if (writeMethodProxy != null){
			try {
				Object[] arg = new Object[]{value};
				writeMethodProxy.invoke(target, arg);
				return;
			} catch (ClassCastException cce){
			    try {
				super.setBeanProperty(target, name, value, stopClass);
				return;
			    } catch (Exception e){
				Class type = getBeanPropertyType(target.getClass(), name);

				String msg = getMessage("pnuts.lang.pnuts", 
							"type.mismatch",
							new Object[] {
								type,
								target,
								name,
								value,
								value.getClass().getName()
							});
				throw new IllegalArgumentException(msg);
			    }
			}
		}
		if (target instanceof Class){
			Class cls = (Class) target;
			Field field = getField(cls, name);
			if (field != null && Modifier.isStatic(field.getModifiers())) {
				field.set(null, value);
				return;
			}
		}
		if (a.findReadMethod(name) == null){
		    Class cls = target.getClass();
		    Field field = getField(cls, name);
		    if (field != null && !Modifier.isStatic(field.getModifiers())) {
			field.set(target, value);
			return;
		    }
		}
		throw new IllegalArgumentException("not writable property: target=" + target + ", fieldName=" + name);
	}

	/**
	 * Gets the type of a bean property
	 *
	 * @param cls the class of the bean
	 * @param name the property name of the bean property
	 * @return the type of the property
	 */
	public Class getBeanPropertyType(Class cls, String name){
		return getAccessor(cls, null).getType(name);
	}

	protected Object _getField(Context context,
				     Class c,
				     String name,
				     Object target)
	{
		try {
			ClassLoader cl = codeLoader;
			synchronized (this){
				if (cl == null){
					cl = Compiler.createCodeLoader(context.getClassLoader(), true);
					codeLoader = (CodeLoader)cl;
				}
			}
			return _getField(context, c, name, target, (CodeLoader)cl);
		} catch (NoSuchFieldException nsf){
			throw new PnutsException("field.notFound", new Object[]{name, target}, context);
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}

	protected void _putField(Context context,
				     Class c,
				     String name,
				     Object target,
				 Object value)
	{
		try {
			ClassLoader cl = codeLoader;
			synchronized (this){
				if (cl == null){
					cl = Compiler.createCodeLoader(context.getClassLoader(), true);
					codeLoader = (CodeLoader)cl;
				}
			}
			_putField(context, c, name, target, value, (CodeLoader)cl);
		} catch (Exception e){
		    throw new PnutsException(e, context);
		}
	}

	Object _getField(Context context,
			 Class c,
			 String name,
			 Object target,
			 CodeLoader codeLoader)
	    throws NoSuchFieldException, InstantiationException, IOException, IllegalAccessException
	{
	    FieldAccessor fa = (FieldAccessor)fieldCache.get(c);
	    if (fa == null){
		if (target instanceof Class){
		    fa = FieldAccessorGenerator.generate(name, (Class)target, codeLoader, true);
		} else {
		    fa = FieldAccessorGenerator.generate(name, c, codeLoader, false);
		}
		fieldCache.put(c, fa);
	    }
	    return fa.get(target);
	}

	void _putField(Context context,
			 Class c,
			 String name,
			 Object target,
		       Object value,
			 CodeLoader codeLoader)
	    throws NoSuchFieldException, InstantiationException, IOException, IllegalAccessException
	{
	    FieldAccessor fa = (FieldAccessor)fieldCache.get(c);
	    if (fa == null){
		if (target instanceof Class){
		    fa = FieldAccessorGenerator.generate(name, (Class)target, codeLoader, true);
		} else {
		    fa = FieldAccessorGenerator.generate(name, c, codeLoader, false);
		}
		fieldCache.put(c, fa);
	    }
	    fa.set(target, value);
	}
}
