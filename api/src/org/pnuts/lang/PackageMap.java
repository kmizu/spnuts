/*
 * PackageMap.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import pnuts.lang.Package;
import pnuts.lang.Context;
import pnuts.lang.NamedValue;
import java.util.*;

public class PackageMap extends AbstractMap {

    private Package pkg;

    public PackageMap(Package pkg){
	this.pkg = pkg;
    }

    public Object get(Object key){
	String symbol = ((String)key).intern();
	NamedValue value = pkg.lookup(symbol);
	if (value != null){
	    return value.get();
	}
	return null;
    }


    public Object put(Object key, Object value){
	String symbol = ((String)key).intern();
	NamedValue binding = pkg.lookup(symbol);
	if (binding != null){
	    Object old = binding.get();
	    binding.set(value);
	    return old;
	} else {
	    pkg.set(symbol, value);
	    return null;
	}
    }

    public int size(){
	return pkg.size();
    }

    public Set entrySet(){
	Enumeration e = pkg.bindings();
	Set set = new HashSet();
	while (e.hasMoreElements()){
	    NamedValue binding = (NamedValue)e.nextElement();
	    set.add(new NamedValueEntry(binding));
	}
	return set;
    }

    static class NamedValueEntry implements Map.Entry {
	private NamedValue binding;

	NamedValueEntry(NamedValue binding){
	    this.binding = binding;
	}

	public Object getKey(){
	    return binding.getName();
	}

	public Object getValue(){
	    return binding.get();
	}

	public Object setValue(Object newValue){
	    Object old = binding.get();
	    binding.set(newValue);
	    return old;
	}

	public int hashCode(){
	    return binding.hashCode();
	}

	public boolean equals(Object obj){
	    if (obj instanceof NamedValueEntry){
		NamedValueEntry e = (NamedValueEntry)obj;
		return binding.equals(e.binding);
	    }
	    return false;
	}
    }
}
