/*
 * @(#)TerminalDebugger.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import pnuts.lang.Context;
import pnuts.lang.Function;
import pnuts.lang.Pnuts;
import pnuts.lang.Runtime;
import pnuts.lang.SimpleNode;
import pnuts.lang.PnutsParserTreeConstants;

/**
 * This class implements a debugger for Pnuts interpreter.
 * It is used through <tt>pnuts -d</tt> command.
 * <pre>
 * Commands:
 *   stop at [FILE:]LINENO
 *	   Stop execution at the LINENO
 *   stop in FUNC[:NARGS]
 *	   Stop execution when FUNC is called.
 *	   When NARGS is specified, stop when FUNC with NARGS is called.
 *   clear
 *	   Clear all breakpoints
 *   cont
 *	   Continue execution
 *   trace
 *	   Toggle trace mode
 *   trace function [FUNC]
 *	   Toggle function call trace mode
 *   step [NUM]
 *	   Single step NUM lines.  The default number is 1.
 *   step up
 *	   Step out of the current function
 *   next [NUM]
 *	   Step NUM line (step OVER calls).  The default number is 1.
 *   help
 *	   Print a summary of commands
 *   ?
 *	   Same as help.
 *  </pre>
 */
public class TerminalDebugger implements Debugger, ContextFactory {
	private static final boolean DEBUG = false;

	private Hashtable bpt_functions;
	private Hashtable bpt_files;
	private BufferedReader reader;
	private boolean initialized = false;

	private boolean step = false;
	private boolean step_up = false;
	private boolean next = false;
	private int nsteps;
	private int nnexts;
	private int c_depth;
	private int initialEvalDepth;
	private int initialCallDepth;
	private int e_depth;
	private Object file;
	private int line;
	private boolean trace_lines;
	private boolean trace_all_functions;
	private Hashtable trace_functions;
	private boolean interactive;

	private boolean session = false;

	private static String indent = " >>> ";

	public TerminalDebugger(){
		this(new InputStreamReader(System.in));
		interactive = true;
	}

	/**
	 * @param reader	debug script to read in
	 */
	public TerminalDebugger(Reader reader){
		initialized = false;
		interactive = false;
		if (reader instanceof BufferedReader){
			this.reader = (BufferedReader)reader;
		} else {
			this.reader = new BufferedReader(reader);
		}
	}

	public Context createContext(){
		DebugContext dc = new DebugContext();
		dc.addCommandListener(this);
		return dc;
	}

	void init(DebugContext dc){
		dc.setDebugger(this);
		initialEvalDepth = Pnuts.evalDepth(dc);
		nsteps = 1;
		nnexts = 1;
		c_depth = 0;
		e_depth = 0;
		file = null;
		line = 0;
		trace_lines = false;
		trace_all_functions = false;
		trace_functions = new Hashtable();
		bpt_functions = new Hashtable();
		bpt_files = new Hashtable();
	}

	/**
	 * @param reader	The target script to be tested
	 */
	void setInput(BufferedReader reader){
		this.reader = reader;
	}

	/**
	 * @see	pnuts.tools.CommandListener
	 */
	void exit(CommandEvent event){
		if (session){
			return;
		}
		DebugContext dc = (DebugContext)event.getSource();
		if (DEBUG){
			System.out.println("depth = " + dc.getEvalDepth() + ", initial depth = " + initialEvalDepth);
		}
		if (dc.getEvalDepth() <= initialEvalDepth &&
			dc.getCallDepth() <= initialCallDepth)
		{
			if (DEBUG){
				System.out.println("initialCallDepth = " + initialCallDepth);
				System.out.println("callDepth = " + dc.getCallDepth());
			}
			PrintWriter term = dc.getTerminalWriter();
			term.println("# Returns " + Pnuts.format(event.getArg()));
			term.flush();
			initialized = false;
		}
	}

	/**
	 * Sets a breakpoint at the specified position
	 *
	 * @param file the script file
	 * @param lineno the line number
	 */
	public void setBreakPoint(Object file, int lineno){
		if (DEBUG){
			System.out.println("setBreakPoint(" + file + ", " + lineno + ")");
		}
		if (file == null){
			return;
		}
		Vector lines = (Vector)bpt_files.get(file);
		if (lines == null){
			lines = new Vector();
			bpt_files.put(file, lines);
		}
		Integer i = new Integer(lineno);
		if (!lines.contains(i)){
			lines.addElement(i);
		}
	}

