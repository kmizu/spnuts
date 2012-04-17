/*
 * @(#)contains.java 1.3 05/01/14
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.util.*;
import java.lang.reflect.Array;

public class contains extends PnutsFunction {

	public contains(){
		super("contains");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 2){
			undefined(args, context);
		}
		Object target = args[0];
		final Object key = args[1];
		if (target instanceof Collection){
			Collection col = (Collection)target;
			return col.contains(args[1]) ? Boolean.TRUE : Boolean.FALSE;
		} else if (target instanceof String){
		    String str = (String)target;
		    if (key instanceof String){
			if (str.indexOf((String)key) >= 0){
			    return Boolean.TRUE;
			} else {
			    return Boolean.FALSE;
			}
		    } else if (key instanceof Character){
			if (str.indexOf(((Character)key).charValue()) >= 0){
			    return Boolean.TRUE;
			} else {
			    return Boolean.FALSE;
			}
		    }
		} else if (target instanceof CharSequence){
			if (key instanceof CharSequence){
				if (check((CharSequence)target, (CharSequence)key)){
					return Boolean.TRUE;
				} else {
					return Boolean.FALSE;
				}
			} else if (key instanceof Character){
				if (check((CharSequence)target, ((Character)key).charValue())){
					return Boolean.TRUE;
				} else {
					return Boolean.FALSE;
				}
			}
		} else if (target instanceof Object[]){
			Object[] array = (Object[])target;
			if (key != null){
				for (int i = 0; i < array.length; i++){
					if (key.equals(array[i])){
						return Boolean.TRUE;
					}
				}
				return Boolean.FALSE;
			} else {
				for (int i = 0; i < array.length; i++){
					if (array[i] == null){
						return Boolean.TRUE;
					}
				}
				return Boolean.FALSE;
			}
		} else if (Runtime.isArray(target)){
			int len = Array.getLength(target);
			if (key != null){
				for (int i = 0; i < len; i++){
					if (key.equals(Array.get(target, i))){
						return Boolean.TRUE;
					}
				}
				return Boolean.FALSE;
			} else {
				for (int i = 0; i < len; i++){
					if (Array.get(target, i) == null){
						return Boolean.TRUE;
					}
				}
				return Boolean.FALSE;
			}
		}
		throw new IllegalArgumentException(String.valueOf(target));

	}

	static boolean check(CharSequence target, CharSequence key){
		int i = 0;
		int target_len = target.length();
		int key_len = key.length();
		int max = target_len - key_len;
		if (key_len < 1){
		    return true;
		}
		char first  = key.charAt(0);

	startSearchForFirstChar:
		while (true) {
			while (i <= max && target.charAt(i) != first) {
				i++;
			}
			if (i > max) {
				return false;
			}
			int j = i + 1;
			int end = j + key_len - 1;
			int k = 1;
			while (j < end) {
				if (target.charAt(j++) != key.charAt(k++)) {
					i++;
					continue startSearchForFirstChar;
				}
			}
			return true;
		}
	}

	static boolean check(CharSequence target, char key){
		int len = target.length();
		for (int i = 0; i < len; i++){
			if (target.charAt(i) == key){
				return true;
			}
		}
		return false;
	}

	public String toString(){
		return "function contains(collection|array|string, elem)";
	}
}
