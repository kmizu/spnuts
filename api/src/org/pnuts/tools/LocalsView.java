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
import pnuts.tools.StackFrameInspector;

public class LocalsView extends JPanel implements ContextListener {
    private final static Font monospaced = Font.getFont("monospaced");
    JTable localsTable;
    
    public LocalsView(){
        setLayout(new BorderLayout());
        String[] columnNames = new String[]{"key", "value"};
        this.localsTable = new JTable(new DefaultTableModel(columnNames, 0));
        localsTable.setRowSelectionAllowed(false);
        JScrollPane p = new JScrollPane(localsTable);
        p.setPreferredSize(new Dimension(300, 130));
        add(p);
    }
    
    
    public void update(ContextEvent event){
        DefaultTableModel dtm = (DefaultTableModel)localsTable.getModel();
        dtm.setRowCount(0);
        TreeMap localSymbols = new TreeMap();
        try {
            Context context = event.getContext();
            StackFrameInspector.localSymbols(context, localSymbols);
        } catch (Exception e){}
        for (Iterator it = localSymbols.entrySet().iterator(); it.hasNext(); ){
            Map.Entry entry = (Map.Entry)it.next();
            dtm.addRow(new Object[]{entry.getKey(), Pnuts.format(entry.getValue())});
        }
    }
}
