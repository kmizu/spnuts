/*
 * @(#)LimitedClassesConfiguration.java 1.2 04/12/06
 * 
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.ext;

import pnuts.lang.*;
import java.util.*;
import java.lang.reflect.*;

public class LimitedClassesConfiguration extends ConfigurationAdapter {

	private HashSet set = new HashSet();

	public LimitedClassesConfiguration() {
	}

	public LimitedClassesConfiguration(Configuration base) {
		super(base);
	}

	public void registerClass(Class cls) {
		set.add(cls);
	}

	public Method[] getMethods(Class cls) {
		if (set.contains(cls)) {
			return super.getMethods(cls);
		} else {
			return new Method[] {};
		}
	}

	public Constructor[] getConstructors(Class cls) {
		if (set.contains(cls)) {
			return super.getConstructors(cls);
		} else {
			return new Constructor[] {};
		}
	}
}