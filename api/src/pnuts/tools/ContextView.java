/*
 * @(#)ContextView.java 1.2 04/12/06
 *
 * Copyright (c) 2003,2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import pnuts.ext.NonPublicMemberAccessor;
import pnuts.lang.Configuration;
import pnuts.lang.Context;

class ContextView {
	private final static Font monospaced = Font.getFont("monospaced");
	private final static Configuration debugConfig = new NonPublicMemberAccessor();

	private JTextField currentPackage;
	private JTextField classLoader;
	private JList imports;
	private JList modules;
	private JTable locals;
	private JTable contextLocals;
	private JTextField inspect;
	private JTextArea result;
	private Context context;
	private GridBagLayout gb; // We have to use GBL because this class is loaded by bootclassloader.
        private VisualDebuggerView debuggerView;

	public ContextView(VisualDebuggerView debuggerView){
            this.debuggerView = debuggerView;
	}

	public JFrame getFrame(){
		JFrame jfr = new JFrame();
		setupGUI(jfr.getContentPane());
		jfr.setSize(jfr.getPreferredSize());
		return jfr;
	}

	public Container getContainer(){
		JPanel panel = new JPanel();
		setupGUI(panel);
		panel.setSize(panel.getPreferredSize());
		return panel;
	}

	static void add(GridBagLayout gb,
					Container container,
					Component component,
					int gridx,
					int gridy,
					int gridwidth,
					int gridheight,
					int weightx,
					int weighty,
					int anchor,
					int fill)
		{
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = gridx;
			c.gridy = gridy;
			c.gridwidth = gridwidth;
			c.gridheight = gridheight;
			c.weightx = weightx;
			c.weighty = weighty;
			c.anchor = anchor;
			c.fill = fill;
			c.insets = new Insets(2, 2, 2, 2);
			gb.setConstraints(component, c);
			container.add(component);
		}

	void addLabel(Container container, Component component, int gridx, int gridy){
		add(gb, container, component,
			gridx, gridy,
			1, 1, 0, 0,
			GridBagConstraints.NORTHEAST, GridBagConstraints.NONE);
	}

	void add_h(Container container, Component component, int gridx, int gridy){
		add(gb, container, component,
			gridx, gridy,
			1, 1, 1, 0,
			GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
	}

	void add_b(Container container, Component component, int gridx, int gridy){
		add(gb, container, component,
			gridx, gridy,
			1, 1, 1, 1,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH);
	}

	void add_n(Container container, Component component, int gridx, int gridy){
		add(gb, container, component,
			gridx, gridy,
			1, 1, 0, 0,
			GridBagConstraints.CENTER, GridBagConstraints.NONE);
	}

	JLabel createJLabel(String key){
		return new JLabel(debuggerView.getResourceString(key + VisualDebuggerView.labelSuffix));
	}
        
	public void setupGUI(Container contentPane){
		this.gb = new GridBagLayout();
		Font monospaced = Font.getFont("monospaced");
		contentPane.setLayout(gb);
                
            String beansDef = debuggerView.getResourceString("inspector.beans");
            StringTokenizer st = new StringTokenizer(beansDef, ",");
            int idx = 0;
            while (st.hasMoreTokens()){
                String token = st.nextToken();
                String clsName = debuggerView.getResourceString("inspector.bean." + token + ".class");
                if (clsName != null){
                    try {
                        Class cls = Class.forName(clsName);
                        Component bean = (Component)cls.newInstance();
			  if (bean instanceof ContextListener){
			     addContextlistener((ContextListener)bean);
			  }
                        addLabel(contentPane, createJLabel(token), 0, idx);
			  String layoutHint = debuggerView.getResourceString("inspector.bean." + token + ".layout");
			  if (layoutHint != null && layoutHint.toLowerCase().equals("both")){
			      add_b(contentPane, bean, 1, idx);
			  } else if (layoutHint != null && layoutHint.toLowerCase().equals("none")){
			      add_n(contentPane, bean, 1, idx);
			  } else { // "horizontal"
			      add_h(contentPane, bean, 1, idx);
			  }
                        idx++;
                    } catch (ClassNotFoundException cnf){
                        // ignore
                    } catch (IllegalAccessException iae){
                        // ignore
                    } catch (InstantiationException ie){
                        // ignore
                    }
                }
            }
	}
        HashSet listeners = new HashSet();
        public void addContextlistener(ContextListener listener){
            listeners.add(listener);
        }
        
	public void setContext(Context context){
		this.context = context;
                Set listeners = (Set)this.listeners.clone();
                for (Iterator it = listeners.iterator(); it.hasNext(); ){
                    ContextListener listener = (ContextListener)it.next();
                    listener.update(new ContextEvent(context));
                }
	}
}
