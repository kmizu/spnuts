/*
 * SimpleObjectDescFactory.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.tinybeans;

import org.pnuts.lang.ObjectDesc;
import org.pnuts.lang.ObjectDescFactory;

public class SimpleObjectDescFactory extends ObjectDescFactory {

    public ObjectDesc create(Class cls){
	return new SimpleObjectDesc(cls);
    }
}
