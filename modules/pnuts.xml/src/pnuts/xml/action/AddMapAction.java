/*
 * @(#)AddMapAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import pnuts.beans.BeanUtil;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * This action constructs a Map object from values that two child-elements
 * produce.  This action is created with a <em>keyword</em> and the <em>value</em>.
 *
 * e.g.
 * <pre>
 * &lt;map&gt;
 *   &lt;key&gt;<em>age</em>&lt;/key&gt;
 *   &lt;value&gt;<em>0</em>&lt;/value&gt;
 * &lt;/map&gt;
 * </pre>
 * <pre>
 * new AddMapAction("key", "value");
 * </pre>
 */
public class AddMapAction extends CallAction {

	private String keyName;
	private String valueName;

	/**
	 * Constructor
	 *
	 * @param keyName a <em>Keyword</em> name in the rule that specifies which element is a key-part of the resulting Map entries.
	 * @param valueName a <em>Keyword</em> anem in the rule that specifies which element is a value-part of the resulting Map entries.
	 */
	public AddMapAction(String keyName, String valueName){
		super(new String[]{keyName, valueName});
	}

	public void start(String path, String key, Map attributeMap, Object top)
		throws Exception
		{
			if (!path.equals(getStackTopPath())){
				Map map = new HashMap();
				if (top instanceof Map){
					((Map)top).put(key, map);
				} else if (top instanceof List){
					((List)top).add(map);
				} else {
					BeanUtil.setProperty(top, key, map);
				}
				push(path, map);
			}
		}

	protected void call(Object[] args){
		if (args.length != 2){
			throw new IllegalArgumentException();
		}
		Object top = getStackTopValue();
		if (top instanceof Map){
			Map map = (Map)top;
			map.put(args[0], args[1]);
			push(getStackTopPath(), map);
		} else {
			throw new IllegalArgumentException(String.valueOf(top));
		}
	}
}
