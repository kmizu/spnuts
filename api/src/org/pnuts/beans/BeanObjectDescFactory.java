/*
 * BeanObjectDescFactory.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import java.beans.Introspector;
import java.beans.IntrospectionException;
import org.pnuts.lang.ObjectDesc;
import org.pnuts.lang.ObjectDescFactory;
import pnuts.lang.Runtime;

public class BeanObjectDescFactory extends ObjectDescFactory {

    private int flag;

    public BeanObjectDescFactory(){
	if (Runtime.getBoolean("pnuts.lang.respect_bean_info")){
	    this.flag = Introspector.USE_ALL_BEANINFO;
	} else {
	    this.flag = Introspector.IGNORE_ALL_BEANINFO;
	}
    }

    public ObjectDesc create(Class cls){
	try {
	    return new BeanObjectDesc(cls, null, flag);
	} catch (IntrospectionException e){
	    return null;
	}
    }

    public ObjectDesc create(Class cls, Class stopClass){
	try {
	    return new BeanObjectDesc(cls, stopClass, flag);
	} catch (IntrospectionException e){
	    return null;
	}
    }
}
