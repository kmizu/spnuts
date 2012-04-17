/*
 * PnutsException.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This is a wrapper class for Exception to be thrown.
 * 
 * @author Toyokazu Tomatsu
 */
public class PnutsException extends RuntimeException {

	static final long serialVersionUID = -5567045200989654942L;

	/**
	 * @serial
	 */
	protected int line;

	/**
	 * @serial
	 */
	protected int column;

	/**
	 * @serial
	 */
	protected Object file;

	/**
	 * @serial
	 */
	protected Throwable throwable;

	/**
	 * @serial
	 */
	protected String contextName;


	protected transient Vector trace;

	protected transient Object operation;

	/**
	 * Constructor
	 */
	public PnutsException() {
		this("");
	}

	/**
	 * Constructor
	 * 
	 * @param msg
	 *            the error message
	 */
	public PnutsException(String msg) {
		super(msg);
		throwable = this;
	}

	/**
	 * Constructor
	 * 
	 * @param msg
	 *            the error message
	 * @param context
	 *            the context in which the error occurs
	 */
	public PnutsException(String msg, Context context) {
		this(msg);

//		while (context.eval && context.parent != null) {
//			context = context.parent;
//		}
		this.file = context.getScriptSource();
		this.line = context.beginLine;
		this.column = context.beginColumn;
		this.contextName = context.getName();
	}

	/**
	 * This constructor creates a PnutsException using i18n resources in
	 * pnuts.properties.
	 */
	public PnutsException(String key, Object param[], Context context) {
		this("pnuts.lang.pnuts", key, param, context);
	}

	/**
	 * This constructor creates a PnutsException using i18n resources in
	 * pnuts.properties.
	 */
	public PnutsException(String bundleName, String key, Object param[],
			Context context) {
		this(Runtime.getMessage(bundleName, key, param), context);
	}

	/**
	 * @deprecated replaced by PnutsException(Throwable, Context)
	 */
	public PnutsException(Throwable t) {
		this.throwable = t;
	}

	/**
	 * Constructor
	 * 
	 * @param t
	 *            a Throwable
	 * @param context
	 *            the context
	 */
	public PnutsException(Throwable t, Context context) {
//		while (context.eval && context.parent != null) {
//			context = context.parent;
//		}

		if (t instanceof PnutsException) {
			PnutsException p = (PnutsException) t;
			this.throwable = p.throwable;
			this.trace = p.trace;
			this.file = p.file;
			this.line = p.line;
			this.column = p.column;
		} else if (t instanceof ParseException) {
			this.throwable = t;
			this.line = ((ParseException) t).getErrorLine();
			this.column = ((ParseException) t).getErrorColumn();
			this.file = context.getScriptSource();
		} else {
			this.throwable = t;
			this.line = context.beginLine;
			this.column = context.beginColumn;
			this.file = context.getScriptSource();
		}
		this.contextName = context.getName();
	}

	/**
	 * Constructor
	 * 
	 * @deprecated
	 * @param t
	 *            a Throwable
	 * @param operation
	 *            a Method or a Constructor
	 * @param context
	 *            the context
	 */
	public PnutsException(Throwable t, Object operation, Context context) {

//		while (context.eval && context.parent != null) {
//			context = context.parent;
//		}

		if (t instanceof PnutsException) {
			PnutsException p = (PnutsException) t;
			this.throwable = p.throwable;
			this.trace = p.trace;
			this.file = p.file;
			this.line = p.line;
			this.column = p.column;
		} else if (t instanceof ParseException) {
			this.throwable = t;
			this.line = ((ParseException) t).getErrorLine();
			this.column = ((ParseException) t).getErrorColumn();
			this.file = context.getScriptSource();
		} else {
			this.throwable = t;
			this.line = context.beginLine;
			this.column = context.beginColumn;
			this.file = context.getScriptSource();
		}
		this.contextName = context.getName();
		this.operation = operation;
	}

	public String getMessage(){
		if (throwable != null && throwable != this) {
			return throwable.getMessage();
		} else {
			return super.getMessage();
		}
	}

	/**
	 * Returns the root cause
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	String position() {
		StringBuffer buf = new StringBuffer();
		if (file != null || contextName != null) {
			buf.append('[');
			if (contextName != null) {
				buf.append(contextName + ", ");
			}
			if (file != null) {
				buf.append(file);
				if (line > 0) {
					buf.append(" line: " + line);
				}
				if (column > 0){
					buf.append(" column: " + column);
				}
			} else if (line > 0) {
				buf.append("line: " + line);
				if (column > 0){
					buf.append(" column: " + column);
				}
			}
			buf.append(']');
		}
		if (buf.length() > 0) {
			buf.append(":\n");
		}
		return buf.toString();
	}

	/**
	 * Returns an enumeration of PnutsException.TraceInfo objects
	 */
	public Enumeration getBackTrace() {
		if (trace != null){
			return trace.elements();
		} else {
			return null;
		}
	}

