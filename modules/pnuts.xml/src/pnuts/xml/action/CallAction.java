/*
 * @(#)CallAction.java 1.2 04/12/06
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
 * This action does some computation using values popped out of the stack.
 * A subclass defines a concrete implementation.
 */
public abstract class CallAction extends DigestAction {

	private String[] parameterNames;
	private int numberOfParameters;

	/**
	 * Subclass should override this method.
	 *
	 * @param args the arguments.
	 */
	protected abstract void call(Object[] args);

	/**
	 * Constructor
	 */
	protected CallAction(){
		this(-1);
	}

	/**
	 * Constructor
	 *
	 * @param nargs the number of parameters.
	 */
	public CallAction(int nargs){
		this.numberOfParameters = nargs;
	}

	/**
	 * Constructor
	 *
	 * @param parameterNames the names of the parameters.
	 *  This parameter defines both the number of parameter and the order of the parameters.
	 */
	public CallAction(String[] parameterNames){
		this.parameterNames = parameterNames;
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
	 * Calls call(Object[]) method with the specified parameter names.
	 * Arguments are poped from the stack and the result is pushed onto the stack.
	 *
	 * @param parameterNames the parameter names
	 */
	protected void callWithNamedParameters(String[] parameterNames){
		Object obj;
		HashMap map = new HashMap();
		ArrayList args = new ArrayList();
		for (int i = 0; i < parameterNames.length; i++){
			obj = getStackTopValue();
			if (obj instanceof Parameter){
				Parameter param = (Parameter)obj;
				map.put(param.name, param.value);
				pop();
			} else {
				throw new IllegalStateException();
			}
		}
		for (int i = 0; i < parameterNames.length; i++){
			args.add(map.get(parameterNames[i]));
		}
		call(args.toArray());
	}

	/**
	 * Calls call(Object[]) method with the fixed number of arguments.
	 * Arguments are poped from the stack and the result is pushed onto the stack.
	 *
	 * @param nargs the number of arguments
	 */
	protected void callWithFixedNumberOfParameters(int nargs){
		Object obj;
		ArrayList args = new ArrayList();
		for (int i = 0; i < nargs; i++){
			obj = getStackTopValue();
			if (obj instanceof Parameter){
				Parameter param = (Parameter)obj;
				args.add(param.value);
				pop();
			} else {
				throw new IllegalStateException();
			}
		}
		call(args.toArray());
	}

	public void end(String path, String key, String text, Object top)
		throws Exception
		{
			if (parameterNames != null){
				callWithNamedParameters(parameterNames);
			} else if (numberOfParameters >= 0){
				callWithFixedNumberOfParameters(numberOfParameters);
			} else {
				throw new IllegalStateException("either paramterNames or numberOfParameters must be set.");
			}
		}
}
