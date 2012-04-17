/*
 * @(#)BeanClassGenerator.java 1.2 04/12/06
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.compiler.*;
import org.pnuts.lang.ClassFileLoader;
import java.io.*;
import java.util.*;

public class BeanClassGenerator {

	public static ClassFile generateClassFile(Map typeMap,
											  String className,
											  String superClassName,
											  String[] interfaces,
											  String[] constructorParams)
		{
			if (superClassName == null){
				superClassName = "java.lang.Object";
			}
			ClassFile cf = new ClassFile(className,  superClassName,  null, Constants.ACC_PUBLIC);
			if (interfaces != null){
				for (int i = 0; i < interfaces.length; i++){
					cf.addInterface(interfaces[i]);
				}
			}
			cf.openMethod("<init>", "()V", Constants.ACC_PUBLIC);
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.INVOKESPECIAL, superClassName, "<init>", "()", "V");
			cf.add(Opcode.RETURN);
			cf.closeMethod();


			if (constructorParams != null){
				boolean constructorParamsAreValid = true;
				StringBuffer sbuf = new StringBuffer("(");
				for (int i = 0; i < constructorParams.length; i++){
					String param = constructorParams[i];
					Class type = (Class)typeMap.get(param);
					if (type == null){
						constructorParamsAreValid = false;
						break;
					}
					sbuf.append(ClassFile.signature(type));
				}
				if (constructorParamsAreValid){
					sbuf.append(")V");
					cf.openMethod("<init>", sbuf.toString(), Constants.ACC_PUBLIC);

					cf.add(Opcode.ALOAD_0);
					cf.add(Opcode.INVOKESPECIAL, superClassName, "<init>", "()", "V");
					for (int i = 0; i < constructorParams.length; i++){
						cf.add(Opcode.ALOAD_0);
						String param = constructorParams[i];
						Class type = (Class)typeMap.get(param);
						if (type.isPrimitive()){
							loadPrimitive(cf, type, i + 1);
						} else {
							cf.loadLocal(i + 1);
						}
						cf.add(Opcode.PUTFIELD, cf.getClassName(), param, ClassFile.signature(type));
					}
					cf.add(Opcode.RETURN);
					cf.closeMethod();
				}
			}

			for (Iterator it = typeMap.entrySet().iterator();
				 it.hasNext(); )
			{
				Map.Entry entry = (Map.Entry)it.next();
				String key = (String)entry.getKey();
				Class type = (Class)entry.getValue();
				emit(cf, key, type);
			}
			return cf;
		}

	private static void loadPrimitive(ClassFile cf, Class primitive, int index){
		if (primitive == int.class){
			cf.iloadLocal(index);
		} else if (primitive == byte.class){
			cf.iloadLocal(index);
		} else if (primitive == short.class){
			cf.iloadLocal(index);
		} else if (primitive == char.class){
			cf.iloadLocal(index);
		} else if (primitive == long.class){
			cf.lloadLocal(index);
		} else if (primitive == float.class){
			cf.floadLocal(index);
		} else if (primitive == double.class){
			cf.dloadLocal(index);
		} else if (primitive == boolean.class){
			cf.iloadLocal(index);
		}
	}

	static void emit(ClassFile cf, String propertyName, Class type){
		cf.addField(propertyName, ClassFile.signature(type), Constants.ACC_PRIVATE);
		/*
		 * getter
		 */
		cf.openMethod(getterName(propertyName, type),
					  makeSignature(null, type),
					  (short)Constants.ACC_PUBLIC);
		cf.add(Opcode.ALOAD_0);
		cf.add(Opcode.GETFIELD, cf.getClassName(), propertyName, ClassFile.signature(type));
		if (!type.isPrimitive()){
			cf.add(Opcode.ARETURN);
		} else {
			if (type == long.class){
				cf.add(Opcode.LRETURN);
			} else if (type == float.class){
				cf.add(Opcode.FRETURN);
			} else if (type == double.class){
				cf.add(Opcode.DRETURN);
			} else {
				cf.add(Opcode.IRETURN);
			}
		}
		cf.closeMethod();

		/*
		 * setter
		 */
		cf.openMethod(setterName(propertyName),
					  makeSignature(type, void.class),
					  Constants.ACC_PUBLIC);
		cf.add(Opcode.ALOAD_0);
		if (!type.isPrimitive()){
			cf.add(Opcode.ALOAD_1);
		} else {
			if (type == long.class){
				cf.add(Opcode.LLOAD_1);
			} else if (type == float.class){
				cf.add(Opcode.FLOAD_1);
			} else if (type == double.class){
				cf.add(Opcode.DLOAD_1);
			} else {
				cf.add(Opcode.ILOAD_1);
			}
		}
		cf.add(Opcode.PUTFIELD, cf.getClassName(), propertyName, ClassFile.signature(type));
		cf.add(Opcode.RETURN);
		cf.closeMethod();
	}

	static String capitalize(String s) {
		char chars[] = s.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars);
	}

	static String getterName(String property, Class type){
		if (type == boolean.class){
			return "is" + capitalize(property);
		} else {
			return "get" + capitalize(property);
		}
	}

	static String setterName(String property){
		return "set" + capitalize(property);
	}

	static String makeSignature(Class type, Class returnType){
		StringBuffer sbuf = new StringBuffer();
		if (type == null){
			sbuf.append("()");
		} else {
			sbuf.append(ClassFile.signature(new Class[]{type}));
		}
		if (returnType == void.class){
			sbuf.append('V');
		} else {
			sbuf.append(ClassFile.signature(returnType));
		}
		return sbuf.toString();
	}

	/**
	 * Generates a JavaBeans class from type map
	 *
	 * @param typeName [propertyName, type] mapping
	 * @param className the class name
	 * @param superClassName the super class mame, null implies java.lang.Object
	 * @param interfaces the array of interface names, null if none.
	 * @return the generated class
	 */
	public static Class generate(Map typeMap,
								 String className,
								 String superClassName,
								 String[] interfaces)
		throws IOException
	{
		return generate(typeMap, className, superClassName, interfaces, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Generates a JavaBeans class from type map
	 *
	 * @param typeName [propertyName, type] mapping
	 * @param className the class name
	 * @param superClassName the super class mame, null implies java.lang.Object
	 * @param interfaces the array of interface names, null if none.
	 * @param loader the parent ClassLoader
	 * @return the generated class
	 */
	public static Class generate(Map typeMap,
								 String className,
								 String superClassName,
								 String[] interfaces,
								 ClassLoader loader)
		throws IOException
	{
		ClassFile cf = generateClassFile(typeMap, className, superClassName, interfaces, null);
		return (Class)new ClassFileLoader(loader).handle(cf);
	}

	/**
	 * Generates a JavaBeans class from type map,
	 * and write the byte code to the specified output stream.
	 *
	 * @param typeName [propertyName, type] mapping
	 * @param className the class name
	 * @param superClassName the super class mame, null implies java.lang.Object
	 * @param interfaces the array of interface names, null if none.
	 * @param out the output stream
	 */
	public static void generate(Map typeMap,
								String className,
								String superClassName,
								String[] interfaces,
								OutputStream out)
		throws IOException
	{
		ClassFile cf = generateClassFile(typeMap, className, superClassName, interfaces, null);
		cf.write(out);
	}
}
