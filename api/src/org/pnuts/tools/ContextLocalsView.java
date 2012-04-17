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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.tools.ContextEvent;
import pnuts.tools.ContextListener;

public class ContextLocalsView extends JPanel implements ContextListener {
    private final static Font monospaced = Font.getFont("monospaced");
    JTable contextLocals;
    
    public ContextLocalsView(){
        setLayout(new BorderLayout());
        String[] columnNames = new String[]{"key", "value"};
        this.contextLocals = new JTable(new DefaultTableModel(columnNames, 0));
        contextLocals.setRowSelectionAllowed(false);
        JScrollPane p = new JScrollPane(contextLocals);
        p.setPreferredSize(new Dimension(300, 70));
        add(p);
    }
    
    public void update(ContextEvent event){
	Context context = event.getContext();
        DefaultTableModel dtm = (DefaultTableModel)contextLocals.getModel();
        TreeMap tmap = new TreeMap();
        Enumeration keys = context.keys();
        while (keys.hasMoreElements()){
            String name = (String)keys.nextElement();
            tmap.put(name, context.get(name));
        }
        dtm.setRowCount(0);
        for (Iterator it = tmap.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Map.Entry)it.next();
            dtm.addRow(new Object[]{entry.getKey(), Pnuts.format(entry.getValue())});
        }
    }
}
