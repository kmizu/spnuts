/*
 * @(#)TextAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import pnuts.xml.DigestAction;
import pnuts.beans.BeanUtil;

/**
 * This action adds a map entry of {<em>keyword</em> => the content of the
 * element} to the object on the stack top, in a way that depends on the type of the object.
 * <ul>
 * <li>If a List object is on the stack top, a Map object with
 * a {<em>keyword</em> => content} entry is created and pushed onto the stack.
 * <li>If a Map object is on the stack top, a map entry of
 * {<em>keyword</em> => content} is added to the Map object.
 * <li>Otherwise, this action assigns the element's content to the Bean
 * property whose name is the <em>keyword</em>.
 * If the property is of a primitive type, the content is automatically
 * converted to the appropriate value.
 * </ul>
 * 
 */
public class TextAction extends DigestAction {

	protected void setBeanProperty(Object bean, String propertyName, String text)
		throws Exception
		{
			BeanHelper.setBeanProperty(bean, propertyName, text);
		}

	public void end(String path, String key, String text, Object top)
		throws Exception
		{
			if (top instanceof Map){
				((Map)top).put(key, text);
			} else if (top instanceof List){
				HashMap m = new HashMap();
				m.put(key, text);
				((List)top).add(m);
				push(getStackTopPath(), m);
			} else {
				setBeanProperty(top, key, text);
			}
		}
}
