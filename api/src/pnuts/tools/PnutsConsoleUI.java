/*
 * @(#)PnutsConsoleUI.java 1.1 05/05/16
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.util.*;
import java.net.URL;
import java.awt.Image;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import pnuts.lang.Context;

/**
 * Swing based UI
 */
public class PnutsConsoleUI extends JTextComponentConsoleUI {

       final static String ICON_RESOURCE = "pnuts16.png";

	Vector history = new Vector();
	int historyIndex = -1;
	JFrame frame;
        JComponent component;
        String title;

	public PnutsConsoleUI(){
	    this(null);
	}

	public PnutsConsoleUI(Console console){
		setModel(console);
		EventHandler handler = new EventHandler();
		JTextArea textarea = (JTextArea)textComponent;
		textarea.addKeyListener(handler);
		textarea.getDocument().addDocumentListener(handler);
	}
        
        public PnutsConsole createConsole(Context context, String inputlog){
            return createConsole(null, context, null, inputlog, true, null, Thread.NORM_PRIORITY);
        }
        
        protected PnutsConsole createConsole(){
            return new PnutsConsole();
        }
        
        public PnutsConsole createConsole(String[] modules,
                Context context,
                ClassLoader cl,
                String inputlog,
                boolean greeting,
                Runnable terminationCallback,
                int priority)
        {
            Context cc;
            if (context != null){
                cc = context;
            } else {
                cc = new CancelableContext();
            }
            if (cl != null){
                cc.setClassLoader(cl);
            }
            if (modules != null){
                for (int i = 0; i < modules.length; i++) {
                    cc.usePackage(modules[i]);
                }
            }
            PnutsConsole console = createConsole();
            setModel(console);
            console.setContext(cc);
            console.setInputLog(inputlog);
            console.setGreeting(greeting);
            console.setClassLoader(cl);
            console.setPriority(priority);
            console.setTerminationCallback(terminationCallback);
            console.setConsoleUI(this);
            return console;            
        }

	public void entered(String command){
		if (command != null && command.length() > 0){
			history.add(command);
		}
		historyIndex = history.size();
	}

	public JTextArea getTextArea(){
		return (JTextArea)textComponent;
	}
        
        public JComponent getComponent(){
            if (this.component == null){
                this.component = new JScrollPane(textComponent);
            }
            return this.component;
        }
        
        public void setTitle(String title){
            this.title = title;
        }
        
        public String getTitle(){
            return this.title;
        }

