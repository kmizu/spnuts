/*
 * @(#)JTextComponentConsoleUI.java 1.1 05/05/16
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.util.*;
import java.io.*;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

public class JTextComponentConsoleUI implements ConsoleUI {
    	private final static int defaultColumns = 80;
	private final static int defaultRows = 24;
        
	JTextComponent textComponent;
	Console model;
	int mark;

       protected JTextComponentConsoleUI(){
                setJTextComponent(createTextComponent());
       }

        public void setModel(Console console){
            this.model = console;
        }
        
        public Console getModel(){
            return this.model;
        }

	public JTextComponent getJTextComponent(){
		return textComponent;
	}
        
        public void setJTextComponent(JTextComponent c){
            this.textComponent = c;
        }
        
        protected JTextComponent createTextComponent(){
		JTextArea textarea = new JTextArea();
		textarea.setLineWrap(true);
		textarea.setFont(new Font("Monospaced", 0, 12));
		textarea.setRows(defaultRows);
		textarea.setColumns(defaultColumns);
		return textarea;
        }

	static void runCommand(Runnable command){
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(command);
		} else {
			command.run();
		}
	}

	public void append(final String str){
		runCommand(new Runnable(){
				public void run(){
					insert(str, mark);
					mark += str.length();
					int pos = getLength();
					setCursorPosition(pos);
				}
			});
	}

	public void insert(final String str, final int mark){
		Document doc = textComponent.getDocument();
		if (doc != null) {
			try {
				doc.insertString(mark, str, null);
			} catch (BadLocationException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}
	}

	public int getLength(){
		return textComponent.getDocument().getLength();
	}

	public void setCursorPosition(int pos){
		textComponent.select(pos, pos);
	}

	public int getMarkPosition(){
		return mark;
	}

	public void setMarkPosition(int pos){
		mark = pos;
	}

	synchronized void enter(){
		try {
			int len = getLength();
			sendText(mark, len - mark);
		} catch (IOException e){
                    System.err.println(e);
		}
	}

	public void entered(String command){
		// skip
	}

	void sendText(int start, int len) throws IOException {
		Document doc = textComponent.getDocument();
		int doclen = doc.getLength();
		Segment segment = new Segment();
		try {
			doc.getText(mark, doclen - mark, segment);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
		}
		entered(segment.toString());
		char[] cbuf = segment.array;
		int offset = segment.offset;
		int count = segment.count;

		model.enter(cbuf, offset, count);

		insert("\n", getLength());
		mark = doc.getLength();
	}
        
        public void close(){
        }
}