	Vector getBreakPoints(Object file){
		if (file instanceof URL){
			URL url = (URL)file;
			String f = url.getFile();
			for (Enumeration e = bpt_files.keys(); e.hasMoreElements(); ){
				String key = (String)e.nextElement();
				if (f.endsWith(key)){
					return (Vector)bpt_files.get(key);
				}
			}
		} else if (file instanceof File){
			String f = ((File)file).getPath();
			for (Enumeration e = bpt_files.keys(); e.hasMoreElements(); ){
				String key = (String)e.nextElement();
				if (f.endsWith(key)){
					return (Vector)bpt_files.get(key);
				}
			}
		}
		return null;
	}

	public void setBreakPointInFunction(String func_name){
		if (bpt_functions.get(func_name) == null){
			bpt_functions.put(func_name, func_name);
		}
	}

	public void setBreakPointInFunction(String func_name, int nargs){
		String key = func_name + ":" + nargs;
		if (bpt_functions.get(key) == null){
			bpt_functions.put(key, key);
		}
	}

	public void removeBreakPoint(Object source, int lineno){
		if (source == null){
			return;
		}
		Vector lines = (Vector)bpt_files.get(source);
		if (lines == null){
			return;
		}
		lines.removeElement(new Integer(lineno + 1));
	}

	public void clearBreakPoints(){
		bpt_functions.clear();
		bpt_files.clear();
	}

	SimpleNode getTopNode(SimpleNode node){
		while (node != null && node.id != PnutsParserTreeConstants.JJTEXPRESSIONLIST){
			SimpleNode parent = node.jjtGetParent();
			if (parent == null || parent.id == PnutsParserTreeConstants.JJTBLOCK){
				break;
			}
			node = parent;
		}
		return node;
	}

	public void signal(CommandEvent event){
		DebugContext dc = (DebugContext)event.getSource();
		int eventType = event.getType();
		if (eventType == CommandEvent.EXITED){
			exit(event);
			return;
		} else if (eventType == CommandEvent.EXCEPTION){
			initialized = false;
			return;
		} else if (eventType == CommandEvent.OPEN_FRAME){
			Object[] a = (Object[])event.getArg();
			Function f = (Function)a[0];
			String fname = f.getName();
			Object[] args = (Object[])a[1];
			if (trace_all_functions ||
				(fname != null &&  trace_functions.get(fname) != null))
			{
				int depth = dc.getCallDepth();
				for (int i = 0; i < depth; i++){
					System.err.print(' ');
				}
				String param = Pnuts.format(args);
				param = param.substring(1, param.length() - 1);
				System.err.println(fname + "(" + param + ") =>");
			}
		} else if (eventType == CommandEvent.CLOSE_FRAME){
			Object[] a = (Object[])event.getArg();
			Function f = (Function)a[0];
			String fname = f.getName();
			Object[] args = (Object[])a[1];
			if (trace_all_functions ||
				(fname != null &&  trace_functions.get(fname) != null))
			{
				int depth = dc.getCallDepth();
				for (int i = 0; i < depth; i++){
					System.err.print(' ');
				}
				String param = Pnuts.format(args);
				param = param.substring(1, param.length() - 1);
				System.err.println(fname + "(" + param + ") <=");
			}
		} else {
			SimpleNode node = (SimpleNode)event.getArg();
			if (node != null){
				lineUpdated(dc, node);
			}
		}
	}

