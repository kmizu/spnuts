/*
 * makeQueryString.java
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.net;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.pnuts.net.URLEncoding;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;

/*
 * makeQueryString(map, encoding)
 */
public class makeQueryString extends PnutsFunction {

	public makeQueryString(){
		super("makeQueryString");
	}

	public boolean defined(int narg){
		return narg == 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		Map map;
		String encoding;
		if (nargs == 2){
			map = (Map)args[0];
			encoding = (String)args[1];
		} else {
			undefined(args, context);
			return null;
		}
		try {
			return URLEncoding.makeQueryString(map, encoding);
		} catch (UnsupportedEncodingException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "makeQueryString(map, encoding)";
	}
}
