/*
 * @(#)StackFrameInspector.java 1.3 05/04/22
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import pnuts.lang.Context;
import pnuts.lang.Function;

/**
 * This class allows to enumerate local variables in pure interpreter mode.
 * 
 * <pre>
 * 
 *   function foo(a){
 *      println(StackFrameInspector::localSymbols(getContext()))
 *   }
 *   foo(100)
 *     =&gt; {a=100}
 *  
 * </pre>
 */
public class StackFrameInspector {

	static Field stackFrameField; // Context.stackFrame
	static Field symbolTableField; // StackFrame.symbolTable
	static Field parentField; // SymbolTable.parent
	static Field nameField; // Binding.name
	static Field valueField; // Binding.value
	static Field parentFrame; // StackFrame.parent
	static Method bindingsMethod; // SymbolTable.bindings()
	static Field frameField; // Context.frame
	static Field lexicalScopeField; // Function.lexicalScope
	static {
		try {
			Class StackFrameClass = Class.forName("pnuts.lang.StackFrame");
			Class SymbolTableClass = Class.forName("pnuts.lang.SymbolTable");
			Class BindingClass = Class.forName("pnuts.lang.Binding");
			stackFrameField = Context.class.getDeclaredField("stackFrame");
			stackFrameField.setAccessible(true);
			frameField = Context.class.getDeclaredField("frame");
			frameField.setAccessible(true);
			lexicalScopeField = Function.class.getDeclaredField("lexicalScope");
			lexicalScopeField.setAccessible(true);
			symbolTableField = StackFrameClass.getDeclaredField("symbolTable");
			symbolTableField.setAccessible(true);
			parentField = SymbolTableClass.getDeclaredField("parent");
			parentField.setAccessible(true);
			nameField = BindingClass.getDeclaredField("name");
			nameField.setAccessible(true);
			valueField = BindingClass.getDeclaredField("value");
			valueField.setAccessible(true);
			bindingsMethod = SymbolTableClass.getMethod("bindings",
					new Class[] {});
			bindingsMethod.setAccessible(true);
			parentFrame = StackFrameClass.getDeclaredField("parent");
			parentFrame.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Map localSymbols(Context context) {
		try {
			HashMap map = new HashMap();
			localSymbols(context, map);
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void localSymbols(Context context, Map map)
			throws IllegalAccessException, InvocationTargetException {
		Object stackFrame = stackFrameField.get(context);
		if (stackFrame != null) {
			scanSymbolTable(symbolTableField.get(stackFrame), map, false);
		}
		Object frame = frameField.get(context);
		if (frame != null) {
			Object lexicalScope = lexicalScopeField.get(frame);
			if (lexicalScope != null) {
				scanSymbolTable(lexicalScope, map, true);
			}
		}
	}

	static void scanSymbolTable(Object st, Map map, boolean lexicalScope)
			throws IllegalAccessException, InvocationTargetException {
		while (st != null) {
			Enumeration bindings = (Enumeration) bindingsMethod.invoke(st,
					new Object[] {});
			while (bindings.hasMoreElements()) {
				Object b = bindings.nextElement();
				String name = (String) nameField.get(b);
				int idx = name.indexOf('!');
				if (idx > 0){
					name = name.substring(0, idx);
				} else if (idx == 0){
					continue;
				}
				Object value = valueField.get(b);
				if (lexicalScope) {
					value = valueField.get(value);
				}
				if (!map.keySet().contains(name)) {
					map.put(name, value);
				}
			}
			st = parentField.get(st);
		}
	}
}
