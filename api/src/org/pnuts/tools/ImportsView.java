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
import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.tools.ContextEvent;
import pnuts.tools.ContextListener;

public class ImportsView extends JPanel implements ContextListener {
    private final static Font monospaced = Font.getFont("monospaced");
    JList importList;
    
    public ImportsView(){
        setLayout(new BorderLayout());
	this.importList = new JList();
	importList.setFont(monospaced);
	importList.setFocusable(false);
	importList.setSelectionBackground(importList.getBackground());
	JScrollPane p = new JScrollPane(importList);
	p.setPreferredSize(new Dimension(300, 70));
        add(p);
    }
    
        
    public void update(ContextEvent event){
        Context context = event.getContext();
        importList.setListData((String[])PnutsFunction.IMPORT.call(new Object[]{}, context));
    }
}
