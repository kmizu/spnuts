/*
 * ReflectionUtil.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import pnuts.compiler.ClassFile;

public class ReflectionUtil {
        
	public static Method[] getInheritableMethods(Class cls){
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
}
