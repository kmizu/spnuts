/*
 * ObjectDescFactory.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

/*
 * ObjectDescFactory.getDefault().create(className)
 */
public abstract class ObjectDescFactory implements ObjectDescFactoryConstants {

    static ObjectDescFactory defaultFactory = getDefaultObjectDescFactory();

    static ObjectDescFactory instantiateObjectDescFactory(String className){
	try {
	    Class cls = Class.forName(className);
	    return (ObjectDescFactory)cls.newInstance();
	} catch (Exception e){
	    e.printStackTrace();
	    return null;
	}
    }

    static ObjectDescFactory getDefaultObjectDescFactory(){
	String prop = System.getProperty(PROPERTY_OBJECT_DESC_FACTORY);
	if (prop != null){
	    return instantiateObjectDescFactory(prop);
	}
	return instantiateObjectDescFactory(DEFAULT_OBJECT_DESC_FACTORY_NAME);
    }

    public static ObjectDescFactory getDefault(){
	return defaultFactory;
    }

    public abstract ObjectDesc create(Class cls);

    public ObjectDesc create(Class cls, Class stopClass){
	return create(cls);
    }
}
