/*
 * SwingPnutsConsole.java
 *
 */
package org.pnuts.tools;

import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import pnuts.tools.*;

/**
 * Utility class for Swing-based PnutsConsole
 * This class is used by NetBeans Pnuts module.
 */
public class SwingPnutsConsole extends PnutsConsole {
    final static String DEFAULT_TITLE = "Pnuts Console";
    
    /**
     * Creates a new PnutsConsole specifying a container
     *
     * @param container the Container object into which the console UI is embedded
     * @param modules the initial modules to be used
     * @param cl the class loader
     * @param inputlog the input log
     * @param terminationCallback the command object that is called when the console session terminates
     * @param priority the priority of the interpreter thread
     */
    public static PnutsConsole getInstance(Container container,
            String[] modules,
            ClassLoader cl,
            String inputlog,
            boolean greeting,
            Runnable terminationCallback,
            int priority)
    {
        PnutsConsoleUI ui = new PnutsConsoleUI(){
            protected PnutsConsole createConsole(){
                return new SwingPnutsConsole();
            }
        };
        ui.setTitle(DEFAULT_TITLE);
        PnutsConsole c = ui.createConsole(modules, null, cl, inputlog, greeting, terminationCallback, priority);
        if (container != null){
            container.add(ui.getComponent());
        }
        c.start(); 
        ui.getTextArea().requestFocus();
        return c;
    }
    
    public JComponent getComponent(){
        PnutsConsoleUI ui = (PnutsConsoleUI)getConsoleUI();
        return ui.getComponent();
    }

    public JTextArea getTextArea(){
        PnutsConsoleUI ui = (PnutsConsoleUI)getConsoleUI();
        return ui.getTextArea();
    }
    
    public void requestFocus(){
        PnutsConsoleUI ui = (PnutsConsoleUI)getConsoleUI();
        ui.getTextArea().requestFocus();
    } 
}