	void printBackTrace(PrintWriter pw, String indent) {
		for (Enumeration e = trace.elements(); e.hasMoreElements();) {
			pw.print(indent);
			Object elem = e.nextElement();
			if (elem == this){
				pw.println(getClass() + "@" + System.identityHashCode(this));
			} else {
				pw.println(elem);
			}
		}
	}

	String trace() {
		StringWriter sw = new StringWriter();
		printBackTrace(new PrintWriter(sw), "    ");
		return sw.toString();
	}

	public void printStackTrace(PrintWriter writer) {
		if (throwable != null && throwable != this) {
			throwable.printStackTrace(writer);
		} else {
			super.printStackTrace(writer);
		}
	}

	public void printStackTrace(PrintStream ps) {
		if (throwable != null && throwable != this) {
			throwable.printStackTrace(ps);
		} else {
			super.printStackTrace(ps);
		}
	}

	/**
	 * Returns the line number where the error occured.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns the column number where the error occured.
	 * -1: unknown 
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Returns the script source (usually a URL) where the error occured.
	 */
	public Object getScriptSource() {
		return file;
	}

	void backtrace(TraceInfo traceInfo) {
		if (trace == null) {
			trace = new Vector();
		}
		trace.addElement(traceInfo);
	}

	public String toString() {
		String s = position();
		if (trace != null) {
			s += trace();
		}
		if (operation != null) {
			s += operation.toString() + "->";
		}
		return s + throwable.getClass().getName() + " : "
				+ throwable.getMessage();
	}

	/**
	 * A node of a call-chain, which represents a position of a certain function
	 * call
	 */
	public static class TraceInfo {
		Object target;

		Object frame;

		Object[] arguments;

		Object scriptSource;

		int line;

		int column;

		protected TraceInfo() {
		}

		/**
		 * Constructor
		 *
		 * @param frame the function's name or the class of the constructor
		 * @param args the arguments
		 * @param scriptSource the object from which the script was read
		 * @param line the line
		 * @param column the column
		 */
		public TraceInfo(Object frame, Object[] args, Object scriptSource,
				int line, int column) {
			this(null, frame, args, scriptSource, line, column);
		}

		/**
		 * Constructor
		 *
		 * @param target the target object
		 * @param methodName the method name
		 * @param args the arguments
		 * @param scriptSource the object from which the script was read
		 * @param line the line
		 * @param column the column
		 */
		public TraceInfo(Object target, Object methodName, Object[] args, Object scriptSource,
				int line, int column) {
			this.target = target;
			this.frame = methodName;
			this.arguments = args;
			this.scriptSource = scriptSource;
			this.line = line;
			this.column = column;
		}

		/**
		 * Gets the source of the script where the function call was taken
		 * place. It is usually a URL object, though the script source could be
		 * any object.
		 */
		public Object getScriptSource() {
			return scriptSource;
		}

		/**
		 * The actual arguments of the function call
		 */
		public Object[] getArguments() {
			return (Object[])arguments.clone();
		}

		/**
		 * The line number of the place where the function call was taken place.
		 */
		public int getLine() {
			return line;
		}

		/**
		 * The column number of the place where the function call was taken place.
		 */
		public int getColumn(){
			return column;
		}

		/**
		 * Gets the callee that throws an exception.
		 *
		 * @return either of a function name, method name, or Class object.
		 */
		public Object getFrame() {
			return frame;
		}

		/**
		 * Gets the target object of the method call that causes an exception
		 *
		 * @return the target object of the method call that causes an exception
		 */
		public Object getTargetObject(){
			return target;
		}

		public String toString() {
			StringBuffer sbuf = new StringBuffer();
			if (target != null){
			        try {
                                    sbuf.append(Pnuts.format(target));
			        } catch (Throwable t){
                                    sbuf.append("?");
			        }
				sbuf.append(".");
			}
			sbuf.append(String.valueOf(frame));
			try {
				String args = Runtime.format(arguments, 64);
				sbuf.append("(" + args.substring(1, args.length() - 1) + ")");
			} catch (Throwable tt) {
				sbuf.append("(?)");
			}
			if (scriptSource != null) {
				sbuf.append(" [");
				sbuf.append(scriptSource);
				sbuf.append(':');
				sbuf.append(" line: " + line);
				if (column >= 0){
					sbuf.append(" column: " + column);
				}
				sbuf.append(']');
			}
			return sbuf.toString();
		}
	}
}
