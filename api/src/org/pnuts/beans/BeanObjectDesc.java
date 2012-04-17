/*
 * BeanObjectDesc.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import org.pnuts.lang.*;
import java.beans.*;
import java.util.*;
import java.lang.reflect.*;

public class BeanObjectDesc implements ObjectDesc {
    private Class cls;
    private Class stopClass;
    private int flag;
    private PropertyDescriptor[] pd;

    public BeanObjectDesc(Class cls) throws IntrospectionException {
	this(cls, null);
    }

    public BeanObjectDesc(Class cls, Class stopClass) throws IntrospectionException {
	this(cls, stopClass, Introspector.IGNORE_ALL_BEANINFO);
    }

    public BeanObjectDesc(Class cls, Class stopClass, int flag) throws IntrospectionException {
	this.cls = cls;
	this.stopClass = stopClass;
	this.flag = flag;
	BeanInfo beanInfo = getBeanInfo(cls, stopClass);
	this.pd = beanInfo.getPropertyDescriptors();
    }

    BeanInfo getBeanInfo(Class targetClass, Class stopClass)
	throws IntrospectionException {
	if (stopClass == null) {
	    return Introspector.getBeanInfo(targetClass, flag);
	} else {
	    return Introspector.getBeanInfo(targetClass, stopClass);
	}
    }

    public Method[] getMethods() {
	try {
	    BeanInfo beanInfo;
	    if (stopClass != null) {
		beanInfo = Introspector.getBeanInfo(cls, stopClass);
	    } else {
		beanInfo = Introspector.getBeanInfo(cls, flag);
	    }
	    MethodDescriptor[] methodDesc = beanInfo.getMethodDescriptors();
	    Method[] m = new Method[methodDesc.length];
	    for (int i = 0; i < methodDesc.length; i++) {
		m[i] = methodDesc[i].getMethod();
	    }
	    return m;
	} catch (IntrospectionException e) {
	    return new Method[] {};
	}
    }

    public void handleProperties(PropertyHandler handler){
	for (int i = 0; i < pd.length; i++){
	    PropertyDescriptor p = pd[i];
	    handler.handle(p.getName(), p.getPropertyType(), p.getReadMethod(), p.getWriteMethod());
	}
    }
}
