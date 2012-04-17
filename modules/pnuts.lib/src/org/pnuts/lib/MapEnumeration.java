/*
 * @(#)MapEnumeration.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.util.*;

class MapEnumeration implements Enumeration {
	private Enumeration en;

	protected MapEnumeration(){
	}

	public MapEnumeration(Enumeration en){
		this.en = en;
	}

	protected Object map(Object obj){
		return obj;
	}

	public boolean hasMoreElements(){
		return en.hasMoreElements();
	}

	public Object nextElement(){
		return map(en.nextElement());
	}
}
