/*
 * @(#)VisualDebuggerModel.java 1.3 05/03/18
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;
import pnuts.lang.Runtime;
import pnuts.lang.SimpleNode;
import pnuts.lang.PnutsParserTreeConstants;

public class VisualDebuggerModel extends Runtime implements Debugger {
	DebugContext dc;
	boolean quit;
	private Object currentSource;
	private int currentLine;
	private int n_nexts;
	private int n_steps;
	private int command;
	private Hashtable files; // source -> lines (break points)
	private boolean initialized = false;
	private int initialEvalDepth;
	private int initialCallDepth;
	private int e_depth;
	private int c_depth;
	private PrintWriter errorStream;
	private Object traceTag;
	private Context parent;
	private VisualDebuggerView view;

	static final int OPEN = 1;
	static final int STEP = 2;
	static final int STEP_UP = 3;
	static final int NEXT = 4;
	static final int CONT = 5;
	static final int CLOSE = 6;
	static final int CLEAR_BP = 7;
	static final int INSPECT = 8;

	public VisualDebuggerModel() {
		this.files = new Hashtable();
	}

	void setView(VisualDebuggerView view) {
		this.view = view;
	}

	void init(DebugContext dc) {
		quit = false;
		this.dc = dc;
		dc.setDebugger(this);
		initialEvalDepth = Pnuts.evalDepth(dc);
		e_depth = 0;
		c_depth = 0;
		n_steps = 1;
		n_nexts = 1;
	}

	public synchronized void do_step(int n) {
		command = STEP;
		n_steps = n;
		notifyAll();
	}

	public synchronized void do_stepup() {
		command = STEP_UP;
		c_depth = dc.getCallDepth();
		e_depth = dc.getEvalDepth();
		notifyAll();
	}

	public synchronized void do_next(int n) {
		command = NEXT;
		n_nexts = n;
		notifyAll();
	}

	public synchronized void do_cont() {
		command = CONT;
		n_steps = 0;
		n_nexts = 0;
		notifyAll();
	}

	public synchronized void do_close() {
		command = CONT;
		quit = true;
		notifyAll();
	}

	public Object getCurrentSource() {
		return currentSource;
	}

	public void setBreakPoint(Object source, int lineno) {
		if (source == null) {
			return;
		}
		Vector lines = (Vector) files.get(source);
		if (lines == null) {
			lines = new Vector();
			files.put(source, lines);
		}
		Integer i = new Integer(lineno + 1);
		if (!lines.contains(i)) {
			lines.addElement(i);
		}
	}

	public void removeBreakPoint(Object source, int lineno) {
		if (source == null) {
			return;
		}
		Vector lines = (Vector) files.get(source);
		if (lines == null) {
			return;
		}
		lines.removeElement(new Integer(lineno + 1));
	}

	public Vector getBreakPoints(Object source) {
		if (source == null) {
			return null;
		}
		return (Vector) files.get(source);
	}

	public void clearBreakPoints() {
		files.clear();
	}

	public void signal(CommandEvent event) {
		this.dc = (DebugContext) event.getSource();

		int eventType = event.getType();

		if (eventType == CommandEvent.EXITED) {
			if (dc.getEvalDepth() <= initialEvalDepth
					&& dc.getCallDepth() <= initialCallDepth) {
				if (!quit) {
					showScript(dc.getScriptSource(), -1, null, dc);
				}
				initialized = false;

			}
			return;
		}

		if (eventType == CommandEvent.LINE_UPDATED) {

			SimpleNode node = (SimpleNode) event.getArg();
			int beginLine = dc.getBeginLine();
			int endLine = dc.getEndLine();
			Object source = dc.getScriptSource();

			if (!initialized) {
				init(dc);
				initialized = true;
				c_depth = dc.getCallDepth();
				e_depth = dc.getEvalDepth();
				initialCallDepth = dc.getCallDepth();

				showScript(source, beginLine, node, dc);
				waitForCommands();
				currentLine = beginLine;
				return;
			}

			if (hasToStop(source, node, beginLine, endLine)) {
				c_depth = dc.getCallDepth();
				e_depth = dc.getEvalDepth();
				showScript(source, beginLine, node, dc);
				waitForCommands();
				currentLine = beginLine;
			}
		} else if (eventType == CommandEvent.EXCEPTION) {
			Throwable t = (Throwable) event.getArg();
			if (t instanceof PnutsException) {
				PnutsException pe = (PnutsException) t;
				Object source = pe.getScriptSource();
				int beginLine = pe.getLine();
				showScript(source, beginLine, null, dc);
			}
			initialized = false;
		}
	}

	boolean checkBreakPoint(Vector lines, SimpleNode node, int begin, int end) {
		if (node != null && node.id == PnutsParserTreeConstants.JJTBLOCK) {
			return false;
		}
		for (Enumeration e = lines.elements(); e.hasMoreElements();) {
			int bp = ((Integer) e.nextElement()).intValue();
			if (bp >= begin && bp <= end) {
				return true;
			}
		}
		return false;
	}

	boolean hasToStop(Object source, SimpleNode node, int beginLine, int endLine) {
		if (source instanceof Runtime) {
			return false;
		}
		if (source == null && node != null && command != CONT) {
			return true;
		}
		if (command == OPEN) {
			currentLine = beginLine;
			command = 0;
			return true;
		} else if (command == STEP) {
			if (--n_steps < 1) {
				return true;
			}
		} else if (command == NEXT) {
			int d1 = dc.getEvalDepth();
			int d2 = dc.getCallDepth();
			if (e_depth >= d1 && c_depth >= d2
					&& (currentSource != source || currentLine != beginLine)) {
				if (--n_nexts < 1) {
					return true;
				}
			}
		} else if (command == STEP_UP) {
			int d1 = dc.getEvalDepth();
			int d2 = dc.getCallDepth();
			if (e_depth > d1 || e_depth >= d1 && c_depth > d2) {
				return true;
			}
		}

		Vector lines = getBreakPoints(source);

		if (lines != null && checkBreakPoint(lines, node, beginLine, endLine)) {
			currentLine = beginLine;
			return true;
		}

		return false;
	}

	synchronized void waitForCommands() {
		command = 0;
		try {
			while (command == 0) {
				wait();
			}
		} catch (InterruptedException e) {
		}
		if (quit) {
			escape(null);
		}
	}

	protected void showScript(Object source, int line, SimpleNode node,
			Context context) {
		if (!quit && view != null) {
			view.update(source, line, node, context);
		}
		currentSource = source;
	}
}
