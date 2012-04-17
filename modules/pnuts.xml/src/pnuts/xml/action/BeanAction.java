/*
 * @(#)BeanAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import pnuts.xml.DigestAction;
import pnuts.beans.BeanUtil;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import org.xml.sax.Attributes;

/**
 * This action creates an instance of the class, which was passed to the
 * constructor, and adds it to the object on the stack top, in a way that
 * depends on the type of the object. Then it pushes the object on the stack.
 * <ul>
 * <li>If a List object is on the stack top, the created object is added to the List.
 * <li>If a Map object is on the stack top, a map entry of {<em>keyword</em> => the
 * created object} is added to the Map.
 * <li>Otherwise, this action assigns the created object to the Bean property whose name
 * is the <em>keyword</em>.  The type of the Bean property must match the type of the created object.
 * </ul>
 * If the element has one or more attributes, this action assigns the
 * attribute's value to the Bean property, whose name is the name of the
 * attribute.  If the Bean property is of a primitive type, the attribute's
 * value is automatically converted to the appropriate value.
 */
public class BeanAction extends DigestAction {
	static Object[] NOARGS = new Object[]{};
	static Class[] NOTYPES = new Class[]{};

	Class cls;

	protected BeanAction(){
	}

	public BeanAction(Class cls){
		this.cls = cls;
	}

	/**
	 * Defines bean properties from the [qName->value] mapping in the specified Attibutes.
	 *
	 * @param bean the target object
	 * @param attributeMap a [qName->value] mapping.
	 */
	protected void setAttributes(Object bean, Map attributeMap) throws Exception {
		for (Iterator it = attributeMap.entrySet().iterator(); it.hasNext(); ){
			Map.Entry entry = (Map.Entry)it.next();
			setBeanProperty(bean, (String)entry.getKey(), (String)entry.getValue());
		}
	}

	/**
	 * Defines a bean property
	 *
	 * @param bean the target object
	 * @param name the property name
	 * @param text a string
	 */
	protected void setBeanProperty(Object bean, String name, String text) throws Exception {
		BeanHelper.setBeanProperty(bean, name, text);
	}

	public void start(String path, String key, Map attributeMap, Object top)
		throws Exception
		{
			Object bean = cls.newInstance();
			setAttributes(bean, attributeMap);
			if (top instanceof List){
				((List)top).add(bean);
			} else if (top instanceof Map){
				((Map)top).put(key, bean);
				push(path, bean);
			} else {
				BeanUtil.setProperty(top, key, bean);
				push(path, bean);
			}
		}
}
