/*
 * @(#)CommandEvent.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import pnuts.lang.Context;
import java.util.EventObject;

/**
 * Event object which is created by DebugContext to communicate with a debugger.
 */
public class CommandEvent extends EventObject {

	private Object arg;
	private int eventType;

	/**
	 * The event type which indicates line number has been changed.
	 */
	public final static int LINE_UPDATED = 0;

	/**
	 * The event type which indicates some exception has been thrown.
	 */
	public final static int EXCEPTION = 1;

	/**
	 * The event type which indicates the execution was normally terminated.
	 */
	public final static int EXITED = 2;

	/**
	 * The event type which indicates a function is called.
	 */
	public final static int OPEN_FRAME = 3;

	/**
	 * The event type which indicates a function returned.
	 */
	public final static int CLOSE_FRAME = 4;

	/**
	 * Constructor
	 *
	 * @param context The context which creates the Command Event object.
	 * @param eventType The event type.
	 * @param arg Optional argument.
	 */
	public CommandEvent(Context context, int eventType, Object arg){
		super(context);
		this.arg = arg;
		this.eventType = eventType;
	}

	/**
	 * Get the event type
	 */
	public int getType(){
		return eventType;
	}

	/**
	 * Get the optional argument
	 */
	public Object getArg(){
		return arg;
	}
}
