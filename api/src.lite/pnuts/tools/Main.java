/*
 * @(#)Main.java 1.2 05/06/21
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.Runtime;
//import pnuts.security.SecurePnutsImpl;

/**
 * The main class of "pnuts" command
 */
public class Main {

	private final static String pnutsDebuggerProperty = "pnuts.debugger";
	static String java_vendor = System.getProperty("java.vendor");
	static String java_version = System.getProperty("java.version");

	static String version() {
		try {
			Class.forName("java.lang.Package");
			java.lang.Package pkg = Pnuts.class.getPackage();
			return pkg.getSpecificationVersion() + " ("
					+ pkg.getImplementationVersion() + ")";
		} catch (ClassNotFoundException e) {
			return Pnuts.pnuts_version;
		}
	}

	static void greeting(Context context) {
		PrintWriter w = context.getTerminalWriter();
		if (java_vendor == null) {
			java_vendor = System.getProperty("java.vendor");
		}
		w.print("Pnuts version " + version() + ", " + java_version + " ("
				+ java_vendor + ")");
		String vm_name = System.getProperty("java.vm.name");
		String vm_info = System.getProperty("java.vm.info");
		if (vm_name != null) {
			w.print("\n" + vm_name);
			if (vm_info != null) {
				w.println(" (" + vm_info + ")");
			} else {
				w.println();
			}
		} else {
			w.println();
		}
	}

