/*
 * Signature.java
 *
 * Copyright (c) 2005-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.util.*;
import java.lang.reflect.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;
import pnuts.lang.Package;
import pnuts.compiler.*;

public class Signature extends Runtime implements Cloneable {

	String methodName;
	Class returnType;  // UNKNOWN == null
	int modifiers;
	Class[] parameterTypes; // UNKNOWN == null
	Class[] exceptionTypes; // UNKNOWN == null
	public Object nodeInfo;

	public Signature(String methodName){
		this(methodName, null, null, null);
	}

	public Signature(String methodName,
			 Class returnType,
			 Class[] parameterTypes,
			 Class[] exceptionTypes)
	{
		this(methodName, returnType, parameterTypes, exceptionTypes, null);
	}

	public Signature(String methodName,
			 Class returnType,
			 Class[] parameterTypes,
			 Class[] exceptionTypes,
			 int modifiers)
	{
		this(methodName, returnType, parameterTypes, exceptionTypes, null);
		this.modifiers = modifiers;
	}

	public Signature(String methodName,
			 Class returnType,
			 Class[] parameterTypes,
			 Class[] exceptionTypes,
			 Object nodeInfo)
	{
		this.methodName = methodName;
		this.returnType = returnType;
		if (parameterTypes != null){
			this.parameterTypes = (Class[])parameterTypes.clone();
		}
		if (exceptionTypes != null){
			this.exceptionTypes = (Class[])exceptionTypes.clone();
		}
		this.nodeInfo = nodeInfo;
	}

    public String getMethodName(){
	return methodName;
    }

    public Class getReturnType(){
	return returnType;
    }

    public void setReturnType(Class t){
	this.returnType = t;
    }

    public Class[] getParameterTypes(){
	return parameterTypes;
    }

    public void setParameterTypes(Class[] types){
	parameterTypes = types;
    }

    public Class[] getExceptionTypes(){
	return exceptionTypes;
    }

    public void setExceptionTypes(Class[] types){
	exceptionTypes = types;
    }

    public int getModifiers(){
	return modifiers;
    }

    public void setModifiers(int m){
	modifiers = m;
    }
/*
	public void generateMethod(ClassFile cf, int mode){
	    String sig = toString();
		SubtypeGenerator.defineMethod(cf, parameterTypes, returnType, exceptionTypes, modifiers,
					      methodName, sig, mode);
	}
*/
	boolean match(String methodName,
			 Class returnType,
			 Class[] parameterTypes,
			 Class[] exceptionTypes)
	{
		if (this.methodName != null && !methodName.equals(this.methodName)){
			return false;
		}
		if (this.returnType != null && !returnType.equals(this.returnType)){
			return false;
		}
		if (this.parameterTypes != null && this.parameterTypes.length != parameterTypes.length){
			return false;
		}
		if (this.parameterTypes != null){
			for (int i = 0; i < parameterTypes.length; i++){
				Class t = this.parameterTypes[i];
				if (t != null && !parameterTypes[i].equals(t)){
					return false;
				}
			}
		}
		if (this.exceptionTypes != null){
			HashSet s1 = new HashSet();
			for (int i = 0; i < exceptionTypes.length; i++){
				s1.add(exceptionTypes[i]);
			}
			HashSet s2 = new HashSet();
			for (int i = 0; i < this.exceptionTypes.length; i++){
				s2.add(this.exceptionTypes[i]);
			}
			if (!s1.equals(s2)){
				return false;
			}
		}
		return true;
	}

	public boolean resolve(Class superClass, Class[] interfaces, Collection methods)
	{
		Set signatures = new HashSet();
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
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
					String signature = methodName
							+ ClassFile.signature(parameterTypes);
					if (match(m.getName(), m.getReturnType(), parameterTypes, exceptionTypes))
					{
					    if (signatures.add(ClassFile.signature(parameterTypes))){
						methods.add(m);
					    }
					}
				}
			}
		}
		if (superClass == null) {
			superClass = Object.class;
		}
		while (superClass != null) {
			Method[] _methods = ReflectionUtil.getInheritableMethods(superClass);
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
				String signature = methodName + ClassFile.signature(parameterTypes);
				if (match(m.getName(), m.getReturnType(), parameterTypes, exceptionTypes))
				{
				    if (signatures.add(ClassFile.signature(parameterTypes))){
					methods.add(m);
				    }
				}
			}
			superClass = superClass.getSuperclass();
		}

		return !methods.isEmpty();
	}


	static void getInheritableConstructors(Class cls, Set signatures, Set results){
		Constructor c[] = cls.getDeclaredConstructors();
		for (int j = 0; j < c.length; j++) {
			Constructor cons = c[j];
			int modifiers = cons.getModifiers();
			if (!Modifier.isPublic(modifiers)
						&& !Modifier.isProtected(modifiers)
						|| Modifier.isStatic(modifiers)
						|| Modifier.isFinal(modifiers))
			{
				continue;
			}
			String sig = ClassFile.signature(cons.getParameterTypes());
			if (signatures.add(sig)){
				results.add(cons);
			}
		}
	}
	
	static Constructor[] getInheritableConstructors(Class cls){
		Set s = new HashSet();
		Set sig = new HashSet();
		if (cls != null){
			getInheritableConstructors(cls, sig, s);
		}
		return (Constructor[])s.toArray(new Constructor[s.size()]);
	}

	public boolean resolveAsConstructor(Class cls, Collection/*<Constructor>*/ constructors){
	    Constructor c[] = getInheritableConstructors(cls);
	    for (int i = 0; i < c.length; i++){
		Constructor cons = c[i];
		Class[] parameterTypes = cons.getParameterTypes();
		Class[] exceptionTypes = cons.getExceptionTypes();
		if (match(null, null, parameterTypes, exceptionTypes)){
		    constructors.add(cons);
		}
	    }
	    return !constructors.isEmpty();
	}


	static String makeSignature(Class[] parameterTypes) {
		StringBuffer buf = new StringBuffer("(");
		for (int i = 0; i < parameterTypes.length; i++) {
			Class type = parameterTypes[i];
			if (type == null){
			    type = Object.class;
			}
			buf.append(makeSignature(type));
		}
		buf.append(")");
		return buf.toString();
	}

	static String makeSignature(Class[] parameterTypes, Class returnType) {
		StringBuffer buf = new StringBuffer("(");
		for (int i = 0; i < parameterTypes.length; i++) {
			Class type = parameterTypes[i];
			if (type == null){
			    type = Object.class;
			}
			buf.append(makeSignature(type));
		}
		buf.append(")");
		if (returnType == null){
		    returnType = Object.class;
		}
		buf.append(makeSignature(returnType));
		return buf.toString();
	}

	public static String makeSignature(Class type) {
		if (type == int.class) {
			return "I";
		} else if (type == byte.class) {
			return "B";
		} else if (type == long.class) {
			return "J";
		} else if (type == char.class) {
			return "C";
		} else if (type == short.class) {
			return "S";
		} else if (type == float.class) {
			return "F";
		} else if (type == double.class) {
			return "D";
		} else if (type == boolean.class) {
			return "Z";
		} else if (type == void.class) {
			return "V";
		} else {
			if (type.isArray()) {
				return "[" + makeSignature(type.getComponentType());
			} else {
				return "L" + type.getName().replace('.', '/') + ";";
			}
		}
	}

	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e){
			throw new InternalError();
		}
	}

	public int hashCode(){
	    if (methodName != null){
		return methodName.hashCode() * parameterTypes.length;
	    } else {
		return parameterTypes.length;
	    }
	}

	public boolean equals(Object obj){
	    if (obj instanceof Signature){
		Signature sig = (Signature)obj;
		return sig.toString().equals(toString());
	    }
	    return false;
	}

	public String toString(){
	    if (methodName != null){
		return (methodName + makeSignature(parameterTypes)).intern();
	    } else {
		return makeSignature(parameterTypes).intern();
	    }
	}

    public boolean isResolved(){
	return returnType != null && parameterTypes != null && exceptionTypes != null;
    }

    public String toJavaIdentifier(){
            return toJavaIdentifier(toString());
    }
    
    public static String toJavaIdentifier(String signature){
        StringBuffer sbuf = new StringBuffer();
        for (int i = 0, len = signature.length(); i < len; i++){
            char ch = signature.charAt(i);
            switch (ch){
                case '(':
                    sbuf.append("$0");
                    break;
                case ')':
                    sbuf.append("$1");
                    break;
                case ';':
                    sbuf.append("$2");
                    break;
                case '/':
                    sbuf.append("$3");
                    break;
                case '$':
                    sbuf.append("$$");
                    break;
                default:
                    sbuf.append(ch);
            }
        }
        return sbuf.toString();
    }

}
