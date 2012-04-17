/*
 * @(#)bind.java 1.4 05/01/27
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.beans;

import pnuts.lang.*;
import java.util.*;
import java.io.*;
import java.beans.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/*
 * function bind(bean, action, func)
 */
public class bind extends PnutsFunction {

	/*
	 * bean => adapter
	 */
	static Hashtable eventAdapterTable = new Hashtable();

	/*
	 * listenerType + action => adapter class
	 */
	static Hashtable adapters = new Hashtable();

	public bind(){
		super("bind");
	}

	public boolean defined(int nargs){
		return nargs == 3;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs != 3){
			undefined(args, context);
			return null;
		}
		Object bean = args[0];
		final PnutsFunction func = (PnutsFunction)args[2];
		if (!func.defined(1)){
			throw new PnutsException("function must take one argument", context);
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
		Method method = null;
		Method addMethod = null;
		try {
			EventSetDescriptor[] eventset =
				Introspector.getBeanInfo(bean.getClass()).getEventSetDescriptors();
			for (int i = 0; i < eventset.length; i++){
				EventSetDescriptor event = eventset[i];
				if (listenerType != null && listenerType != event.getListenerType().getName()){
					continue;
				}
				Method[] methods = event.getListenerMethods();
				Method add = event.getAddListenerMethod();
				for (int j = 0; j < methods.length; j++){
					Method m = methods[j];
					if (m.getName().equals(action)){
						method = m;
						addMethod = add;
						break;
					}
				}
			}
			if (addMethod == null){
			    for (int i = 0; i < eventset.length; i++){
				EventSetDescriptor event = eventset[i];
				Class t = event.getListenerType();
				Method[] methods = t.getMethods();
				for (int j = 0; j < methods.length; j++){
				    Method mj = methods[j];
				    if (mj.getName().equals(action)){
					method = mj;
					addMethod = event.getAddListenerMethod();
					break;
				    }
				}
				if (addMethod != null){
				    break;
				}
			    }
			    if (addMethod == null){
				throw new PnutsException("cannot find add-method for: " + action, context);
			    }
			}
			Class listenerClass = method.getDeclaringClass();
		
			String key = listenerClass + ":" + action;
			Class adapter = (Class)adapters.get(key);
			if (adapter == null){
				adapters.put(key, adapter = EventAdapter.generateEventAdapter(listenerClass, action));
			}
			Constructor cons = adapter.getConstructor(new Class[]{PnutsFunction.class, Context.class});
			Object listener = cons.newInstance(new Object[]{
				new PnutsFunction(){
					protected Object exec(Object[] args, Context context){
						func.call(new Object[]{args[0]}, context);
						PrintWriter w = context.getWriter();
						if (w != null){
							w.flush();
						}
						return null;
					}
				},
				(Context)context.clone()
			});
			addMethod.invoke(bean, new Object[]{listener});
			register(context, bean, action, func, listener);
		} catch (IntrospectionException e1){
			throw new PnutsException(e1, context);
		} catch (IllegalAccessException e2){
			throw new PnutsException(e2, context);
		} catch (InvocationTargetException e3){
			throw new PnutsException(e3, context);
		} catch (NoSuchMethodException e4){
		} catch (InstantiationException e5){
			throw new PnutsException(e5, context);
		}
		return null;
	}

	static void register(Context context,
						 Object bean,
						 String action,
						 PnutsFunction func,
						 Object adapter)
		{
			Hashtable ht = (Hashtable)eventAdapterTable.get(bean);
			if (ht == null){
				eventAdapterTable.put(bean, ht = new Hashtable());
				Hashtable u = new Hashtable();
				ht.put(action, u);
				u.put(func, adapter);
			} else {
				Hashtable u = (Hashtable)ht.get(action);
				if (u == null){
					ht.put(action, u = new Hashtable());
				}
				u.put(func, adapter);
			}
		}

	public String toString(){
		return "function bind(bean, action, func)";
	}
}
