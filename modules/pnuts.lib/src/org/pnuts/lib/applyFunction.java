/*
 * @(#)applyFunction.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.util.*;

public class applyFunction extends call {
	public applyFunction(){
		super("applyFunction");
	}
	public String toString(){
		return "function applyFunctoin(func, arguments)";
	}
}
