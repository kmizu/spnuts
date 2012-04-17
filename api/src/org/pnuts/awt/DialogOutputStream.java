/*
 * @(#)DialogOutputStream.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.awt;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;

/**
 * Output stream to a Dialog. The flush() method brings up the dialog.
 */
public class DialogOutputStream extends ByteArrayOutputStream implements
		ActionListener, KeyListener {

	Frame parent;

	Dialog dialog;

	TextArea textArea;

	Button button;

	int len = 0;

	private static FlowLayout flowLayout = new FlowLayout();

	static int screen_width, screen_height;
	static {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		screen_width = dim.width;
		screen_height = dim.height;
	}

	public DialogOutputStream(Frame parent) {
		this(parent, 32);
	}

	public DialogOutputStream(Frame parent, int size) {
		this(parent, size, false);
	}

	public DialogOutputStream(Frame parent, int size, boolean modal) {
		super(size);
		this.parent = parent;
		dialog = new Dialog(parent, "Error", modal);
		textArea = new TextArea("", 0, 0, TextArea.SCROLLBARS_BOTH);
		textArea.setEditable(false);
		textArea.setSize(screen_width * 3 / 4, screen_height / 2);
		textArea.setBackground(Color.white);
		textArea.addKeyListener(this);
		dialog.add("Center", textArea);

		button = new Button("OK");
		button.addActionListener(this);

		Panel panel = new Panel();
		panel.setLayout(flowLayout);
		panel.add(button);
		dialog.add("South", panel);
		dialog.setSize(screen_width * 3 / 4, screen_height / 2);
	}

	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_ENTER) {
			dialog.setVisible(false);
			reset();
			textArea.setText("");
		}
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void actionPerformed(ActionEvent e) {
		dialog.setVisible(false);
		reset();
		textArea.setText("");
		dialog.getParent().requestFocus();
	}

	public Dimension getSize() {
		return dialog.getSize();
	}

	public void setSize(int w, int h) {
		dialog.setSize(w, h);
	}

	public void toFront() {
		dialog.toFront();
	}

	public synchronized void write(int b) {
		this.len++;
	}

	public synchronized void write(byte b[], int off, int len) {
		super.write(b, off, len);
		this.len += len;
	}

	public synchronized void flush() {
		if (len < 1) {
			return;
		}
		textArea.setText(new String(toByteArray()));
		Point loc = parent.getLocation();
		Dimension sz1 = parent.getSize();
		Dimension sz2 = dialog.getSize();
		int x = loc.x + (sz1.width - sz2.width) / 2;
		int y = loc.y + (sz1.height - sz2.height) / 2;
		if (x < 0 || y < 0) {
			Dimension d = dialog.getToolkit().getScreenSize();
			x = (d.width - sz2.width) / 2;
			y = (d.height - sz2.height) / 2;
		}
		dialog.setLocation(x, y);

		dialog.setVisible(true);
		dialog.toFront();
		len = 0;
	}
}