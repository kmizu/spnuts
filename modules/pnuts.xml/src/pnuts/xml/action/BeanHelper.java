/*
 * @(#)BeanHelper.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import pnuts.beans.BeanUtil;

class BeanHelper {
	public static void setBeanProperty(Object bean, String propertyName, String text)
		throws Exception
		{
			Class cls = bean.getClass();
			Object value;
			Class type = BeanUtil.getPropertyType(cls, propertyName);
			if (type == String.class){
				value = text;
			} else if (type == int.class){
				value = Integer.valueOf(text);
			} else if (type == long.class){
				value = Long.valueOf(text);
			} else if (type == float.class){
				value = Float.valueOf(text);
			} else if (type == double.class){
				value = Double.valueOf(text);
			} else if (type == boolean.class){
				value = Boolean.valueOf(text);
			} else if (type == short.class){
				value = Short.valueOf(text);
			} else if (type == byte.class){
				value = Byte.valueOf(text);
			} else {
				throw new IllegalArgumentException("class="+cls.getName()+", property="+propertyName + ", text="+text);
			}
			BeanUtil.setProperty(bean, propertyName, value);
		}
}
