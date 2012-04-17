/*
 * @(#)ListAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import pnuts.beans.BeanUtil;
import pnuts.xml.DigestAction;
import java.util.Iterator;
import java.util.Stack;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * This action creates a List object and adds it to the object on the stack top, in a way that
 * depends on the type of the object.
 * <ul>
 * <li>If a List object is on the stack top, the created object is added to the List object.
 * <li>If a Map object is on the stack top, a map entry of {<em>keyword</em> => the created object}
 * is added to the Map object.
 * <li>Otherwise,  this action assigns the created object to the Bean
 * property whose name is the <em>keyword</em>.  In this case, the Bean property must be of java.util.List type.
 * </ul>
 * 
 * <div>Some elements may be added to the created List object, according to the following rules. </div>
 * <ul>
 * <li>If the content of the element is not empty, the content is added to the List object.
 * <li>If the element has one or more child-elements, the values produced by those elements are added to the List object.
 * <li>If the element has one or more attributes, a Map object of {attribute's name => the value} is created and added to the List object.
 * </ul>
 */
public class ListAction extends DigestAction {

	Stack attributeStack = new Stack();

	protected Object create(String path, String key, Map attributeMap, Object top)
		throws Exception
		{
			Object v = new ArrayList();
			if (top instanceof Map){
				((Map)top).put(key, v);
			} else if (top instanceof List){
				((List)top).add(v);
			} else {
				BeanUtil.setProperty(top, key, v);
			}
			return v;
		}

	public void start(String path, String key, Map attributeMap, Object top)
		throws Exception
		{
			if (!listAlive(path)){
				Object v = create(path, key, attributeMap, top);
				push(path, v);
				registerListPath(path, v);
			}
			attributeStack.push(new HashMap(attributeMap));
		}

	public void end(String path, String key, String text, Object top)
		throws Exception
		{
			Map attr = (Map)attributeStack.pop();

			if (text.length() > 0){
				if (top == currentListValue()){
					((List)top).add(text);
					push(getStackTopPath(), top);
				}
			} else {
				Object value = currentListValue();
				if (top == value){
					if (attr != null){
						((List)top).add(attr);
						push(getStackTopPath(), top);
					}
				} else {
					while (!getStackTopPath().equals(path)){
						pop();
					}
					top = getStackTopValue();
					if (attr != null && top instanceof Map){
						Map m = (Map)top;
						for (Iterator it = attr.entrySet().iterator(); it.hasNext();){
							Map.Entry entry = (Map.Entry)it.next();
							m.put(entry.getKey(), entry.getValue());
						}
					}
					while (getStackTopValue() != value){
						pop();
					}
				}
			}
		}
}
