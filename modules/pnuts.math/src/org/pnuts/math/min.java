/*
 * min.java
 *
 * Copyright (c) 2001-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.math;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import java.util.Collection;
import java.util.Iterator;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class min extends PnutsFunction {

	public min(){
		super("min");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}

	protected Object exec(Object[] args, final Context context){
		int nargs = args.length;
		Object arg0 = args[0];
		int type0 = 0;
		if (nargs == 1 || nargs == 2){
			if (arg0 instanceof Collection){
				type0 = 1;
			} else if (arg0 instanceof int[]){
				type0 = 2;
			} else if (arg0 instanceof long[]){
				type0 = 3;
			} else if (arg0 instanceof short[]){
				type0 = 4;
			} else if (arg0 instanceof byte[]){
				type0 = 5;
			} else if (arg0 instanceof double[]){
				type0 = 6;
			} else if (arg0 instanceof float[]){
				type0 = 7;
			} else if (arg0 instanceof Object[]){
				type0 = 8;
			}			
		}
		if (nargs == 1){
			Object arg = args[0];
			if (type0 == 0){
				throw new IllegalArgumentException(String.valueOf(arg0));
			} else if (type0 == 1){
				Iterator it = ((Collection)arg).iterator();
				Object min = null;
				if (it.hasNext()){
					min = it.next();
				}
				while (it.hasNext()){
					Object next = it.next();
					if (Runtime.gt(min, next)){
						min = next;
					}
				}
				return min;
			} else {
				Object min = null;
				int len = Array.getLength(arg);
				if (len > 0){
					min = Array.get(arg, 0);
				}
				for (int i = 1; i < len; i++){
					Object next = Array.get(arg, i);
					if (Runtime.gt(min, next)){
						min = next;
					}
				}
				return min;
			}
		} else if (nargs == 2){
			Object arg1 = args[1];
			if (type0 == 0){
				if (Runtime.gt(arg0, arg1)){
					return arg1;
				} else {
					return arg0;
				}				
			}
			int type1 = 0;
			if (arg1 instanceof PnutsFunction){
				type1 = 1;
			} else if (arg1 instanceof Comparator){
				type1 = 2;
			} else if (arg1 == null){
				type1 = 3;
			} else {
				throw new IllegalArgumentException(String.valueOf(arg1));
			}
			Comparator comp = null;
			if (type1 == 3){  // default comparator
				switch (type0){
				case 1: // Collection	
					return Collections.min((Collection)arg0);
				case 2: { // int[]
					int[] array = (int[])arg0;
					if (array.length < 1){
						return null;
					}
					int m = array[0];
					for (int i = 1; i < array.length; i++){
						int j = array[i];
						if (j < m){
							m = j;
						}
					}
					return new Integer(m);
				}
				case 3: { // long[]
					long[] array = (long[])arg0;
					if (array.length < 1){
						return null;
					}
					long m = array[0];
					for (int i = 1; i < array.length; i++){
						long j = array[i];
						if (j < m){
							m = j;
						}
					}
					return new Long(m);						
				}
				case 4: { // short[]
					short[] array = (short[])arg0;
					if (array.length < 1){
						return null;
					}
					short m = array[0];
					for (int i = 1; i < array.length; i++){
						short j = array[i];
						if (j < m){
							m = j;
						}
					}
					return new Short(m);							
				}
				case 5: { // byte[]
					byte[] array = (byte[])arg0;
					if (array.length < 1){
						return null;
					}
					byte m = array[0];
					for (int i = 1; i < array.length; i++){
						byte j = array[i];
						if (j < m){
							m = j;
						}
					}
					return new Byte(m);	
				}
				case 6: { // double[]
					double[] array = (double[])arg0;
					if (array.length < 1){
						return null;
					}
					double m = array[0];
					for (int i = 1; i < array.length; i++){
						double j = array[i];
						if (j < m){
							m = j;
						}
					}
					return new Double(m);						
				}
				case 7: { // float[]
					float[] array = (float[])arg0;
					if (array.length < 1){
						return null;
					}
					float m = array[0];
					for (int i = 1; i < array.length; i++){
						float j = array[i];
						if (j < m){
							m = j;
						}
					}
					return new Float(m);
				}
				case 8: { // Object[]
					Object[] array = (Object[])arg0;
					int len = array.length;
					if (len < 1){
						return null;
					}
					Object m = array[0];
					Comparable c;
					if (m instanceof Comparable){
						c = (Comparable)m;
					} else {
						return null;
					}
					for (int i = 1; i < len; i++){
						Object j = array[i];
						if (c.compareTo(j) > 0){
							if (j instanceof Comparable){
								c = (Comparable)j;
							}
						}
					}
					return c;						
				}
				default:
					throw new IllegalArgumentException(String.valueOf(arg0));	
				}
			} else if (type1 == 1){
				final PnutsFunction func = (PnutsFunction)arg1;
				comp = new Comparator(){
					public int compare(Object obj1, Object obj2){
						Object o1 = func.call(new Object[]{obj1}, context);
						Object o2 = func.call(new Object[]{obj2}, context);
						return Runtime.compareTo(o1, o2);
					}
				};				
			} else if (type1 == 2){
				comp = (Comparator)arg1;
			}
			switch (type0){
			case 1: // Collection	
				return Collections.min((Collection)arg0, comp);
			case 2: { // int[]
				int[] array = (int[])arg0;
				if (array.length < 1){
					return null;
				}
				Integer m = new Integer(array[0]);
				for (int i = 1; i < array.length; i++){
					Integer j = new Integer(array[i]);
					if (comp.compare(j, m) < 0){
						m = j;
					}
				}
				return m;
			}
			case 3: { // long[]
			long[] array = (long[])arg0;
				if (array.length < 1){
					return null;
				}
				Long m = new Long(array[0]);
				for (int i = 1; i < array.length; i++){
					Long j = new Long(array[i]);
					if (comp.compare(j, m) < 0){
						m = j;
					}
				}
				return m;						
			}
			case 4: { // short[]
				short[] array = (short[])arg0;
				if (array.length < 1){
					return null;
				}
				Short m = new Short(array[0]);
				for (int i = 1; i < array.length; i++){
					Short j = new Short(array[i]);
					if (comp.compare(j, m) < 0){
						m = j;
					}
				}
				return m;							
			}
			case 5: { // byte[]
				byte[] array = (byte[])arg0;
				if (array.length < 1){
					return null;
				}
				Byte m = new Byte(array[0]);
				for (int i = 1; i < array.length; i++){
					Byte j = new Byte(array[i]);
					if (comp.compare(j, m) < 0){
						m = j;
					}
				}
				return m;	
			}
			case 6: { // double[]
				double[] array = (double[])arg0;
				if (array.length < 1){
					return null;
				}
				Double m = new Double(array[0]);
				for (int i = 1; i < array.length; i++){
					Double j = new Double(array[i]);
					if (comp.compare(j, m) < 0){
						m = j;
					}
				}
				return m;						
			}
			case 7: { // float[]
				float[] array = (float[])arg0;
				if (array.length < 1){
					return null;
				}
				Float m = new Float(array[0]);
				for (int i = 1; i < array.length; i++){
					Float j = new Float(array[i]);
					if (comp.compare(j, m) < 0){
						m = j;
					}
				}
				return m;
			}
			case 8: { // Object[]
				Object[] array = (Object[])arg0;
				int len = array.length;
				if (len < 1){
					return null;
				}
				Object m = array[0];
				for (int i = 1; i < len; i++){
					Object j = array[i];
					if (comp.compare(j, m) < 0){
						m = j;
					}
				}
				return m;						
			}
			default:
				throw new IllegalArgumentException(String.valueOf(arg0));	
			}
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "fuction min(arg1, arg2) or (collection/array {, comparator | f(arg) })";
	}
}
