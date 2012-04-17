/*
 * @(#)SimpleCallAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import pnuts.xml.DigestAction;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * This action does some computation with the element's attributes as
 * the parameters.  A subclass defines a concrete implementation.
 */
public abstract class SimpleCallAction extends DigestAction {

	private String[] parameterNames;

	/**
	 * Subclass should override this method.
	 *
	 * @param args the arguments.
	 */
	protected abstract void call(Object[] args);

	/**
	 * Constructor
	 */
	public SimpleCallAction(){
	}

	/**
	 * Constructor
	 *
	 * @param parameterNames the parameter names
	 */
	public SimpleCallAction(String[] parameterNames){
		setParameterNames(parameterNames);
	}

	/**
	 * Sets the parameter names
	 *
	 * @param parameterNames the parameter names
	 */
	public void setParameterNames(String[] parameterNames){
		this.parameterNames = parameterNames;
	}

	/**
	 * Gets the parameter names
	 *
	 */
	public String[] getParameterNames(){
		return this.parameterNames;
	}

	/**
	 * Calls call(Object[]) method, assuming the parameter names are given to the constructor.
	 * The actual parameters are marshaled from the attributes, in the order of the parameter names.
	 *
	 * @param attributeMap the attributes of the start element
	 */
	protected void callWithNamedParameters(Map attributeMap){
		ArrayList args = new ArrayList();
		for (int i = 0; i < parameterNames.length; i++){
			args.add(attributeMap.get(parameterNames[i]));
		}
		call(args.toArray());
	}

	/**
	 * Calls call(Object[]) method with a single argument, a Map object, which contains
	 * { qualified_name => value } mappings.
	 *
	 * @param attributeMap the attributes of the start element
	 */
	protected void call(Map attributeMap){
		call(new Object[]{attributeMap});
	}

	public void start(String path, String key, Map attributeMap, Object top)
		throws Exception
		{
			if (parameterNames != null){
				callWithNamedParameters(attributeMap);
			} else {
				call(attributeMap);
			}
		}
}
