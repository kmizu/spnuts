/*
 * @(#)ControlEnv.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import pnuts.lang.PnutsParserTreeConstants;
import java.util.Stack;

class ControlEnv {
	Label breakLabel;
	Label continueLabel;
	Stack finallyBlocks;
	ControlEnv parent;

	ControlEnv(int id, ControlEnv parent) {
		this.parent = parent;
		if (parent != null) {
			finallyBlocks = parent.finallyBlocks;
			if (id == PnutsParserTreeConstants.JJTSWITCHSTATEMENT) {
				continueLabel = parent.continueLabel;
			}
		} else {
			finallyBlocks = new Stack();
		}
	}

	void pushFinallyBlock(Label label) {
		finallyBlocks.push(label);
	}

	Label popFinallyBlock() {
		if (finallyBlocks.size() > 0) {
			return (Label) finallyBlocks.pop();
		} else {
			return null;
		}
	}
}
