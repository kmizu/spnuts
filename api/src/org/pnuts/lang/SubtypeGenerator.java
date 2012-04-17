/*
 * SubtypeGenerator.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Hashtable;

import pnuts.compiler.Compiler;
import pnuts.compiler.ClassFile;
import pnuts.compiler.Constants;
import pnuts.compiler.Label;
import pnuts.compiler.MultiClassLoader;
import pnuts.compiler.Opcode;
import pnuts.compiler.ClassGenerator;
import pnuts.lang.Context;
import pnuts.lang.Function;
import pnuts.lang.Package;
import pnuts.lang.Pnuts;
import pnuts.lang.SimpleNode;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Runtime;

/**
 * This class provides a way to extends a Java class in Pnuts.
 */
public class SubtypeGenerator extends Runtime {

	private final static boolean DEBUG = false;

	public final static int THIS = 0x0001;
	public final static int SUPER = 0x0002;

	private final static String SUPER_PROXY_NAME = "pnuts.compiler.ClassGenerator$SuperCallProxy";

	protected SubtypeGenerator() {
	}

	/**
	 * Generates a class that extends the superClass and implements the
	 * interfaces. Functions in the pkg are mapped to non-final/static
	 * public/protected methods with the same name and the same number of
	 * parameters. Note that this method may throw ClassFormatError.
	 * 
	 * @param superClass
	 *            super class that the generated class extends
	 * @param interfaces
	 *            list of interfaces that the generated class implements
	 * @param pkg
	 *            a Package that includes functions to be mapped to methods.
	 * @param context
	 *            a Context in which functions are extracted from pkg
	 * @param mode
	 *            THIS if each function has 'this' parameter, otherwise 0.
	 * @return the generated class
	 */
	public static Class generateSubclass(Class superClass,
					     Class[] interfaces,
					     Package pkg,
					     Context context,
					     int mode)
	{
		String superClassName = superClass.getName();
		String className = superClassName.replace('.', '_') + "__adapter";
		return generateSubclass(className, superClass, interfaces, pkg,
				context, mode);
	}

	/**
	 * Generates a class that extends the superClass and implements the
	 * interfaces. Functions in the pkg are mapped to non-final/static
	 * public/protected methods with the same name and the same number of
	 * parameters. Note that this method may throw ClassFormatError.
	 * 
	 * @param className
	 *            the name of the class to be generated
	 * @param superClass
	 *            super class that the generated class extends
	 * @param interfaces
	 *            list of interfaces that the generated class implements
	 * @param pkg
	 *            a Package that includes functions to be mapped to methods.
	 * @param context
	 *            a Context in which functions are extracted from pkg
	 * @param mode
	 *            THIS if each function has 'this' parameter, otherwise 0.
	 * @return the generated class
	 */
	public static Class generateSubclass(String className, Class superClass,
			Class[] interfaces, Package pkg, Context context, int mode) 
	{
		return generateSubclass(className, superClass, interfaces, null, pkg, context, mode);
	}

	/**
	 * Generates a class that extends the superClass and implements the
	 * interfaces. Functions in the pkg are mapped to non-final/static
	 * public/protected methods with the same name and the same number of
	 * parameters. Note that this method may throw ClassFormatError.
	 * 
	 * @param className
	 *            the name of the class to be generated
	 * @param superClass
	 *            super class that the generated class extends
	 * @param interfaces
	 *            list of interfaces that the generated class implements
	 * @param sigs
	 *            an array of optional explicit method signature.
	 *            If it is null, method signatures are determined by
	 *            supertypes' method signature and number of function parameters.
	 *            If it isn't null, the array of method signature
	 *            is used to determin method signatures.
	 * @param pkg
	 *            a Package that includes functions to be mapped to methods.
	 * @param context
	 *            a Context in which functions are extracted from pkg
	 * @param mode
	 *            THIS if each function has 'this' parameter, otherwise 0.
	 * @return the generated class
	 */
	public static Class generateSubclass(String className, Class superClass,
					     Class[] interfaces, Signature[] sigs,
					     Package pkg, Context context, int mode)
	{
	    return generateSubclass(className, superClass, interfaces, sigs, pkg, null, context, mode);
	}

