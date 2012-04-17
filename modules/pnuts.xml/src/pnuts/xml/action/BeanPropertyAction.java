/*
 * @(#)BeanPropertyAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import pnuts.xml.DigestAction;
import pnuts.beans.BeanUtil;
import java.util.List;

/**
 * This action sets a Bean property of the object on the stack top,
 * using <em>keyword</em> and the content of the element.
 * <ul>
 * <li>If a List object is on the stack top,  it creates an instance of
 * the class passed to the constructor,  and push it onto the stack. 
 * <li>Regardless of the type of the stack top object,  if
 * the element has non-empty content,  this action assigns the content's
 * value to the Bean property whose name is the <em>keyword</em>.  If the
 * Bean property is of a primitive type, the content's value is
 * automatically converted to the appropriate value.
 * </ul>
 */
public class BeanPropertyAction extends DigestAction {
	static Object[] NOARGS = new Object[]{};
	static Class[] NOTYPES = new Class[]{};

	Class cls;

	protected BeanPropertyAction(){
	}

	public BeanPropertyAction(Class cls){
		this.cls = cls;
	}

	/**
	 * Sets the bean property "key".
	 * A subclass may override this method to convert the string to an appropriate type.
	 *
	 * @param bean the target object
	 * @param key the property name
	 * @param text the value
	 */
	protected void setBeanProperty(Object bean, String key, String text) throws Exception {
		BeanHelper.setBeanProperty(bean, key, text);
	}

	public void end(String path, String key, String text, Object top)
		throws Exception
		{
			if (top instanceof List){
				Object bean = cls.newInstance();
				((List)top).add(bean);
				setBeanProperty(bean, key, text);
				push(getStackTopPath(), bean);
			} else {
				setBeanProperty(top, key, text);
			}
		}
}
