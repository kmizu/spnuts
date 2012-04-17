/*
 * @(#)DynamicProxyFactory.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import pnuts.lang.Pnuts;

/**
 * This class is used to create a proxy class on-the-fly
 * to replace a reflective method call.  This mechanism
 * improves execution speed, especially with JIT compiler.
 */
public final class DynamicProxyFactory {
    private final static boolean DEBUG = false;

    static Map prim_wrapper_table = new HashMap(8);
    static {
		prim_wrapper_table.put(int.class, "java.lang.Integer");
		prim_wrapper_table.put(short.class, "java.lang.Short");
		prim_wrapper_table.put(long.class, "java.lang.Long");
		prim_wrapper_table.put(char.class, "java.lang.Character");
		prim_wrapper_table.put(byte.class, "java.lang.Byte");
		prim_wrapper_table.put(float.class, "java.lang.Float");
		prim_wrapper_table.put(double.class, "java.lang.Double");
		prim_wrapper_table.put(boolean.class, "java.lang.Boolean");
    }

    static String wrapperClass(Class returnType){
		return (String)prim_wrapper_table.get(returnType);
    }

    private static void loadParam(ClassFile cf, Class[] paramTypes, int index){
		int nargs = paramTypes.length;
		if (nargs == 0){
			return;
		}
		int n = nargs;
		if (nargs > 6){
			n = 6;
		}
		int i = 0;
		for (; i < n; i++){
			cf.add((byte)(Opcode.ALOAD_0 + index));
			cf.add((byte)(Opcode.ICONST_0 + i));
			cf.add(Opcode.AALOAD);
			if (paramTypes[i] != Object.class){
				castParam(cf, paramTypes[i]);
			}
		}
		for (; i < nargs; i++){
			cf.add((byte)(Opcode.ALOAD_0 + index));
			cf.add(Opcode.BIPUSH, i);
			cf.add(Opcode.AALOAD);
			if (paramTypes[i] != Object.class){
				castParam(cf, paramTypes[i]);
			}
		}
    }

    public static byte byte_cast(Object param){
		if (param instanceof Character){
			return (byte)((Character)param).charValue();
		} else if (param instanceof Number){
			return ((Number)param).byteValue();
		} else {
			throw new ClassCastException(Pnuts.format(param));
		}
    }

    public static int int_cast(Object param){
		if (param instanceof Character){
			return (int)((Character)param).charValue();
		} else if (param instanceof Number){
			return ((Number)param).intValue();
		} else {
			throw new ClassCastException(Pnuts.format(param));
		}
    }

    public static short short_cast(Object param){
		if (param instanceof Character){
			return (short)((Character)param).charValue();
		} else if (param instanceof Number){
			return ((Number)param).shortValue();
		} else {
			throw new ClassCastException(Pnuts.format(param));
		}
    }

    public static char char_cast(Object param){
		if (param instanceof Character){
			return ((Character)param).charValue();
		} else if (param instanceof Number){
			return (char)((Number)param).intValue();
		} else {
			throw new ClassCastException(Pnuts.format(param));
		}
    }

    public static long long_cast(Object param){
		if (param instanceof Character){
			return (long)((Character)param).charValue();
		} else if (param instanceof Number){
			return ((Number)param).longValue();
		} else {
			throw new ClassCastException(Pnuts.format(param));
		}
    }

    public static float float_cast(Object param){
		if (param instanceof Character){
			return (float)((Character)param).charValue();
		} else if (param instanceof Number){
			return ((Number)param).floatValue();
		} else {
			throw new ClassCastException(Pnuts.format(param));
		}
    }

    public static double double_cast(Object param){
		if (param instanceof Character){
			return (double)((Character)param).charValue();
		} else if (param instanceof Number){
			return ((Number)param).doubleValue();
		} else {
			throw new ClassCastException(Pnuts.format(param));
		}
    }

    static void castParam(ClassFile cf, Class paramType){
		if (paramType == int.class){
			cf.add(Opcode.INVOKESTATIC,
				   "pnuts.compiler.DynamicProxyFactory", "int_cast", "(Ljava/lang/Object;)", "I");
		} else if (paramType == byte.class){
			cf.add(Opcode.INVOKESTATIC,
				   "pnuts.compiler.DynamicProxyFactory", "byte_cast", "(Ljava/lang/Object;)", "B");
		} else if (paramType == short.class){
			cf.add(Opcode.INVOKESTATIC,
				   "pnuts.compiler.DynamicProxyFactory", "short_cast", "(Ljava/lang/Object;)", "S");
		} else if (paramType == char.class){
			cf.add(Opcode.INVOKESTATIC,
				   "pnuts.compiler.DynamicProxyFactory", "char_cast", "(Ljava/lang/Object;)", "C");
		} else if (paramType == long.class){
			cf.add(Opcode.INVOKESTATIC,
				   "pnuts.compiler.DynamicProxyFactory", "long_cast", "(Ljava/lang/Object;)", "J");
		} else if (paramType == float.class){
			cf.add(Opcode.INVOKESTATIC,
				   "pnuts.compiler.DynamicProxyFactory", "float_cast", "(Ljava/lang/Object;)", "F");
		} else if (paramType == double.class){
			cf.add(Opcode.INVOKESTATIC,
				   "pnuts.compiler.DynamicProxyFactory", "double_cast", "(Ljava/lang/Object;)", "D");
		} else if (paramType == boolean.class){
			String wrapper = "java.lang.Boolean";
			cf.add(Opcode.CHECKCAST, wrapper);
			cf.add(Opcode.INVOKEVIRTUAL, wrapper, "booleanValue", "()", "Z");
		} else {
			cf.add(Opcode.CHECKCAST, paramType.getName());
		}
    }

