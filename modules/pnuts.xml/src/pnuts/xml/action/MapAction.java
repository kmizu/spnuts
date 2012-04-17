/*
 * @(#)MapAction.java 1.2 04/12/06
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
 * This action creates a Map object and adds it to the object on the
 * stack top, in a way that depends on the type of the object.
 * <ul>
 * <li>If a List object is on the stack top, an another Map object of
 * {<em>keyword</em> => the created Map object} is created and added to the List object.
 * <li>If a Map object is on the stack top, a map entry of {<em>keyword</em> => the
 * created Map object} is added to the Map object.
 * <li>Otherwise, this action assigns the created Map object to the Bean
 * property whose name is the <em>keyword</em>.  In this case, the Bean property must be
 * of java.util.Map type.
 * </ul>
 * If the element has one or more attributes, a map entry of {attribute's
 * name=>the value} is added to the created Map object.
 */
public class MapAction extends DigestAction {

	public void start(String path, String key, Map attributeMap, Object top)
		throws Exception
		{
			Map v = new HashMap(attributeMap);
			if (top instanceof Map){
				((Map)top).put(key, v);
				push(path, v);
			} else if (top instanceof List){
				Map m = new HashMap();
				m.put(key, v);
				((List)top).add(m);
				push(getStackTopPath(), m);
			} else {
				BeanUtil.setProperty(top, key, v);
				push(path, v);
			}
		}

	public void end(String path, String key, String text, Object top)
		throws Exception
		{
			pop();
		}
}
