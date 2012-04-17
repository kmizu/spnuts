/*
 * @(#)getSession.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.servlet;

import pnuts.servlet.*;
import pnuts.lang.*;
import pnuts.servlet.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

/*
 * function getSession()
 * function getSession(boolean)
 */
public class getSession extends PnutsFunction {
	public getSession(){
		super("getSession");
	}

	public boolean defined(int nargs){
		return nargs < 2;
	}

	static HttpSession getSession(Context context, boolean create){
		HttpServletRequest request =
			(HttpServletRequest)context.get(PnutsServlet.SERVLET_REQUEST);
		if (request == null){
			throw new IllegalStateException();
		}
		return request.getSession(create);
	}

	protected Object exec(Object args[], Context context){
		int nargs = args.length;
		boolean create;
		if (nargs == 1){
			create = ((Boolean)args[0]).booleanValue();
		} else if (nargs == 0){
			create = true;
		} else {
			undefined(args, context);
			return null;
		}
		return getSession(context, create);
	}

	public String toString(){
		return "function getSession( { create } )";
	}
}
