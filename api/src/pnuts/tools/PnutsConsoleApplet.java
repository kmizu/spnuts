/*
 * @(#)PnutsConsoleApplet.java 1.1 05/06/14
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import pnuts.tools.*;
import pnuts.lang.*;
import pnuts.lang.Package;
import pnuts.lang.Runtime;
import pnuts.compiler.*;
import java.awt.*;
import java.security.*;
import java.io.*;
import java.util.*;
import java.applet.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.EmptyBorder;

/**
 * Pnuts Console Applet
 *
 * Usage: <pre>
 * &lt;applet code="pnuts.tools.PnutsConsoleApplet" archive="pnuts.jar,pnuts-modules.jar" codebase="/" width="100%" height="100%"&gt;
 *  &lt;param name="modules" value="pnuts.tools"/&gt;
 * &lt;/applet&gt;
 * </pre>
 */
public class PnutsConsoleApplet extends JApplet {

	private final static String PNUTS_APPLET = "pnuts.applet".intern();

	final static String[] runtimePermissions = {
		"createClassLoader",
		"getProtectionDomain"
	};

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { /* skip */ }
	}

	private Context context;
	private Console console;

        public PnutsConsoleApplet(){
        }
        
	public void init () {
		boolean canCreateClassLoader = false;
		try {
			for (int i = 0; i < runtimePermissions.length; i++){
				RuntimePermission perm =
					new RuntimePermission(runtimePermissions[i], null);
				AccessController.checkPermission(perm);
			}
			canCreateClassLoader = true;
		} catch (SecurityException e){
		}
		Package pkg = new Package("", null);
		this.context = new Context(pkg);
		if (canCreateClassLoader){
			context.setImplementation(new CompilerPnutsImpl());
		} else {
                        context.setImplementation(new PnutsImpl());
		}
		context.set(PNUTS_APPLET, this);

		initializeModuleProperty(Runtime.getProperty("pnuts.tools.modules"), context);
		initializeModuleProperty(getParameter("modules"), context);

		getContentPane().setLayout(new BorderLayout());
		this.console = new Console();
		PnutsConsoleUI ui = new PnutsConsoleUI(console);
		JTextComponent component = ui.getJTextComponent();
		JScrollPane sp = new JScrollPane(component);
		console.setConsoleUI(ui);
		getContentPane().add("Center", sp);
		sp.setBorder(new EmptyBorder(0, 0, 0, 0));

		Writer w = console.getWriter();
		context.setWriter(w);
		context.setTerminalWriter(w);
		context.setErrorWriter(w);
	}

	public void start(){
		Thread th = new Thread(){
			public void run(){
				Pnuts.load(console.getReader(), true, context);
				PrintWriter tw = context.getTerminalWriter();
				tw.println("Terminated.");
				tw.flush();
			}
			};
		th.setDaemon(true);
		th.start();
	}

	static void initializeModuleProperty(String property, Context context){
		if (property != null) {
			StringTokenizer stoken = new StringTokenizer(property, ",");
			while (stoken.hasMoreTokens()) {
				context.usePackage(stoken.nextToken());
			}
		}
	}
}
