/*
 * @(#)BeanUtil.java 1.2 04/12/06
 *
 * Copyright (c) 2003, 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.beans;

import java.util.*;
import java.beans.*;
import java.lang.reflect.*;
import pnuts.compiler.DynamicRuntime;

/**
 * This class provides utility methods to access Bean properties.
 */
public class BeanUtil {

	private final static DynamicRuntime rt = new DynamicRuntime();

	protected BeanUtil(){
	}

	/**
	 * Gets a bean property
	 *
	 * @param bean the Bean
	 * @param property the bean property
	 * @return the bean property
	 */
	public static Object getProperty(Object bean, String property)
		throws IllegalAccessException
		{
			return rt.getBeanProperty(bean, property);
		}

	/**
	 * Sets a bean property
	 *
	 * @param bean the Bean
	 * @param property the bean property
	 * @param value the value of the bean property
	 */
	public static void setProperty(Object bean, String property, Object value)
	    throws IllegalAccessException, InvocationTargetException
	{
		rt.setBeanProperty(bean, property, value);
	}

	/**
	 * Gets a bean property type
	 *
	 * @param cls the class
	 * @param property the property name
	 * @return the type of the bean property
	 */
	public static Class getPropertyType(Class cls, String property){
		return rt.getBeanPropertyType(cls, property);
	}


	public static void setProperties(Object bean, Map map)
	    throws IntrospectionException, IllegalAccessException, InvocationTargetException
		{
			BeanInfo info = Introspector.getBeanInfo(bean.getClass());
			PropertyDescriptor[] props = info.getPropertyDescriptors();
			Set keySet = map.keySet();

			for (int j = 0; j < props.length; j++){
				PropertyDescriptor p = props[j];
				String name = p.getName();
				if (keySet.contains(name)){
					Object value = map.get(name);
					Method m = p.getWriteMethod();
					if (m != null){
						rt.setBeanProperty(bean, name, value);
					}
				}
			}
	}
}


