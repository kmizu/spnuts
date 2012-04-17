/*
 * @(#)encodeBean.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import pnuts.lang.*;
import java.beans.*;
import java.io.*;

/*
 * encodeBean(bean {, outputStream } )
 */
public class encodeBean extends PnutsFunction {

	public encodeBean(){
		super("encodeBean");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 1){
			Object bean = args[0];
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			XMLEncoder enc = new XMLEncoder(bout);
			enc.writeObject(bean);
			enc.close();
			return new ByteArrayInputStream(bout.toByteArray());
		} else if (nargs == 2){
			Object bean = args[0];
			OutputStream output = (OutputStream)args[1];
			XMLEncoder enc = new XMLEncoder(output);
			enc.writeObject(bean);
			enc.flush();
		} else {
			undefined(args, context);
		}
		return null;
	}

	public String toString(){
		return "function encodeBean(bean {, outputStream })";
	}
}