	public JFrame getFrame(){
            if (frame == null){
		frame = new JFrame(title);
		frame.setContentPane(getComponent());
		frame.setSize(textComponent.getPreferredSize());
		URL imageURL = PnutsConsole.class.getResource(ICON_RESOURCE);
		if (imageURL != null) {
		    Image iconImage = Toolkit.getDefaultToolkit().getImage(imageURL);
		    if (iconImage != null) {
			frame.setIconImage(iconImage);
		    }
		}
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
			    ((PnutsConsole)model).dispose();
			}
		    });
	    };
	    return frame;
	}

	void largerFont(){
		Font font = textComponent.getFont();
		int size = font.getSize() + 1;
		textComponent.setFont(font.deriveFont((float)size));
	}

	void smallerFont(){
		Font font = textComponent.getFont();
		int size = font.getSize() - 1;
		if (size < 1){
			size = 1;
		}
		textComponent.setFont(font.deriveFont((float)size));
	}

       public void close(){
	   if (frame != null){
	       frame.dispose();
	   }
       }

	class EventHandler implements KeyListener, DocumentListener {
	
		public void keyPressed(KeyEvent e) {
			int code = e.getKeyCode();
			char ch = e.getKeyChar();
			boolean control = e.isControlDown();

			int pos = textComponent.getCaretPosition();
			if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_LEFT) {
				if (mark == pos) {
					e.consume();
				}
			} else if (control && code == KeyEvent.VK_A) {
				textComponent.setCaretPosition(mark);
				e.consume();
			} else if (control && code == KeyEvent.VK_E) {
				textComponent.setCaretPosition(textComponent.getDocument().getLength());
				e.consume();
			} else if (control && code == KeyEvent.VK_B) {
				if (mark < pos){
					textComponent.setCaretPosition(pos - 1);
				}
				e.consume();
			} else if (control && code == KeyEvent.VK_D) {
				Document doc = textComponent.getDocument();
				if (pos < doc.getLength()){
					try {
						doc.remove(pos, 1);
					} catch (BadLocationException ble){
						ble.printStackTrace();
					}
				}
				e.consume();
			} else if (control && code == KeyEvent.VK_F) {
				if (textComponent.getDocument().getLength() > pos){
					textComponent.setCaretPosition(pos + 1);
				}
				e.consume();
			} else if (control && code == KeyEvent.VK_K) {
				Document doc = textComponent.getDocument();
				try {
					doc.remove(pos, doc.getLength() - pos);
				} catch (BadLocationException ble) {
					ble.printStackTrace();
				}
				e.consume();
			} else if (control && code == KeyEvent.VK_U) {
				Document doc = textComponent.getDocument();
				try {
					doc.remove(mark, doc.getLength() - mark);
				} catch (BadLocationException ble) {
					ble.printStackTrace();
				}
				e.consume();
			} else if (control && ch == '+'){
				largerFont();
				e.consume();
			} else if (control && (ch == '-' || code == KeyEvent.VK_MINUS)){
				smallerFont();
				e.consume();
			} else if (code == KeyEvent.VK_HOME || (control && code == KeyEvent.VK_A)) {
				if (pos == mark) {
					e.consume();
				} else if (pos > mark) {
					if (!control) {
						if (e.isShiftDown()) {
							textComponent.moveCaretPosition(mark);
						} else {
							textComponent.setCaretPosition(mark);
						}
						e.consume();
					}
				}
			} else if (code == KeyEvent.VK_ENTER) {
				enter();
				e.consume();
			} else if (code == KeyEvent.VK_UP || (control && code == KeyEvent.VK_P)) {
				historyIndex--;
				if (historyIndex >= 0) {
					if (historyIndex >= history.size()) {
						historyIndex = history.size() -1;
					}
					if (historyIndex >= 0) {
						String str = (String)history.elementAt(historyIndex);
						int len = textComponent.getDocument().getLength();
						((JTextArea)textComponent).replaceRange(str, mark, len);
						int caretPos = mark + str.length();
						textComponent.select(caretPos, caretPos);
					} else {
						historyIndex++;
					}
				} else {
					historyIndex++;
				}
				e.consume();
			} else if (code == KeyEvent.VK_DOWN || (control && code == KeyEvent.VK_N)) {
				int caretPos = mark;
				if (history.size() > 0) {
					historyIndex++;
					if (historyIndex < 0) {
						historyIndex = 0;
					}
					int len = textComponent.getDocument().getLength();
					if (historyIndex < history.size()) {
						String str = (String)history.elementAt(historyIndex);
						((JTextArea)textComponent).replaceRange(str, mark, len);
						caretPos = mark + str.length();
					} else {
						historyIndex = history.size();
						((JTextArea)textComponent).replaceRange("", mark, len);
					}
				}
				textComponent.select(caretPos, caretPos);
				e.consume();

			}
		}
 
		public void keyTyped(KeyEvent e) {
			int keyChar = e.getKeyChar();
			if (keyChar == KeyEvent.VK_BACK_SPACE) {
				if (mark == textComponent.getCaretPosition()) {
					e.consume();
				}
			} else if (textComponent.getCaretPosition() < mark) {
				textComponent.setCaretPosition(mark);
			}
		}
	
		public void keyReleased(KeyEvent e) {
		}

		public void insertUpdate(DocumentEvent e) {
			synchronized (PnutsConsoleUI.this){
				int len = e.getLength();
				int off = e.getOffset();
				if (mark > off) {
					mark += len;
				}
			}
		}
	
		public void removeUpdate(DocumentEvent e) {
			synchronized (PnutsConsoleUI.this){
				int len = e.getLength();
				int off = e.getOffset();
				if (mark > off) {
					if (mark >= off + len) {
						mark -= len;
					} else {
						mark = off;
					}
				}
			}
		}

		public void changedUpdate(DocumentEvent e) {
		}
	}
}