	public static Class generateSubclass(String className, Class superClass,
					     Class[] interfaces, Signature[] sigs,
					     Package pkg, Map typeMap, Context context,
					     int mode)
	{
		ClassLoader classLoader = context.getClassLoader();
		if (classLoader == null) {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		ClassFileLoader handler = new ClassFileLoader(classLoader);
		handler.setup(pkg, context);
		ClassFile cf = getClassFileForSubclass(className, superClass,
				interfaces, pkg, typeMap, context, mode, sigs);

		if (DEBUG){
			try {
				FileOutputStream fout = new FileOutputStream("c:/tmp/" + className + ".class");
				DataOutputStream dout = new DataOutputStream(fout);
				cf.write(dout);
				fout.close();
				System.out.println(className);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		return (Class) handler.handle(cf);
	}

	/**
	 * Generates a subtype of the specified class and instantiates with the
	 * given arguments.
	 * 
	 * @param context
	 *            the context
	 * @param superClass
	 *            the super class to extend
	 * @param interfaces
	 *            the interfaces to implement
	 * @param pkg
	 *            a Package that includes functions to be mapped to methods.
	 * @param args
	 *            the arguments to the constructor
	 */
	public static Object instantiateSubtype(Context context, Class superClass,
			Class[] interfaces, Package pkg, Object[] args) {
		try {
			if (superClass == null) {
				superClass = Object.class;
			}
			Class cls = generateSubclass(superClass, interfaces, pkg, context,
					0);
			return Runtime.callConstructor(context, cls, args, null);
		} catch (Throwable t) {
			throw new PnutsException(t, context);
		}
	}

	/**
	 * Creates a class file image of a subtype of superClass (or some
	 * interfaces) and writes to the output stream.
	 * 
	 * @param superClass
	 *            super class that the generated class extends
	 * @param interfaces
	 *            list of interfaces that the generated class implements
	 * @param pkg
	 *            a Package that includes functions to be mapped to methods.
	 * @param context
	 *            a Context in which functions are extracted from pkg
	 * @param mode
	 *            THIS if each function has 'this' parameter, otherwise 0.
	 */
	public static ClassFile getClassFileForSubclass(String className,
			Class superClass, Class[] interfaces, Package pkg, Context context,
			int mode) {
	    return getClassFileForSubclass(className, superClass, interfaces, pkg, null,
					   context,  mode, null);
	}

	/**
	 * Creates a class file image of a subtype of superClass (or some
	 * interfaces) and writes to the output stream.
	 * 
	 * @param superClass
	 *            super class that the generated class extends
	 * @param interfaces
	 *            list of interfaces that the generated class implements
	 * @param pkg
	 *            a Package that includes functions to be mapped to methods.
	 * @param context
	 *            a Context in which functions are extracted from pkg
	 * @param mode
	 *            THIS if each function has 'this' parameter, otherwise 0.
	 * @param signatures
	 *            signature information
	 */
	public static ClassFile getClassFileForSubclass(String className,
							Class superClass,
							Class[] interfaces,
							Package pkg,
							Map typeMap, 
							Context context,
							int mode,
							Signature[] signatures)
	{
		ClassFile cf = ClassGenerator.createClassFile(className, superClass, interfaces, mode);
		constructor(cf, superClass);

		//
		// public static void attach(Context, Package);
		//
		cf.openMethod("attach",
			      "(Lpnuts/lang/Context;Lpnuts/lang/Package;)V",
			      (short) (Constants.ACC_PUBLIC | Constants.ACC_STATIC));

		cf.add(Opcode.ALOAD_0);
		cf.add(Opcode.PUTSTATIC, className, "_context",
		       "Lpnuts/lang/Context;");
		cf.add(Opcode.ALOAD_1);
		cf.add(Opcode.PUTSTATIC, className, "_package",
		       "Lpnuts/lang/Package;");
		cf.add(Opcode.RETURN);
		cf.closeMethod();

		if (typeMap != null){
		    for (Iterator it = typeMap.entrySet().iterator(); it.hasNext();){
			Map.Entry entry = (Map.Entry)it.next();
			String name = (String)entry.getKey();
			Class type = (Class)entry.getValue();
			cf.addField(name, Signature.makeSignature(type), Constants.ACC_PRIVATE);
		    }
		}

		if (signatures != null && typeMap != null){
		    defineMethods(cf, pkg, superClass, interfaces, context, mode, signatures);
		    Set sigset = new HashSet();
		    for (int i = 0; i < signatures.length; i++){
			sigset.add(signatures[i]);
		    }
		    for (Iterator it = typeMap.entrySet().iterator(); it.hasNext();){
			Map.Entry entry = (Map.Entry)it.next();
			String name = (String)entry.getKey();
			Class type = (Class)entry.getValue();
			Signature s = Compiler.getterSignature(type, name);
			if (!sigset.contains(s)){
			    Compiler.generateGetter(cf, name, type, s.getMethodName());
			}
			s = Compiler.setterSignature(type, name);
			if (!sigset.contains(s)){
			    Compiler.generateSetter(cf, name, type, s.getMethodName());
			}
		    }
		    
		} else {
		    // just for subclass() function
		    defineMethods(cf, pkg, superClass, interfaces, context,  mode);
		}

		return cf;
	}

	static void defineMethods(ClassFile cf,
				  Package pkg,
				  Class superClass,
				  Class[] interfaces,
				  Context context, 
				  int mode,
				  Signature[] signatures)
	{
		for (int i = 0; i < signatures.length; i++){
			Signature s = signatures[i];
			Object value = pkg.get(s.toString(), context);
			if (value instanceof PnutsFunction){
				PnutsFunction func = (PnutsFunction)value;
				List methods = new ArrayList();
				if (s.resolve(superClass, interfaces, methods)){
					String sig = s.toString();
					Class returnType = s.returnType;
					if (returnType == null){
					    returnType = Object.class;
					}
					defineMethod(cf, s.parameterTypes, returnType,
					       s.exceptionTypes, s.modifiers, s.methodName,
					       sig, mode);
				} else {
					for (Enumeration e = getFunctions(func); e.hasMoreElements();){
						Signature s2 = (Signature)s.clone();
						Function f = (Function)e.nextElement();
						int n = f.getNumberOfParameter();
						if ((mode & THIS) == THIS){
							n--;
						}
						if ((mode & SUPER) == SUPER){
							n--;
						}
						if (s.parameterTypes == null){
							Class[] p = new Class[n];
							for (int j = 0; j < n; j++){
								p[j] = Object.class;
							}
							s2.parameterTypes = p;
						} else {
							for (int j = 0; j < n; j++){
								if (s2.parameterTypes[j] == null){
									s2.parameterTypes[j] = Object.class;
								}
							}
						}
						if (s2.returnType == null){
							s2.returnType = Object.class;
						}
						String sig = s.methodName + Signature.makeSignature(s2.parameterTypes, s2.returnType);
						sig = sig.intern();
						pkg.set(sig, func, context);
						defineMethod(cf, s2.parameterTypes, s2.returnType,
						      s.exceptionTypes, Modifier.PUBLIC, s2.methodName,
						      sig, mode);							
					}
				}
			}
		}
	}

	static void defineMethods(ClassFile cf,
				  Package pkg,
				  Class superClass,
				  Class[] interfaces,
				  Context context, 
				  int mode)
	{
		for (Enumeration e = pkg.keys(); e.hasMoreElements();) {
			String name = (String) e.nextElement();
			Object value = pkg.get(name, context);
			if (!(value instanceof PnutsFunction)) {
				continue;
			}
			PnutsFunction func = (PnutsFunction) value;
			String sig = name;
			String methodName = name;
			
			int idx = methodName.indexOf('(');
			if (idx >= 0) {
				methodName = methodName.substring(0, idx);
			}
			defineMethods(cf, methodName, sig, func, superClass, interfaces,
				      context, mode);
		}
	}

	static int parseTypes(String sig, Context context, List types) {
		int idx = sig.indexOf('(');
		if (idx >= 0) {
			try {
				return parseParameterSignature(sig.substring(idx + 1), types,
						context)
						+ idx + 1;
			} catch (ClassNotFoundException e) {
			}
		}
		return -1;
	}

	static List returnTypeAndExceptions(String sig, Context context) {
		List types = new ArrayList();
		try {
			parseParameterSignature(sig, types, context);
		} catch (ClassNotFoundException e) {
			return null;
		}
		return types;
	}

	public static void constructor(ClassFile cf, Class superClass) {
	    constructor(cf, superClass, null, null, null);
	}

	public static void constructor(ClassFile cf,
				       Class superClass,
				       Compiler compiler,
				       Context cc,
				       List assignments) 
    {
		String className = cf.getClassName();

		cf.addField("_superCallProxy", "Lpnuts/lang/AbstractData;",
				Constants.ACC_PRIVATE);
		if (superClass == null){
		    superClass = Object.class;
		}
		Constructor constructors[] = superClass.getDeclaredConstructors();
		int count = 0;
		for (int i = 0; i < constructors.length; i++) {
			Constructor cons = constructors[i];
			Class parameterTypes[] = cons.getParameterTypes();
			String sig = ClassFile.signature(parameterTypes);

			int mod = cons.getModifiers();
			short acc;
			if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
				continue;
			}
			count++;
			cf.openMethod("<init>", sig + "V", Constants.ACC_PUBLIC);
			cf.add(Opcode.ALOAD_0);
			for (int j = 0, k = 0; j < parameterTypes.length; j++) {
				Class type = parameterTypes[j];
				if (type == int.class || type == byte.class
						|| type == short.class || type == char.class
						|| type == boolean.class) {
					cf.iloadLocal(1 + j + k);
				} else if (type == long.class) {
					cf.lloadLocal(1 + j + k);
					k++;
				} else if (type == float.class) {
					cf.floadLocal(1 + j + k);
				} else if (type == double.class) {
					cf.dloadLocal(1 + j + k);
					k++;
				} else {
					cf.loadLocal(1 + j + k);
				}
			}
			cf.add(Opcode.INVOKESPECIAL, superClass.getName(), "<init>", sig,
					"V");

			assignSuperCallProxy(cf, className);

			if (compiler != null){
			    declareFields(cf, cc, compiler, assignments);
			}

			cf.add(Opcode.RETURN);
			cf.closeMethod();
		}
		if (count == 0) {
			cf.openMethod("<init>", "()V", Constants.ACC_PUBLIC);
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.INVOKESPECIAL, superClass.getName(), "<init>", "()",
					"V");

			assignSuperCallProxy(cf, className);

			if (compiler != null){
			    declareFields(cf, cc, compiler, assignments);
			}

			cf.add(Opcode.RETURN);
			cf.closeMethod();
		}
	}

    private static void declareFields(ClassFile cf,
				      Context cc, 
				      Compiler compiler,
				      List assignments)
    {
	String className = cf.getClassName();
	for (int i = 0, n = assignments.size(); i < n ; i++){
	    SimpleNode assign = (SimpleNode)assignments.get(i);
	    SimpleNode lhs = assign.jjtGetChild(0);
	    SimpleNode rhs = assign.jjtGetChild(1);
	    cf.add(Opcode.GETSTATIC, className, "_context", "Lpnuts/lang/Context;");
	    cf.add(Opcode.ALOAD_0);  // this
	    cf.add(Opcode.LDC, cf.addConstant(lhs.str));
	    rhs.accept(compiler, cc);
	    cf.add(Opcode.INVOKESTATIC,
		   "pnuts.lang.Runtime",
		   "putField",
		   "(Lpnuts/lang/Context;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)",
		   "V");
	}
    }

	/*
	 * this._superCallProxy = new SuperCallProxy(this);
	 */
	private static void assignSuperCallProxy(ClassFile cf, String className) {
		cf.add(Opcode.ALOAD_0);
		cf.add(Opcode.NEW, SUPER_PROXY_NAME);
		cf.add(Opcode.DUP);
		cf.add(Opcode.ALOAD_0);
		cf.add(Opcode.INVOKESPECIAL, SUPER_PROXY_NAME, "<init>",
				"(Ljava/lang/Object;)", "V");
		cf.add(Opcode.PUTFIELD, className, "_superCallProxy",
				"Lpnuts/lang/AbstractData;");
	}

	private static void defineMethods(ClassFile cf, String name, String sig,
			PnutsFunction func, Class superClass, Class[] interfaces,
			Context context, int mode)
	{
		Hashtable methods = new Hashtable();
		int count = 0;
		if (interfaces != null) {
			loop1: for (int i = 0; i < interfaces.length; i++) {
				Class _interface = interfaces[i];
				Method[] _methods = _interface.getMethods();
				for (int j = 0; j < _methods.length; j++) {
					Method m = _methods[j];
					int modifiers = m.getModifiers();
					if (!Modifier.isPublic(modifiers)
							&& !Modifier.isProtected(modifiers)
							|| Modifier.isStatic(modifiers)
							|| Modifier.isFinal(modifiers)) {
						continue;
					}
					Class[] parameterTypes = m.getParameterTypes();
					Class[] exceptionTypes = m.getExceptionTypes();
					String signature = name
							+ ClassFile.signature(parameterTypes);
					int expected_args = parameterTypes.length;

					if ((mode & THIS) == THIS) {
						expected_args++;
					}
					if ((mode & SUPER) == SUPER) {
						expected_args++;
					}
					if (m.getName().equals(name) && func.defined(expected_args)) {
						if (sig == name) { // no type info
							if (methods.get(signature) == null) {
								defineMethod(cf, parameterTypes, m.getReturnType(),
										exceptionTypes, Modifier.PUBLIC, name,
										sig, mode);
								methods.put(signature, signature);
								count++;
							}
						} else {
							if (!sig.equals(signature)) {
								continue;
							}
							if (methods.get(signature) == null) {
								defineMethod(cf, parameterTypes, m.getReturnType(),
										exceptionTypes, Modifier.PUBLIC, name,
										sig, mode);
								methods.put(signature, signature);
								count++;
							}
						}
					}
				}
			}
		}
		if (superClass == null) {
			superClass = Object.class;
		}
		while (superClass != null) {
			Method[] _methods = getInheritableMethods(superClass);

			loop2: for (int j = 0; j < _methods.length; j++) {
				Method m = _methods[j];
				int modifiers = m.getModifiers();
				if (!Modifier.isPublic(modifiers)
						&& !Modifier.isProtected(modifiers)
						|| Modifier.isStatic(modifiers)
						|| Modifier.isFinal(modifiers)) {
					continue;
				}
				Class[] parameterTypes = m.getParameterTypes();
				Class[] exceptionTypes = m.getExceptionTypes();
				String signature = name + ClassFile.signature(parameterTypes);
				int expected_args = parameterTypes.length;
				if ((mode & THIS) == THIS) {
					expected_args++;
				}
				if ((mode & SUPER) == SUPER) {
					expected_args++;
				}
				if (m.getName().equals(name) && func.defined(expected_args)) {
					if (sig == name) { // no type info
						if (methods.get(signature) == null) {
							defineMethod(cf, parameterTypes, m.getReturnType(),
									exceptionTypes, Modifier.PUBLIC, name,
									sig, mode);
							methods.put(signature, signature);
							count++;
						}
					} else {
						if (!sig.equals(signature)) {
							continue;
						}
						if (methods.get(signature) == null) {
							defineMethod(cf, parameterTypes, m.getReturnType(),
									exceptionTypes, Modifier.PUBLIC, name,
									sig, mode);
							methods.put(signature, signature);
							count++;
						}
					}
				}
			}
			superClass = superClass.getSuperclass();
		}

		if (count == 0) { // when not override
			Enumeration ee = getFunctions(func);
			if (ee != null) {
				Function f = (Function) ee.nextElement();
				int narg = f.getNumberOfParameter();
				if ((mode & THIS) == THIS && narg > 0) {
					narg--;
				}
				if ((mode & SUPER) == SUPER && narg > 0) {
					narg--;
				}
				Class[] parameterTypes = new Class[narg];
				Class[] exceptionTypes = null;
				Class returnType;
				List types = null;
				int pos = -1;
				if (sig != name) {
					types = new ArrayList();
					pos = parseTypes(sig, context, types);
				}
				if (pos == -1) { // no type info
					for (int i = 0; i < narg; i++) {
						parameterTypes[i] = Object.class;
					}

					returnType = Object.class;
				} else {
					for (int i = 0; i < narg; i++) {
						parameterTypes[i] = (Class) types.get(i);
					}
					types = returnTypeAndExceptions(sig.substring(pos), context);

					returnType = (Class) types.get(0);
					if (types.size() > 1) {
						exceptionTypes = new Class[types.size() - 1];
						for (int i = 0; i < exceptionTypes.length; i++) {
							exceptionTypes[i] = (Class) types.get(1 + i);
						}
					}
				}
				defineMethod(cf, parameterTypes, returnType, exceptionTypes,
						Modifier.PUBLIC, name, sig, mode);
			}
		}
	}

	static Method[] getInheritableMethods(Class cls){
		Set s = new HashSet(); // signatures
		Set m = new HashSet(); // methods
		Class c = cls;
		while (c != null){
			getInheritableMethods(c, s, m);
			c = c.getSuperclass();
		}
		Method[] results = new Method[m.size()];
		return (Method[])m.toArray(results);
	}

	static void getInheritableMethods(Class cls, Set signatures, Set methods){
		Method _methods[] = cls.getDeclaredMethods();
		for (int j = 0; j < _methods.length; j++) {
			Method m = _methods[j];
			int modifiers = m.getModifiers();
			if (!Modifier.isPublic(modifiers)
						&& !Modifier.isProtected(modifiers)
						|| Modifier.isStatic(modifiers)
						|| Modifier.isFinal(modifiers))
			{
				continue;
			}
			String sig = m.getName() + ClassFile.signature(m.getParameterTypes());
			if (signatures.add(sig)){
				methods.add(m);
			}
		}
		Class[] interfaces = cls.getInterfaces();
		for (int j = 0; j < interfaces.length; j++){
		    getInheritableMethods(interfaces[j], signatures, methods);
		}
	}

	public static void defineMethod(ClassFile cf, Class[] parameterTypes,
			Class returnType, Class[] exceptionTypes, int modifiers,
			String methodName, String sig, int mode)
	{
	    ClassGenerator.defineMethod(cf, parameterTypes, returnType, exceptionTypes, modifiers,
					methodName, sig, mode);
	}

	private static int parseParameterSignature(String signature, List types,
			Context context) throws ClassNotFoundException {
		char[] c = signature.toCharArray();
		int index = 0;
		int dim = 0;
		//	boolean returnPart = false;
		loop: while (index < c.length) {
			switch (c[index]) {
			case 'V':
				types.add(void.class);
				dim = 0;
				index++;
				break;
			case 'J':
				types.add(Runtime.arrayType(Long.TYPE, dim));
				dim = 0;
				index++;
				break;
			case 'D':
				types.add(Runtime.arrayType(Double.TYPE, dim));
				dim = 0;
				index++;
				break;
			case 'B':
				types.add(Runtime.arrayType(Byte.TYPE, dim));
				dim = 0;
				index++;
				break;
			case 'S':
				types.add(Runtime.arrayType(Short.TYPE, dim));
				dim = 0;
				index++;
				break;
			case 'C':
				types.add(Runtime.arrayType(Character.TYPE, dim));
				dim = 0;
				index++;
				break;
			case 'I':
				types.add(Runtime.arrayType(Integer.TYPE, dim));
				dim = 0;
				index++;
				break;
			case 'Z':
				types.add(Runtime.arrayType(Boolean.TYPE, dim));
				dim = 0;
				index++;
				break;
			case 'F':
				types.add(Runtime.arrayType(Float.TYPE, dim));
				dim = 0;
				index++;
				break;
			case '[':
				while (c[index] == '[') {
					dim++;
					index++;
				}
				break;
			case 'L':
				int start = index + 1;
				while (c[index++] != ';') {
				}
				String cn = new String(c, start, index - start - 1);
				types.add(Runtime.arrayType(Pnuts.loadClass(cn.replace(
						'/', '.'), context), dim));
				dim = 0;
				break;
			case ')':
				//		returnPart = true;
				index++;
				//		continue loop;
				break loop;
			default:
				throw new PnutsException("illegal method signature", context);
			}
			//	    if (returnPart){
			//		break loop;
			//	    }
		}
		return index;
	}


	/**
	 * Generates an interface
	 * 
	 * @param name
	 *            the name of the interface
	 * @param superInterfaces
	 *            an array of super interface
	 * @param signatures
	 *            an array of method signatures
	 * @param context
	 *            the context in which the classes are loaded
	 * @return the generated interface
	 */
	public static Class generateInterface(String name, Class[] superInterfaces,
			String[] signatures, Context context) {
		return generateInterface(name, superInterfaces, signatures, context,
				(short) (Constants.ACC_PUBLIC | Constants.ACC_INTERFACE));
	}

	/**
	 * Generates an interface
	 * 
	 * @param name
	 *            the name of the interface
	 * @param superInterfaces
	 *            an array of super interface
	 * @param signatures
	 *            an array of method signatures
	 * @param context
	 *            the context in which the classes are loaded
	 * @param modifiers
	 *            the modifiers of the interface
	 * @return the generated interface
	 */
	public static Class generateInterface(String name, Class[] superInterfaces,
			String[] signatures, Context context, short modifiers) {
		ClassLoader classLoader = context.getClassLoader();
		if (classLoader == null) {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		ClassFileLoader handler = new ClassFileLoader(classLoader);
		ClassFile cf = getClassFileForInterface(name, superInterfaces,
				signatures, context, modifiers);
		return (Class) handler.handle(cf);
	}

	public static ClassFile getClassFileForInterface(String name,
			Class[] superInterfaces, String[] signatures, Context context,
			short modifiers) {
		ClassFile cf = new ClassFile(name, "java.lang.Object", null, modifiers);
		if (superInterfaces != null) {
			for (int i = 0; i < superInterfaces.length; i++) {
				cf.addInterface(superInterfaces[i].getName());
			}
		}
		if (signatures != null) {
			for (int i = 0; i < signatures.length; i++) {
				String sig = signatures[i];
				List types = new ArrayList();
				int idx0 = sig.indexOf('(');
				if (idx0 < 0) {
					continue;
				}
				String methodName = sig.substring(0, idx0);
				int idx = parseTypes(sig, context, types);
				Class[] parameterTypes = new Class[types.size()];
				types.toArray(parameterTypes);
				if (idx > 0) {
					List types2 = returnTypeAndExceptions(sig.substring(idx),
							context);
					Class returnType = (Class) types2.get(0);
					String[] exceptionTypeInfo = null;
					if (types2.size() > 1) {
						exceptionTypeInfo = new String[types2.size() - 1];
						for (int j = 0; j < exceptionTypeInfo.length; j++) {
							exceptionTypeInfo[j] = ((Class) types2
									.get(j + 1)).getName();
						}
					}
					cf.openMethod(
						      methodName,
						      Signature.makeSignature(parameterTypes, returnType),
						      (short) (Constants.ACC_PUBLIC | Constants.ACC_ABSTRACT),
						      exceptionTypeInfo);
					cf.closeMethod();
				}
			}
		}
		return cf;
	}

	public static ClassLoader mergeClassLoader(Class[] types, ClassLoader loader) {
		ArrayList classLoaders = new ArrayList();
		for (int i = 0; i < types.length; i++) {
			Class type = types[i];
			try {
				if (loader.loadClass(type.getName()) == type) {
					continue;
				}
			} catch (Exception e) {
			}
			classLoaders.add(type.getClassLoader());
		}
		int size = classLoaders.size();
		if (size > 0) {
			ClassLoader[] cl = new ClassLoader[size];
			classLoaders.toArray(cl);
			return new MultiClassLoader(loader, cl);
		} else {
			return loader;
		}
	}
}
