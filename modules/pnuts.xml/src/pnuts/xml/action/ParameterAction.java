/*
 * @(#)ParameterAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml.action;

import pnuts.xml.DigestAction;

/**
 * This action pushes the content of an element onto the stack, which will be poped from the stack
 * by a <a href="CallAction.html">CallAction</a>.
 */
public class ParameterAction extends DigestAction {

	public void end(String path, String key, String text, Object top)
		throws Exception
		{
			push(getStackTopPath(), new Parameter(key, text));
		}
}


class Parameter {
	String name;
	Object value;

	Parameter(String name, Object value){
		this.name = name;
		this.value = value;
	}
}
