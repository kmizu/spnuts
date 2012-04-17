/*
 * LayoutException.java
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

/**
 * Throws to indicate that there is an error in layout components
 *
 * @version	1.1
 * @author	Toyokazu Tomatsu
 */
public class LayoutException extends RuntimeException {

	public LayoutException(){
		this("");
	}

	public LayoutException(String message){
		super(message);
	}
}
