/*
 * PnutsConsole.java
 *
 * Copyright (c) 2004-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;

public class PnutsConsole extends Console {
    final static String THREAD_NAME = "Pnuts Console";
    public final static String GREETING = "pnuts.tools.PnutsConsole.greeting";
    public final static String INPUTLOG = "pnuts.tools.PnutsConsole.inputlog";
    
    boolean greeting = true;
    Thread th;
    String inputlog;
    Reader reader;
    Context context;
    ClassLoader classLoader;
    Runnable terminationCallback;
    String[] modules;
    int priority = Thread.NORM_PRIORITY;
    
    /**
     * Constructor
     */
    public PnutsConsole() {
    }

    public void setModules(String[] modules){
        this.modules = modules;
    }
    
    public String[] getModules(){
        return this.modules;
    }
    
    public void setGreeting(boolean flag){
        this.greeting = flag;
    }
    
    public boolean getGreeting(){
        return this.greeting;
    }
    
    public void setContext(Context context){
        this.context = context;
    }
    
    public Context getContext(){
        return this.context;
    }
    
    public void setClassLoader(ClassLoader cl){
        this.classLoader = cl;
    }
    
    public ClassLoader getClassLoader(){
        return this.classLoader;
    }
    
    public void setInputLog(String name){
        this.inputlog = name;
    }
    
    public String getInputLog(){
        return this.inputlog;
    }
    
    public void setPriority(int priority){
        this.priority = priority;
    }
    
    public int getPriority(){
        return this.priority;
    }
    
    
    public void setTerminationCallback(Runnable terminationCallback){
        this.terminationCallback = terminationCallback;
    }

    public Runnable getTerminationCallback(){
        return this.terminationCallback;
    }
    
    public void start(){
        if (this.ui == null){
            throw new IllegalStateException("no UI ");
        }
        final Writer w = getWriter();
        final Context c;
        if (context == null) {
            c = new CancelableContext();
        } else {
            c = context;
        }
	 if (classLoader != null){
	    c.setClassLoader(classLoader);
	 }
	 if (modules != null){
	     for (int i = 0; i < modules.length; i++){
		 c.usePackage(modules[i]);
	     }
	 }
        c.setWriter(w);
        c.setErrorWriter(w);
        c.setTerminalWriter(w);
        final Reader r = getReader();

        th = new Thread(new Runnable() {
            public void run() {
                if (greeting){
                    Main.greeting(c);
                }
                Pnuts.load(r, true, c);
                close();
                try {
                    w.close();
                } catch (IOException e) {
                    /* ignore */
                }
                try {
                    r.close();
                } catch (IOException e) {
                    /* ignore */
                }
            }
        }, THREAD_NAME);
        th.setDaemon(true);
        th.setPriority(priority);
        if (classLoader != null){
            th.setContextClassLoader(classLoader);
        }
        
        th.start();
    }
    
    protected void close(){
        if (terminationCallback != null){
            terminationCallback.run();
        }
        ui.close();
    }
    
    public void dispose(){
        try {
            if (context instanceof CancelableContext){
                ((CancelableContext)context).cancel();
            }
            enter("quit()\n");
            getReader().close();
            getWriter().close();
            if (th != null) {
                th.interrupt();
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
    }
    
    public synchronized Reader getReader() {
        if (this.reader == null) {
            if (inputlog != null) {
                try {
                    this.reader = new LogReader(super.getReader(), inputlog);
                } catch (IOException e) {
                    this.reader = super.getReader();
                }
            } else {
                this.reader = super.getReader();
            }
        }
        return reader;
    }
}
