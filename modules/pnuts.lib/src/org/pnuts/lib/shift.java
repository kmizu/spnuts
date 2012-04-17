/*
 * @(#)shift.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.util.*;

/*
 * function shift(linkedList)
 */
public class shift extends PnutsFunction {

	public shift(){
		super("shift");
	}

	public boolean defined(int nargs){
		return nargs == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		return ((LinkedList)args[0]).removeFirst();
	}

	public String toString(){
		return "function shift(linkedList)";
	}
}