	void lineUpdated(DebugContext dc, SimpleNode node){
		if (session){
			return;
		}

		PrintWriter term = dc.getTerminalWriter();
		int beginLine = dc.getBeginLine();
		int endLine = dc.getEndLine();

		if (!initialized){
			init(dc);
			initialCallDepth = dc.getCallDepth();

			Object f = dc.getScriptSource();
			if (f == null){
				f = "?";
			}
			term.println("# Stopped at " + f + ":" + beginLine);
			SimpleNode n = getTopNode(node);
			if (n != null){
				term.print(indent);
				term.println(Runtime.unparse(n, dc));
			}
			c_depth = dc.getCallDepth();
			e_depth = dc.getEvalDepth();
			this.file = dc.getScriptSource();
			this.line = dc.getBeginLine();
			initialized = true;
			session(dc);
			return;
		}
		if (step){
			if (this.file != dc.getScriptSource() ||
				this.line != beginLine ||
				c_depth != dc.getCallDepth())
			{

				this.line = beginLine;
				this.file = dc.getScriptSource();
				c_depth = dc.getCallDepth();

				if (--nsteps < 1){
					Object f = file;
					if (f == null){
						f = "?";
					}
					term.println("# Stopped at " + f + ":" + beginLine);
					SimpleNode n = getTopNode(node);
					if (n != null){
						term.print(indent);
						term.println(Runtime.unparse(n, dc));
					}
					session(dc);
				} else if (trace_lines){
					term.print(file + ":" + line + indent);
					term.println(Runtime.unparse(node, dc));
					term.flush();
				}
			}
		} else if (step_up){
			if (e_depth > dc.getEvalDepth() ||
				e_depth >= dc.getEvalDepth() && c_depth > dc.getCallDepth()){

				this.line = beginLine;
				this.file = dc.getScriptSource();
				this.c_depth = dc.getCallDepth();

				Object f = this.file;
				if (f == null){
					f = "?";
				}
				term.println("# Stopped at " + f + ":" + beginLine);

				SimpleNode n = getTopNode(node);
				if (n != null){
					term.print(indent);
					term.println(Runtime.unparse(n, dc));
				}
				session(dc);
			} else if (trace_lines){
				term.print(file + ":" + line + indent);
				term.println(Runtime.unparse(node, dc));
				term.flush();
			}
		} else if (next && e_depth >= dc.getEvalDepth() && c_depth >= dc.getCallDepth()){
			if (this.file != dc.getScriptSource() ||
				this.line != beginLine ||
				c_depth != dc.getCallDepth()){

				this.line = beginLine;
				this.file = dc.getScriptSource();
				this.c_depth = dc.getCallDepth();

				if (--nnexts < 1){
					Object f = this.file;
					if (f == null){
						f = "?";
					}
					term.println("# Stopped at " + f + ":" + beginLine);
					SimpleNode n = getTopNode(node);
					if (n != null){
						term.print(indent);
						term.println(Runtime.unparse(n, dc));
					}
					session(dc);
				}
			} else if (trace_lines){
				term.print(file + ":" + line + indent);
				term.println(Runtime.unparse(node, dc));
				term.flush();
			}
		} else {
			Object file = dc.getScriptSource();
			if (trace_lines){
				term.print(file + ":" + beginLine + indent);
				term.println(Runtime.unparse(node, dc));
				term.flush();
			}
			if (file != null){
				Vector lines = getBreakPoints(file);

				if (lines != null &&
					checkBreakPoint(lines, node.id, beginLine, endLine) &&
					(line != beginLine || c_depth != dc.getCallDepth()))
				{
					this.line = beginLine;
					this.file = dc.getScriptSource();
					this.c_depth = dc.getCallDepth();
					Object f = this.file;
					if (f == null){
						f = "?";
					}
					term.println("# Stopped at " + f + ":" + beginLine);
					SimpleNode n = getTopNode(node);
					if (n != null){
						term.print(indent);
						term.println(Runtime.unparse(n, dc));
					}
					session(dc);
					return;
				}
			}
			if (node.id == PnutsParserTreeConstants.JJTAPPLICATIONNODE){
				String name = node.jjtGetChild(0).str;
				if (name != null && bpt_functions.get(name) != null){
					Object f = this.file;
					if (f == null){
						f = "?";
					}
					term.println("# Stopped at " + f + ":" + beginLine);
					SimpleNode n = getTopNode(node);
					if (n != null){
						term.print(indent);
						term.println(Runtime.unparse(n, dc));
					}
					session(dc);
				}
			}
		}
	}

	boolean checkBreakPoint(Vector lines, int nodeID, int begin, int end){
		if (DEBUG){
			System.out.println("checkBreakPoint(" + begin + ", " + end + ")");
		}
		if (nodeID == PnutsParserTreeConstants.JJTBLOCK){
			return false;
		}
		for (Enumeration e = lines.elements(); e.hasMoreElements(); ){
			int bp = ((Integer)e.nextElement()).intValue();
			if (bp >= begin && bp <= end){
				return true;
			}
		}
		return false;
	}

