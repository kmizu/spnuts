/*
 * @(#)Import.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Hashtable;

class Import implements Serializable {

	static final long serialVersionUID = 5497516072354034892L;

	/**
	 * @serial
	 */
	private String name;

	transient private Hashtable table;

	public Import(String name) {
		this.name = name;
		this.table = new Hashtable(64);
	}

	public String getName() {
		return name;
	}

	public Class get(String className, Context context) {
		Class c = (Class) table.get(className);
		if (c != null) {
			return c;
		}
		String fullName = className;
		try {
			Class clazz = null;
			if (name.length() > 0) {
				fullName = name + "." + className;
			}

			clazz = Pnuts.loadClass(fullName, context);
			if (clazz != null) {
				table.put(className, clazz);
			}
			return clazz;
		} catch (ClassNotFoundException e) {
			//	    if (Pnuts.debug()){
			//		System.out.println(fullName + " class not found");
			//	    }
		}
		return null;
	}

	void reset() {
		table = new Hashtable(64);
	}

	private void readObject(ObjectInputStream s)
			throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		table = new Hashtable(64);
	}

	public String toString() {
		return "import " + name;
	}
}
