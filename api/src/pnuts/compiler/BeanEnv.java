/*
 * BeanEnv.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

class BeanEnv {
    BeanEnv parent;

    BeanEnv(BeanEnv parent){
	this.parent = parent;
    }
}
