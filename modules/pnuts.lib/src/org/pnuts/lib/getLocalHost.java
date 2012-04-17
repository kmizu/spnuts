/*
 * @(#)getLocalHost.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class getLocalHost extends PnutsFunction {

	public getLocalHost(){
		super("getLocalHost");
	}

	public boolean defined(int nargs){
		return nargs == 0;
	}
	
	protected Object exec(Object[] args, Context context){
		if (args.length == 0){
			try {
				return InetAddress.getLocalHost();
			} catch (UnknownHostException e){
				throw new PnutsException(e, context);
			}
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function getLocalHost()";
	}
}
