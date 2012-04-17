/*
 * @(#)unbind.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import pnuts.lang.*;
import java.beans.*;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/*
 * function unbind(bean, action {, func })
 */
public class unbind extends PnutsFunction {

	public unbind(){
		super("unbind");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 2 && nargs != 3){
			undefined(args, context);
			return null;
		}
		Object bean = args[0];
		PnutsFunction func;
		if (nargs == 2){
			func = null;
		} else {
			func = (PnutsFunction)args[2];
			if (!func.defined(1)){
				throw new PnutsException("");
			}
		}
		String action = (String)args[1];
		int idx = action.lastIndexOf('.');
		String listenerType;
		if (idx > 0){
			listenerType = action.substring(0, idx - 1);
			action = action.substring(idx + 1);
		} else {
			listenerType = null;
		}

		try {
			EventSetDescriptor[] eventset = Introspector.getBeanInfo(bean.getClass()).getEventSetDescriptors();
			for (int i = 0; i < eventset.length; i++){
				EventSetDescriptor event = eventset[i];
				if (listenerType != null && listenerType != event.getListenerType().getName()){
					continue;
				}
				Method[] methods = event.getListenerMethods();
				Method removeMethod = event.getRemoveListenerMethod();
				for (int j = 0; j < methods.length; j++){
					Method m = methods[j];
					if (m.getName().equals(action)){
						unregister(context, bean, action, func, removeMethod);
						break;
					}
				}
			}
		} catch (IntrospectionException e1){
			throw new PnutsException(e1, context);
		} catch (IllegalAccessException e2){
			throw new PnutsException(e2, context);
		} catch (InvocationTargetException e3){
			throw new PnutsException(e3, context);
		}
		return null;
	}

	static void unregister(Context context,
						   Object bean,
						   String action,
						   PnutsFunction func,
						   Method removeMethod)
		throws IllegalAccessException, InvocationTargetException
		{
			Hashtable t = (Hashtable)bind.eventAdapterTable.get(bean);
			if (t != null){
				Hashtable u = (Hashtable)t.get(action);
				if (u != null){
					if (func != null){
						Object adapter = u.get(func);
						u.remove(func);
						if (adapter != null){
							removeMethod.invoke(bean, new Object[]{adapter});
						}
					} else {
						for (Enumeration e = u.elements(); e.hasMoreElements();){
							removeMethod.invoke(bean, new Object[]{e.nextElement()});
						}
						u.clear();
					}
				}
			}
		}

	public String toString(){
		return "function unbind(bean, action { , func } )";
	}
}
