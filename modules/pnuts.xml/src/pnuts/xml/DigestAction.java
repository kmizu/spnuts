/*
 * @(#)DigestAction.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import org.xml.sax.*;
import java.util.*; 

/**
 * A DigestAction is associated with a path pattern and defines an action that
 * is invoked when a XML parser sees beginning/end of the XML elements that match the pattern.
 *
 * @see DigestHandler
 */
public class DigestAction {

	protected DigestHandler handler;

	public DigestAction(){
	}

	/**
	 * The method called when XML parser looks an element of the associated path starts.
	 *
	 * @param path the path of the element, which the element hierarchy is
	 *  denoted as a '/'-separated string.
	 * @param key the key to acces the result.
	 * @param attributeMap a Map object created from org.xml.sax.Attributes object.
	 * @param top the top of the value stack.
	 */
	public void start(String path, String key, Map attributeMap, Object top)
		throws Exception
		{
		}

	/**
	 * The method called when XML parser looks an element of the associated path ends.
	 *
	 * @param path the path of the element, which the element hierarchy is
	 *  denoted as a '/'-separated string.
	 * @param key the key to acces the result.
	 * @param text the text element as a String object
	 * @param top the top of the value stack.
	 */
	public void end(String path, String key, String text, Object top)
		throws Exception
		{
		}

	/**
	 * Pushes a values to the stack top.
	 *
	 * @param path the associated path
	 * @param value the value to be pushed
	 */
	protected void push(String path, Object value){
		handler.pushValue(path, value);
	}

	/**
	 * Pushes a values to the stack top.
	 *
	 * @param value the value to be pushed
	 */
	protected void push(Object value){
		push(handler.getStackTopPath(), value);
	}

	/**
	 * Pops a value from the stack top
	 *
	 * @return the value
	 */
	protected Object pop(){
		return handler.popValue();
	}

	/**
	 * Gets the stack top without poping
	 *
	 * @return the object at the stack top
	 */
	protected Object getStackTopValue(){
		return handler.getStackTopValue();
	}
	
	/**
	 * Gets the associated path of the stack top object
	 *
	 * @return the path
	 */
	protected String getStackTopPath(){
		return handler.getStackTopPath();
	}

	/**
	 * Registers <em>list</em> for the specified <em>path</em>.
	 * The registered <em>list</em> is unregistered when different branch from the one
	 * the list is registered with, or an element of parent path is found by the parser.
	 */
	protected void registerListPath(String path, Object list){
		handler.registerListPath(path, list);
	}

	/**
	 * Checks if the list registered with <em>path</em> is still managed by the DigestReader.
	 *
	 * @param path the path
	 * @return true if it is still managed by the DigestReader.
	 */
	protected boolean listAlive(String path){
		return handler.listAlive(path);

	}

	/**
	 * Returns the most recent managed list.
	 *
	 * @return the list object
	 */
	protected Object currentListValue(){
		return handler.currentListValue();
	}
}
