/*
 * @(#)toUpperCase.java 1.2 05/01/19
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;

public class toUpperCase extends PnutsFunction {

	public toUpperCase(){
		super("toUpperCase");
	}

	public boolean defined(int narg){
		return narg == 1;
	}

	protected Object exec(Object[] args, Context context){
		if (args.length != 1){
			undefined(args, context);
			return null;
		}
		Object arg = args[0];
		if (arg instanceof String){
			return ((String)arg).toUpperCase();
		} else if (arg instanceof Character){
			return new Character(Character.toUpperCase(((Character)arg).charValue()));
		} else if (arg instanceof CharSequence){
			return new UpperCaseCharSequence((CharSequence)arg);
		} else if (arg == null){
			return null;
		} else {
			throw new IllegalArgumentException(String.valueOf(arg));
		}
	}

	public String toString(){
		return "function toUpperCase(char|String|CharSequence)";
	}

	static class UpperCaseCharSequence implements CharSequence {
		private CharSequence base;

		UpperCaseCharSequence(CharSequence base){
			this.base = base;
		}

		public int length(){
			return base.length();
		}

		public char charAt(int index){
			return Character.toUpperCase(base.charAt(index));
		}

		public CharSequence subSequence(int start, int end){
			return new UpperCaseCharSequence(base.subSequence(start, end));
		}

		public String toString(){
			return base.toString().toUpperCase();
		}
	}
}
