/*
 * @(#)PnutsObjectInputStream.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.io;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Hashtable;
import java.lang.reflect.Array;
import pnuts.lang.Pnuts;
import pnuts.lang.Context;

/**
 * This class deserializes primitive date and objects.  Classes are
 * resolved using the classloader of the context passed to the constructor.
 */
public class PnutsObjectInputStream extends ObjectInputStream {

	private Context context;
	private static Hashtable primitives = new Hashtable();
	static {
		primitives.put("int", int.class);
		primitives.put("short", short.class);
		primitives.put("byte", byte.class);
		primitives.put("char", char.class);
		primitives.put("long", long.class);
		primitives.put("float", float.class);
		primitives.put("double", double.class);
		primitives.put("boolean", boolean.class);
	}

	public PnutsObjectInputStream(InputStream in, Context context)
		throws IOException, StreamCorruptedException
		{
			super(in);
			this.context = context;
		}

	public void setContext(Context context){
		this.context = context;
	}

	protected Class resolveClass(ObjectStreamClass objectStreamClass)
		throws IOException, ClassNotFoundException
		{
			String name = objectStreamClass.getName();

			if (!name.startsWith("[")){
				Class type = (Class)primitives.get(name);
				if (type != null){
					return type;
				} else {
					return Pnuts.loadClass(name, context);
				}
			}

			int i;
			for (i = 1; name.charAt(i) == '['; i++) { /* just skip */ }

			Class clazz;
			if (name.charAt(i) == 'L'){
				clazz = Pnuts.loadClass(name.substring(i + 1, name.length() - 1), context);
			} else {
				if (name.length() != i + 1){
					throw new ClassNotFoundException(name);
				}
				clazz = primitiveType(name.charAt(i));
			}
			int dim[] = new int[i];
			for (int j = 0; j < i; j++){
				dim[j] = 0;
			}
			return Array.newInstance(clazz, dim).getClass();
		}

	private Class primitiveType(char ch){
		switch (ch){
		case 'B':
			return Byte.TYPE;
		case 'C':
			return Character.TYPE;
		case 'D':
			return Double.TYPE;
		case 'F':
			return Float.TYPE;
		case 'I':
			return Integer.TYPE;
		case 'J':
			return Long.TYPE;
		case 'S':
			return Short.TYPE;
		case 'Z':
			return Boolean.TYPE;
		default:
			return null;
		}
	}
}
