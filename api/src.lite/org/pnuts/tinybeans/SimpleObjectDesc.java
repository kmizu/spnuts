/*
 * SimpleObjectDesc.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.tinybeans;

import java.util.*;
import java.lang.reflect.*;
import org.pnuts.lang.ObjectDesc;
import org.pnuts.lang.PropertyHandler;

public class SimpleObjectDesc implements ObjectDesc {
    private Class cls;
    private Map properties;

    public SimpleObjectDesc(Class cls){
	this.cls = cls;
	Map m = new HashMap();
	analyze(cls, m);
	this.properties = m;
    }

    SimpleObjectDesc(Class cls, Map properties){
	this.cls = cls;
	this.properties = properties;
    }

    public Method[] getMethods(){
	return cls.getMethods();
    }

    public void handleProperties(PropertyHandler handler){
	for (Iterator it = properties.values().iterator(); it.hasNext();){
	    ObjectProperty p = (ObjectProperty)it.next();
	    handler.handle(p.getName(), p.getType(), p.getReadMethod(), p.getWriteMethod());
	}
    }

    public static void analyze(Class cls, Map/*<String,ObjectProperty>*/ properties){
	Method[] methods = cls.getMethods();
	for (int i = 0; i < methods.length; i++){
	    Method method = methods[i];
	    int mods = method.getModifiers();
	    if (Modifier.isStatic(mods)) {
		continue;
	    }
	    analyze(method, properties);
	}
    }

    public static void analyze(Method method, Map/*<String,ObjectProperty>*/ properties){
	    String name = method.getName();
	    Class argTypes[] = method.getParameterTypes();
	    Class resultType = method.getReturnType();
	    int argCount = argTypes.length;
	    ObjectProperty pd = null;
	    String propertyName = null;
	    String substr = null;
	    if (argCount == 0) {
		if (name.startsWith("get")) {
		    substr = name.substring(3);
		    propertyName = decapitalize(substr);
		    pd = new ObjectProperty(propertyName, resultType, method, null, isCanonicalName(substr));
		} else if (resultType == boolean.class && name.startsWith("is")) {
		    substr = name.substring(2);
		    propertyName = decapitalize(substr);
		    pd = new ObjectProperty(propertyName, resultType, method, null, isCanonicalName(substr));
		}
	    } else if (argCount == 1) {
		if (resultType == void.class && name.startsWith("set")) {
		    substr = name.substring(3);
		    propertyName = decapitalize(substr);
		    pd = new ObjectProperty(propertyName, argTypes[0], null, method, isCanonicalName(substr));
		}
	    }
	    if (propertyName != null){
		ObjectProperty p = (ObjectProperty)properties.get(propertyName);
		if (p == null || !p.hasCanonicalName() && isCanonicalName(substr)){
		    properties.put(propertyName, pd);
		} else if (p.type.equals(pd.type)){
		    if (p.r == null && pd.r != null){
			p.r = pd.r;
		    } else if (p.w == null && pd.w != null){
			p.w = pd.w;
		    }
		}
	    }
    }

    static boolean isCanonicalName(String name){
	int len = name.length();
	if (len == 1){
	    return Character.isUpperCase(name.charAt(0));
	} else if (len > 1){
	    if (!Character.isUpperCase(name.charAt(0))){
		return false;
	    }
	    if (Character.isUpperCase(name.charAt(1))){
		return true;
	    }
	    for (int i = 2; i < len; i++){
		if (Character.isUpperCase(name.charAt(i))){
		    return false;
		}
	    }
	    return true;
	}
	return false;
    }

    public static String decapitalize(String name) {
	if (name == null || name.length() == 0) {
	    return name;
	}
	if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
			Character.isUpperCase(name.charAt(0))){
	    return name;
	}
	char chars[] = name.toCharArray();
	chars[0] = Character.toLowerCase(chars[0]);
	return new String(chars);
    }

}
