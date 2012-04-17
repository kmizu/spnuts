/*
 * @(#)mapget.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.util.*;

public class mapget extends PnutsFunction {

	public mapget(){
		super("mapget");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
		}
		Map map = (Map)args[0];
		return map.get(args[1]);
	}

	public String toString(){
		return "function mapget(map, key)";
	}
}