    static DynamicProxy makeProxy(Method method, CodeLoader loader) {
		return makeProxy(method.getName(),
						 method.getDeclaringClass(),
						 method.getReturnType(),
						 method.getParameterTypes(),
						 Modifier.isStatic(method.getModifiers()) ? 1 : 0,
						 loader);
    }

    static DynamicProxy makeProxy(Constructor cons, CodeLoader loader) {
		return makeProxy("<init>",
						 cons.getDeclaringClass(),
						 void.class,
						 cons.getParameterTypes(),
						 2,
						 loader);
    }

    static DynamicProxy makeProxy(String methodName,
								  Class declaringClass,
								  Class returnType,
								  Class[] paramTypes,
								  int type,  // 0: instance method, 1: static method, 2: constructor
								  CodeLoader loader)
		{
			boolean prim = returnType.isPrimitive() && returnType != void.class;

			String tempClassName = "_" + (loader.nextCount() & 0x7fffffff);
	
			ClassFile cf = new ClassFile(tempClassName,
										 "pnuts.compiler.DynamicProxy",
										 null,
										 (short)(Constants.ACC_PUBLIC | Constants.ACC_FINAL));
			cf.openMethod("<init>", "()V", Constants.ACC_PUBLIC);
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.INVOKESPECIAL, "pnuts.compiler.DynamicProxy", "<init>", "()", "V");
			cf.add(Opcode.RETURN);
			cf.closeMethod();

			int nargs = paramTypes.length;
			String sig = null;
			if (nargs == 0){
				sig = "(Ljava/lang/Object;";
				sig += ")Ljava/lang/Object;";
			} else {
				sig = "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
			}
			cf.openMethod("invoke", sig, Constants.ACC_PUBLIC);
			String className = declaringClass.getName();

			if (type == 2){
				cf.add(Opcode.NEW, className);
				cf.add(Opcode.DUP);
			} else if (prim){
				cf.add(Opcode.NEW, wrapperClass(returnType));
				cf.add(Opcode.DUP);
			}

			if (type == 0){
				cf.add(Opcode.ALOAD_1); // target
				if (declaringClass != Object.class){
					cf.add(Opcode.CHECKCAST, className);
				}
			}

			if (paramTypes.length > 0){
				loadParam(cf, paramTypes, 2);
			}
			if (type == 1){
				cf.add(Opcode.INVOKESTATIC, className, methodName, ClassFile.signature(paramTypes), ClassFile.signature(returnType));
			} else if (type == 0){
				if (declaringClass.isInterface()){
					cf.add(Opcode.INVOKEINTERFACE, className, methodName, ClassFile.signature(paramTypes), ClassFile.signature(returnType));
				} else {
					cf.add(Opcode.INVOKEVIRTUAL, className, methodName, ClassFile.signature(paramTypes), ClassFile.signature(returnType));
				}
			} else if (type == 2){
				cf.add(Opcode.INVOKESPECIAL, className, methodName, ClassFile.signature(paramTypes), ClassFile.signature(returnType));
			}
	
			if (prim){
				cf.add(Opcode.INVOKESPECIAL, wrapperClass(returnType), "<init>", "(" + ClassFile.signature(returnType) + ")", "V");
			}
			if (type != 2 && returnType == void.class){
				cf.add(Opcode.ACONST_NULL);
			}
			cf.add(Opcode.ARETURN);
			cf.closeMethod();

			try {
				if (DEBUG){
					FileOutputStream fout = new FileOutputStream("/tmp/" + tempClassName + ".class");
					DataOutputStream dout = new DataOutputStream(fout);
					cf.write(dout);
					fout.close();
					System.out.println(tempClassName);
				}

				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream dout = new DataOutputStream(bout);
				cf.write(dout);

				Class clazz = loader.define(tempClassName, bout.toByteArray(), 0, bout.size());
				loader.resolve(clazz);
				return (DynamicProxy)clazz.newInstance();
			} catch (ClassCastException cce){
			    throw cce;
			} catch (Exception e){
				throw new InternalError(e.toString());
			}
		}
}
