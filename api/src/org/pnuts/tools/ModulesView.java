/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import pnuts.tools.ContextEvent;
import pnuts.tools.ContextListener;

public class ModulesView extends JPanel implements ContextListener {
    private final static Font monospaced = Font.getFont("monospaced");
    JList moduleList;
    
    public ModulesView(){
        setLayout(new BorderLayout());
	this.moduleList = new JList();
	moduleList.setFont(monospaced);
	moduleList.setFocusable(false);
	moduleList.setSelectionBackground(moduleList.getBackground());
	JScrollPane p = new JScrollPane(moduleList);
	p.setPreferredSize(new Dimension(300, 70));
        add(p);
    }
    
    
    public void update(ContextEvent event){
        moduleList.setListData(event.getContext().usedPackages());
    }    
}
