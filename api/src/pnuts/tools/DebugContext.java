/*
 * @(#)DebugContext.java 1.3 05/03/18
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.util.Enumeration;
import java.util.Vector;

import pnuts.lang.Context;
import pnuts.lang.Function;
import pnuts.lang.Package;
import pnuts.lang.PnutsImpl;
import pnuts.lang.SimpleNode;

/**
 * This class is a Context used in debug mode.
 */
public class DebugContext extends Context {
	private final static boolean DEBUG = false;
	private Vector listeners = null;
	private int callDepth = 0;
	private int lastCallDepth = 0;
	private Debugger debugger;

	public DebugContext(){
		this.setImplementation(new PnutsImpl());  // force pure interpreter
	}

	public DebugContext(Package pkg){
		super(pkg);
		this.setImplementation(new PnutsImpl()); // force pure interpreter
	}

	public DebugContext(Context context){
		super(context);
		this.setImplementation(new PnutsImpl()); // force pure interpreter
	}

	/**
	 * Registers the specified debugger as the controller of this debug context
	 */
	public void setDebugger(Debugger debugger){
		this.debugger = debugger;
	}

	/**
	 * Returns the debugger that controls this debug context
	 */
	public Debugger getDebugger(){
		return this.debugger;
	}

	/**
	 * Tracks stack depth (function call) in pure interpreter.
	 * This method overrides Context.open().
	 */
	protected void open(Function f, Object[] args){
		super.open(f, args);
		++callDepth;
		fireCommandEvent(CommandEvent.OPEN_FRAME, new Object[]{f, args});
	}

	/**
	 * Tracks stack depth (function return) in pure interpreter.
	 * This method overrides Context.close().
	 */
	protected void close(Function f, Object[] args){
		fireCommandEvent(CommandEvent.CLOSE_FRAME, new Object[]{f, args});
		super.close(f, args);
		--callDepth;
	}

	/**
	 * Get the depth of evaluation.
	 *
	 * This value increases when load(), loadFile(), or eval() is called.
	 */
	public int getEvalDepth(){
		return depth;
	}

	/**
	 * Get the stack depth (in pure interpreter)
	 */
	public int getCallDepth(){
		return callDepth;
	}

	protected int getBeginLine(){
		return beginLine;
	}

	protected int getEndLine(){
		return beginLine;
	}

	protected Object getScriptSource(){
		return super.getScriptSource();
	}

	/**
	 * This method is called when line number is changed.
	 */
	protected void updateLine(SimpleNode node, int beginLine, int beginColumn){
		int line = beginLine;
		if (line > 0 && (this.beginLine != line || callDepth != lastCallDepth))
		{
			this.beginLine = line;
			this.lastCallDepth = callDepth;
			fireCommandEvent(CommandEvent.LINE_UPDATED, node);
		}
	}

	/**
	 * This method is called when some exception is thrown.
	 */
	protected void onError(Throwable t){
		fireCommandEvent(CommandEvent.EXCEPTION, t);
	}

	/**
	 * This method is called when an evaluation is terminated normally.
	 */
	protected void onExit(Object arg){
		fireCommandEvent(CommandEvent.EXITED, arg);
	}

	void fireCommandEvent(int type, Object arg){
		if (listeners != null){
			Vector vec = (Vector)listeners.clone();
			CommandEvent event = new CommandEvent(this, type, arg);
			for (Enumeration e = vec.elements(); e.hasMoreElements(); ){
				((CommandListener)e.nextElement()).signal(event);
			}
		}
	}

	public synchronized void addCommandListener(CommandListener listener){
		if (listeners == null){
			listeners = new Vector();
		}
		listeners.addElement(listener);
	}

	public synchronized void removeCommandListener(CommandListener listener){
		listeners.removeElement(listener);
		if (listeners.size() == 0){
			listeners = null;
		}
	}

	public Object clone(boolean clear_attributes,
						boolean clear_locals,
						boolean clear_listeners)
		{
			DebugContext ret = (DebugContext)super.clone(clear_attributes, clear_locals);
			if (clear_listeners){
				ret.listeners = null;
			}
			return ret;
		}
}
