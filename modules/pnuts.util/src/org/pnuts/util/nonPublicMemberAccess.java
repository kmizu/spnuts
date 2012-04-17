/*
 * @(#)nonPublicMemberAccess.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import pnuts.lang.*;
import pnuts.ext.*;

/*
 * function nonPublicMemberAccess({boolean})
 */
public class nonPublicMemberAccess extends PnutsFunction {

	final static String LAST_CONFIGURATION = "pnuts.util.lastConfiguration".intern();

	public nonPublicMemberAccess(){
		super("nonPublicMemberAccess");
	}

	public boolean defined(int nargs){
		return (nargs == 0 || nargs == 1);
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Configuration current = context.getConfiguration();
		if (nargs == 0){
			return (current instanceof NonPublicMemberAccessor) ? Boolean.TRUE : Boolean.FALSE;
		} else if (nargs == 1){
			boolean enabled = ((Boolean)args[0]).booleanValue();
			if (enabled){
				if (!(current instanceof NonPublicMemberAccessor)){
					context.set(LAST_CONFIGURATION, current);
					context.setConfiguration(new NonPublicMemberAccessor());
				}
			} else {
				if (current instanceof NonPublicMemberAccessor){
					context.setConfiguration((Configuration)context.get(LAST_CONFIGURATION));
				}
			}
			return null;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function nonPublicMemberAccess({boolean})";
	}
}
