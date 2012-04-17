/*
 * @(#)Label.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

public class Label {

	ClassFile cf;
	int target_pc = -1;
	IntArray pc = new IntArray();
	int size;

	Label(ClassFile cf) {
		this.cf = cf;
	}

	Label(ClassFile cf, int pc) {
		this.cf = cf;
		this.target_pc = pc;
	}

	/**
	 * Fixes the position to which the label points as the current position in
	 * the code buffer
	 */
	public void fix() {
		target_pc = cf.codeBuffer.size();
		setRelativePosition(target_pc);
	}

	public Label shift(int offset) {
		return new Label(this.cf, this.target_pc + offset);
	}

	void setOffset(int offset) {
		setRelativePosition(target_pc + offset);
	}

	/**
	 * Fixes the position to which the label points
	 * 
	 * @param tgt
	 *            the relative position to which the label points
	 */
	public void setRelativePosition(int tgt) {
		int array[] = pc.getArray();
		int sz = pc.size();
		for (int i = 0; i < sz; i++) {
			if (size == 2) {
				cf.codeBuffer.set((short) (tgt - array[i]), array[i] + 1);
			} else if (size == 4) {
				cf.codeBuffer.set((int) (tgt - array[i]), array[i] + 1);
			}
		}
	}

	/**
	 * Fixes the position to which the label points
	 * 
	 * @param tgt
	 *            the absolute position to which the label points
	 */
	public void setPosition(int tgt) {
		int array[] = pc.getArray();
		int sz = pc.size();
		for (int i = 0; i < sz; i++) {
			if (size == 2) {
				cf.codeBuffer.set((short) tgt, array[i]);
			} else if (size == 4) {
				cf.codeBuffer.set((int) tgt, array[i]);
			}
		}
	}

	short getPC() {
		return (short) target_pc;
	}

	void setPC(short pos) {
		target_pc = pos;
	}

	/**
	 * Registers the label and allocate 2 or 4 bytes in the code buffer.
	 */
	public void register(int pos, int size) {
		pc.add(pos);
		this.size = size;
		if (target_pc > 0) {
			setRelativePosition(target_pc);
		} else {
			if (size == 2) {
				cf.codeBuffer.add((short) 0);
			} else if (size == 4) {
				cf.codeBuffer.add((int) 0);
			}
		}
	}
}
