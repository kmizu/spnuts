/*
 * @(#)Template.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.text;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import pnuts.lang.*;
import pnuts.lang.Runtime;

public class Template {
	private String text;
	private int[] startIndexes;
	private int[] endIndexes;
	private String[] keys;

	public Template(String text,
					int[] startIndexes,
					int[] endIndexes,
					String[] keys)
		{
			this.text = text;
			this.startIndexes = startIndexes;
			this.endIndexes = endIndexes;
			this.keys = keys;
		}

	static void apply(Object val, Writer writer, Context context) throws IOException {
		if (val instanceof String){
			writer.write((String)val);
		} else if (val instanceof PnutsFunction){
			((PnutsFunction)val).call(new Object[]{writer}, context);
		} else if (val instanceof Collection){
			Collection col = (Collection)val;
			for (Iterator it = col.iterator(); it.hasNext();){
				apply(it.next(), writer, context);
			}
		} else if (val instanceof Iterator){
			for (Iterator it = (Iterator)val; it.hasNext();){
				apply(it.next(), writer, context);
			}
		} else if (val instanceof Enumeration){
			for (Enumeration en = (Enumeration)val; en.hasMoreElements();){
				apply(en.nextElement(), writer, context);
			}
		} else if (Runtime.isArray(val)){
			int len = Runtime.getArrayLength(val);
			for (int i = 0; i < len; i++){
				apply(Array.get(val, i), writer, context);
			}
		} else if (val != null){
			writer.write(val.toString());
		}
	}

	public void format(Map def, Writer writer, Context context) throws IOException {
		int offset = 0;
		for (int i = 0; i < keys.length; i++){
			writer.write(text, offset, startIndexes[i] - offset);
			apply(def.get(keys[i]), writer, context);
			offset = endIndexes[i];
		}
		writer.write(text, offset, text.length() - offset);
	}

	public String toString(){
		return text;
	}
}