	static void printHelp(String command) {
		try {
			InputStream in = Main.class.getResourceAsStream(command + ".help");
			Reader reader = new InputStreamReader(in, "UTF-8");
			PrintWriter writer = new PrintWriter(System.err);
			char[] buf = new char[512];
			int n;
			while ((n = reader.read(buf, 0, 512)) != -1) {
				writer.write(buf, 0, n);
			}
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * starts the command shell interpreter
	 */
	public static void main(String args[]) throws Throwable {
		String param = null;

		Context context = null;
		Properties defaultSettings = new Properties();

		boolean evaluated = false;
		boolean interactive = false;
		boolean quiet = false;
//		String inputlog = null;
//		ContextFactory contextFactory = null;

		String iparam = Runtime.getProperty("pnuts.interactive");
		if (iparam != null) {
			if ("false".equals(iparam)) {
				quiet = true;
			} else if ("true".equals(iparam)) {
				interactive = true;
			}
		}

		boolean use_compiler = true;
		boolean optimize = false;
//		boolean debug_mode = false;
//		boolean accessible = false;
//		boolean publicAccess = false;
//		boolean guiConsole = false;
//		boolean sandbox = false;
		boolean verbose = false;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("-help".equals(arg)) {
				printHelp("pnuts");
				System.exit(0);
			} else if ("-version".equals(arg)) {
				System.err.println(version());
				System.exit(0);
//			} else if ("-vd".equals(arg)) {
//				defaultSettings.setProperty(pnutsDebuggerProperty,
//						"pnuts.tools.VisualDebugger");
//				contextFactory = new VisualDebugger();
//				use_compiler = false;
//				debug_mode = true;
//			} else if (arg.startsWith("-d")) {

//				String pnutsContextFactory = Runtime
//						.getProperty(pnutsDebuggerProperty);
//				if (pnutsContextFactory != null) {
//					Class cls = Class.forName(pnutsContextFactory);
//					contextFactory = (ContextFactory) cls.newInstance();//
//				} else {
//					if (arg.length() > 3 && arg.charAt(2) == ':') { // -d:file
//						String debug_script = arg.substring(3);
//						System.out.println("reading " + debug_script);
//						Reader reader = new FileReader(debug_script);////
//						contextFactory = new TerminalDebugger(reader);
//					} else {
//						contextFactory = new TerminalDebugger();
//					}
//				}
//				use_compiler = false;
//				debug_mode = true;
			} else if ("-pure".equals(arg)) {
				use_compiler = false;
			} else if ("-O".equals(arg)) {
				optimize = true;
//			} else if ("-a".equals(arg)) {
//				accessible = true;
			} else if ("-b".equals(arg)) {
				// default
//			} else if ("-p".equals(arg)) {
//				publicAccess = true;
			} else if ("-v".equals(arg)) {
				verbose = true;
//			} else if ("-w".equals(arg)) {
//				guiConsole = true;
//			} else if ("-s".equals(arg)) {
//				sandbox = true;
//			} else if ("-inputlog".equals(arg)) {
			} else if ("-a".equals(arg) || "-u".equals(arg) || "-U".equals(arg)
					|| "-f".equals(arg) || "-F".equals(arg) || "-r".equals(arg)
					|| "-R".equals(arg) || "-e".equals(arg) || "-m".equals(arg)) {
				i++;
			} else if (arg.startsWith("-J")) {
			} else {
				break;
			}
		}

		/*
		 * PnutsImpl
		 */
		if (use_compiler) {
			defaultSettings.setProperty("pnuts.lang.defaultPnutsImpl",
					"pnuts.compiler.CompilerPnutsImpl");
//			if (!accessible && !"false".equals(System.getProperty("pnuts.compiler.useDynamicProxy"))) {
			if (!"false".equals(System.getProperty("pnuts.compiler.useDynamicProxy"))) {
				defaultSettings.setProperty("pnuts.compiler.useDynamicProxy", "true");
			}
			if (optimize) {
				defaultSettings.setProperty("pnuts.compiler.optimize", "true");
			}
		}

		if (verbose) {
			defaultSettings.setProperty("pnuts.verbose", "true");
		}

		Pnuts.setDefaults(defaultSettings);

		/*
		 * Debugger
		 */
//		if (debug_mode) {
//			context = contextFactory.createContext();
//		} else {
			context = new Context();
//		}

		/*
		 * Configuration
		 */
//		if (accessible) {
//			context.setConfiguration(new pnuts.ext.NonPublicMemberAccessor());
//		} else if (publicAccess) {
//			context.setConfiguration(new pnuts.ext.PublicMemberAccessor());
//		}

		/*
		 * sandbox
		 */
//		if (sandbox && Pnuts.isJava2()) {
//			if (System.getSecurityManager() == null) {
//				System.setSecurityManager(new SecurityManager());
//			}
//			context.setImplementation(new SecurePnutsImpl(context
//					.getImplementation()));
//		}

		String module_property = Runtime.getProperty("pnuts.tools.modules");
		if (module_property != null) {
			StringTokenizer stoken = new StringTokenizer(module_property, ",");
			while (stoken.hasMoreTokens()) {
				context.usePackage(stoken.nextToken());
			}
		}

		for (int i = 0; i < args.length; i++) {

			try {
				if ("-r".equals(args[i])) {
					if (++i < args.length) {
						Pnuts.load(args[i], (Context) context.clone());
						continue;
					} else {
						break;
					}
				} else if ("-R".equals(args[i])) {
					if (++i < args.length) {
						Pnuts.load(args[i], (Context) context.clone());
						evaluated = true;
						continue;
					} else {
						break;
					}
				} else if ("-f".equals(args[i])) {
					if (++i < args.length) {
						Pnuts.loadFile(args[i], (Context) context.clone());
						continue;
					} else {
						break;
					}
				} else if ("-F".equals(args[i])) {
					if (++i < args.length) {
						Pnuts.loadFile(args[i], (Context) context.clone());
						evaluated = true;
						continue;
					} else {
						break;
					}
				} else if ("-u".equals(args[i])) {
					if (++i < args.length) {
						Pnuts.load(new URL(args[i]), (Context) context.clone());
						continue;
					} else {
						break;
					}
				} else if ("-U".equals(args[i])) {
					if (++i < args.length) {
						Pnuts.load(new URL(args[i]), (Context) context.clone());
						evaluated = true;
						continue;
					} else {
						break;
					}
				} else if ("-e".equals(args[i])) {
					if (++i < args.length) {
						Pnuts.eval(args[i], (Context) context.clone());
						evaluated = true;
						continue;
					} else {
						break;
					}
				} else if ("-m".equals(args[i])) {
					if (++i < args.length) {
						context.usePackage(args[i]);
						continue;
					} else {
						break;
					}
				} else if ("-encoding".equals(args[i])) {
					if (++i < args.length) {
						context.setScriptEncoding(args[i]);
						continue;
					} else {
						break;
					}
//				} else if ("-inputlog".equals(args[i])) {
//					if (++i < args.length) {
//						inputlog = args[i];
//						continue;
//					} else {
//						break;
//					}
				} else if (args[i].startsWith("-pure")) {
				} else if (args[i].startsWith("-O")) {
				} else if (args[i].startsWith("-a")) {
				} else if (args[i].startsWith("-b")) {
				} else if (args[i].startsWith("-p")) {
				} else if (args[i].startsWith("-d")) {
				} else if (args[i].startsWith("-vd")) {
				} else if ("-v".equals(args[i])) {
//				} else if ("-w".equals(args[i])) {
//				} else if ("-s".equals(args[i])) {
					/* skip */
				} else {
					evaluated = true;
					String[] scriptArgs = new String[args.length - i];
					System.arraycopy(args, i, scriptArgs, 0, scriptArgs.length);
					context.getCurrentPackage().set("$args".intern(),
							scriptArgs);
					Pnuts.loadFile(args[i], (Context) context.clone());
					break;
				}
			} catch (Throwable t) {
				Runtime.printError(t, context);
				return;
			}
		}

		if (!interactive && evaluated) {
			return;
		}

		if (quiet) {
			interactive = false;
		} else if (!interactive) {
			int av = 0;
			try {
				av = System.in.available();
			} catch (IOException e) {
			}
			if (av < 1) {
				interactive = true;
			}
		}

		if (interactive) {
//			if (guiConsole) {
//				PnutsConsole console = new PnutsConsole(null, context, inputlog, true);
//				JFrame f = console.getFrame();
//				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//				f.setVisible(true);
//				Reader in = console.getReader();
//				if (contextFactory instanceof TerminalDebugger) {
//					((TerminalDebugger) contextFactory)
//							.setInput(new BufferedReader(in));
//				}
//			} else {
				InputStream in;
//				if (Pnuts.isJava2()){
					in = System.in;
//				} else {
//					in = new TerminalInputStream(System.in);
//				}
				greeting(context);
				Reader r = Runtime.getScriptReader(in, context);
//				if (inputlog != null) {
//					try {
//						r = new LogReader(r, inputlog);
//					} catch (IOException e) {
//					}
//				}
//				if (contextFactory instanceof TerminalDebugger) {
//					((TerminalDebugger) contextFactory)
//							.setInput(new BufferedReader(r));
//				}
				Pnuts.load(r, true, context);
//			}
		} else {
			try {
				Pnuts.load(System.in, false, context);
			} catch (Throwable t) {
				Runtime.printError(t, context);
			}
		}
	}
}