	String getCommand(){
		String cmd = null;
		try {
			cmd = reader.readLine();
		} catch (IOException e){}
		return cmd;
	}

	void session(DebugContext context){
		step = false;
		step_up = false;
		next = false;

		while (true){

			file = context.getScriptSource();
			c_depth = context.getCallDepth();
			e_depth = context.getEvalDepth();

			PrintWriter term = context.getTerminalWriter();

			if (interactive){
				term.print("debug> ");
				term.flush();
			}

			String cmd = getCommand();
			if (!interactive){
				term.println("debug> " + cmd);
			}

			if (cmd == null){
				break;
			}
			int offset = 0;;
			if ((offset = cmd.indexOf("stop at ")) >= 0){
				String arg = cmd.substring(offset + "stop at ".length()).trim();
				int idx = arg.indexOf(':');
				if (idx < 0){
					Object f = context.getScriptSource();
					String s = null;
					if (f instanceof URL){
						s = ((URL)f).getFile();
					} else if (f instanceof File){
						s = ((File)f).getPath();
					} else if (f instanceof Runtime){
						s = f.getClass().getName();
					}
					setBreakPoint(s, Integer.parseInt(arg));
				} else {
					setBreakPoint(arg.substring(0, idx), Integer.parseInt(arg.substring(idx + 1)));
				}
			} else if ((offset = cmd.indexOf("stop in ")) >= 0){
				String arg = cmd.substring(offset + "stop at ".length()).trim();
				int idx = arg.indexOf(':');
				if (idx < 0){
					setBreakPointInFunction(arg);
				} else {
					setBreakPointInFunction(arg.substring(0, idx), Integer.parseInt(arg.substring(idx + 1)));
				}
			} else if ("help".equals(cmd) || "?".equals(cmd)){
				try {
					ResourceBundle rb = ResourceBundle.getBundle("pnuts.tools.debug");
					String help = rb.getString("pnuts.debug.help");
					term.println(help);
				} catch (MissingResourceException mis){
					mis.printStackTrace(term);
				}
			} else if ((offset = cmd.indexOf("step")) >= 0){
				String arg = cmd.substring(offset + "step".length()).trim();
				if (arg.indexOf("up") >= 0){
					file = context.getScriptSource();
					c_depth = context.getCallDepth();
					e_depth = context.getEvalDepth();
					step_up = true;
				} else if (arg.length() > 0 && Character.isDigit(arg.charAt(0))){
					step = true;
					nsteps = Integer.parseInt(arg);
				} else {
					step = true;
					nsteps = 1;
				}
				break;
			} else if ((offset = cmd.indexOf("next")) >= 0){
				String arg = cmd.substring(offset + "step".length()).trim();
				if (arg.length() > 0 && Character.isDigit(arg.charAt(0))){
					nnexts = Integer.parseInt(arg);
				} else {
					nnexts = 1;
				}
				next = true;
				c_depth = context.getCallDepth();
				e_depth = context.getEvalDepth();
				file = context.getScriptSource();
				break;
			} else if ((offset = cmd.indexOf("trace function")) >= 0){
				String arg = cmd.substring(offset + "trace_function".length()).trim();
				if (arg.length() > 0){
					if (trace_functions.get(arg) == null){
						trace_functions.put(arg, arg);
					} else {
						trace_functions.remove(arg);
					}
				} else {
					trace_all_functions = !trace_all_functions;
					if (trace_all_functions){
						term.println("on");
					} else {
						term.println("off");
					}
				}
		
			} else if ("trace".equals(cmd)){
				trace_lines = !trace_lines;
				if (trace_lines){
					term.println("on");
				} else {
					term.println("off");
				}
			} else if ("cont".equals(cmd)){
				break;
			} else if ("clear".equals(cmd)){
				clearBreakPoints();
			} else {
				try {
					session = true;
					Context c = (Context)context.clone(false, false, true);
					term.println(Pnuts.format(Pnuts.eval(cmd, c)));
				} catch (Throwable t){
					term.println(t);
				} finally {
					session = false;
				}
			}
		}
	}
}
