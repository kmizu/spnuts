/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import pnuts.ext.NonPublicMemberAccessor;
import pnuts.lang.Configuration;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.tools.ContextEvent;
import pnuts.tools.ContextListener;
import pnuts.tools.DebugContext;

public class InspectWatchView extends JPanel implements ContextListener {
    private final static Configuration debugConfig = new NonPublicMemberAccessor();
    private final static Font monospaced = Font.getFont("monospaced");
    
    JTextField inspectField;
    JTextArea result;
    Context currentContext;
    
    public InspectWatchView(){
        inspectField = new JTextField(20);
        inspectField.setBackground(Color.white);
        inspectField.setFont(monospaced);
        inspectField.setAutoscrolls(true);

        result = new JTextArea(2, 0);
        result.setLineWrap(true);
        result.setEditable(false);
        JScrollPane resultpane = new JScrollPane(result);
        resultpane.setPreferredSize(new Dimension(300, 180));
        
        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);
        gb.setConstraints(inspectField, c);
        add(inspectField);

        c.gridy = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        gb.setConstraints(resultpane, c);
        add(resultpane);

        inspectField.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    if (currentContext == null){
                        return;
                    }
                    try {
                        Context c = (Context)((DebugContext)currentContext).clone(false, false, true);
                        c.setConfiguration(debugConfig);
                        Object r = Pnuts.eval(inspectField.getText(), c);
                        result.setText(Pnuts.format(r));
                    } catch (Exception e2){
                        result.setText(e2.toString());
                    }
                }
            });

    }
    
    
    public void update(ContextEvent event){
	this.currentContext = event.getContext();
        String watch = inspectField.getText();
        if (watch != null && watch.length() > 0){
            try {
                Context c = (Context)((DebugContext)currentContext).clone(false, false, true);
                c.setConfiguration(debugConfig);
                Object r = Pnuts.eval(watch, c);
                result.setText(Pnuts.format(r));
            } catch (Exception e2){
                result.setText(e2.toString());
            }
        } else {
            result.setText("");
        }
    }
}
