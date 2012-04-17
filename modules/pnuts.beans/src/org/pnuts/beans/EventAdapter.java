/*
 * @(#)EventAdapter.java 1.3 05/03/18
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import pnuts.compiler.*;
import java.io.*;
import java.security.*;
import java.lang.reflect.*;

/**
 * This class generates an event adapter for a particular listener type and callback method.
 *
 * <pre>In Pnuts,
 * import("java.awt.event.*")
 *
 * adapterClass = generateEventAdapter(ActionListener, "actionPerformed")
 * adapter = adapterClass(function (e) ... , getContext())
 * </pre>
 */
public class EventAdapter {

	static boolean hasJava2Security = false;
	static {
		try {
			Class.class.getMethod("getProtectionDomain", new Class[]{});
			hasJava2Security = true;
		} catch (Exception e){
		}
	}

	/**
	 * Generate an adapter class for bean events.
	 * The generated class implements the the specified <em>listenerType</em>.
	 * The constructor has two arguments (PnutsFunction, Context).  The method <em>methodName</em>
	 * calls a function which is specified as a parameter of the constructor.
	 *
	 * @param listenerType a Class object of a EventListener subclass
	 * @param methodName a method name of the listener type.
	 * @return a Class object of the generated class
	 */
	public static Class generateEventAdapter(Class listenerType, String methodName){
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ClassFile cf = getClassFileForEventAdapter("__event_adapter__", listenerType, methodName);
			cf.write(bout);
			return defineClass(cf.getClassName(), bout.toByteArray(), EventAdapter.class.getClassLoader());
		} catch (IOException io){
			return null;
		}
	}

	/**
	 * Creates a class file of a event adapter that implements the specified listenerType.
	 * The constructor of the created class takes PnutsFunction and Context as the parameters.
	 * When the corresponding event is observed, the PnutsFunction is called in the Context.
	 *
	 * @param className the class name of the event adapter
	 * @param listenerType a Class object of a EventListener subclass
	 * @param methodName a method name of the listener type.
	 */
	public static ClassFile getClassFileForEventAdapter(String className,
														Class listenerType,
														String methodName)
		{
			/*
			  if (!EventListener.class.isAssignableFrom(listenerType)){
			  throw new RuntimeException("EventListener type is required:" + listenerType);
			  }
			*/
			ClassFile cf = new ClassFile(className, "java.lang.Object", null, Constants.ACC_PUBLIC);
			cf.addInterface(listenerType.getName());
			cf.addField("context", "Lpnuts/lang/Context;", Constants.ACC_PRIVATE);
			cf.addField("function", "Lpnuts/lang/PnutsFunction;", Constants.ACC_PRIVATE);

			cf.openMethod("<init>", "(Lpnuts/lang/PnutsFunction;Lpnuts/lang/Context;)V", Constants.ACC_PUBLIC);
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.INVOKESPECIAL, "java.lang.Object", "<init>", "()", "V");
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.ALOAD_1);
			cf.add(Opcode.PUTFIELD, cf.getClassName(), "function", "Lpnuts/lang/PnutsFunction;");
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.ALOAD_2);
			cf.add(Opcode.PUTFIELD, cf.getClassName(), "context", "Lpnuts/lang/Context;");
			cf.add(Opcode.RETURN);
			cf.closeMethod();

			Method[] methods = listenerType.getMethods();
			for (int i = 0; i < methods.length; i++){
				Method m = methods[i];
				Class[] types = m.getParameterTypes();
				if (m.getName().equals(methodName)){
					cf.openMethod(methodName, makeSignature(types, void.class), Constants.ACC_PUBLIC);
					Label catchStart = cf.getLabel(true);

					cf.add(Opcode.ALOAD_0);
					cf.add(Opcode.GETFIELD, cf.getClassName(), "function", "Lpnuts/lang/PnutsFunction;");
					cf.add(Opcode.ICONST_1);
					cf.add(Opcode.ANEWARRAY, "java.lang.Object");
					cf.add(Opcode.DUP);
					cf.add(Opcode.ICONST_0);
					cf.add(Opcode.ALOAD_1);
					cf.add(Opcode.AASTORE);
					cf.add(Opcode.ALOAD_0);
					cf.add(Opcode.GETFIELD, cf.getClassName(), "context", "Lpnuts/lang/Context;");
					cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.PnutsFunction", "call", "([Ljava/lang/Object;Lpnuts/lang/Context;)", "Ljava/lang/Object;");
					cf.add(Opcode.POP);
					cf.add(Opcode.RETURN);
					Label catchEnd = cf.getLabel(true);
					Label catchTarget = cf.getLabel(true);
					cf.reserveStack(1);
					cf.add(Opcode.ALOAD_0);
					cf.add(Opcode.GETFIELD, cf.getClassName(), "context", "Lpnuts/lang/Context;");
					cf.add(Opcode.INVOKESTATIC,
					       "pnuts.lang.Runtime",
					       "printError",
					       "(Ljava/lang/Throwable;Lpnuts/lang/Context;)",
					       "V");
					cf.add(Opcode.RETURN);
					
					cf.addExceptionHandler(catchStart,
							       catchEnd,
							       catchTarget,
							       "java.lang.Throwable");
					cf.closeMethod();
				} else {
					cf.openMethod(m.getName(), makeSignature(types, void.class), Constants.ACC_PUBLIC);
					cf.add(Opcode.RETURN);
					cf.closeMethod();
				}
			}
			return cf;
		}

	private static String makeSignature(Class[] parameterTypes, Class returnType){
		StringBuffer sbuf = new StringBuffer();
		sbuf.append('(');
		for (int i = 0; i < parameterTypes.length; i++){
			sbuf.append(ClassFile.signature(parameterTypes[i]));
		}
		sbuf.append(')');
		sbuf.append(ClassFile.signature(returnType));
		return sbuf.toString();
	}

	private static Class defineClass(String name, byte[] array, final ClassLoader parent){
		ByteCodeLoader loader = null;
		if (hasJava2Security){
			loader = (ByteCodeLoader)AccessController.doPrivileged(new PrivilegedAction(){
					public Object run(){
						return new ByteCodeLoader(parent);
					}
				});
		} else {
			loader = new ByteCodeLoader(parent);
		}
		return loader.define(name, array, 0, array.length);
	}
}
