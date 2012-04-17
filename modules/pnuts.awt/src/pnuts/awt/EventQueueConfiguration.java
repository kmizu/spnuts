/*
 * @(#)EventQueueConfiguration.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.awt.EventQueue;
import pnuts.lang.Runtime;
import pnuts.lang.Context;
import pnuts.lang.Configuration;
import pnuts.lang.PnutsException;
import pnuts.ext.ConfigurationAdapter;

/**
 * This class allows you to execute all Java API access from scrips on the event dispatch thread.
 *
 * <pre>e.g.
 * context.setConfiguration(new EventQueueConfiguration(context.getConfiguration()));
 * </pre>
 */
public class EventQueueConfiguration extends ConfigurationAdapter {

	/**
	 * Constructor
	 */
	public EventQueueConfiguration(){
	}

	/**
	 * Constructor
	 *
	 * @param base the base configuration
	 */
	public EventQueueConfiguration(Configuration base){
		super(base);
	}

	/**
	 * Calls a method.
	 *
	 * @param method the method to call
	 * @param target the object
	 * @param args   the arguments
	 * 
	 * @return the methods return value
	 */
	public Object callMethod(final Context context,
							 final Class c,
							 final String name,
							 final Object args[],
							 final Class types[],
							 final Object target)
		{
			Object result = null;

			if (EventQueue.isDispatchThread()){
				result = super.callMethod(context, c, name, args, types, target);
			} else {
				try {
					result = (new Invoker(){
							public Object runSelf() throws Throwable {
								return EventQueueConfiguration.super.callMethod(context, c, name, args, types, target);
							}
						}).invoke();
				} catch (IllegalAccessException e0) {
					throw new PnutsException(e0, context);
				} catch (InvocationTargetException e1) {
					throw new PnutsException(e1.getTargetException(), context);
				}
			}
			return result;
		}

	/*
	 *
	 */
	public Object callConstructor(final Context context,
								  final Class clazz,
								  final Object args[],
								  final Class types[])
		{
			Object result = null;

			if (EventQueue.isDispatchThread()) {
				result = super.callConstructor(context, clazz, args, types);
			} else {
				try {
					result = (new Invoker(){
							public Object runSelf() throws Throwable {
								return EventQueueConfiguration.super.callConstructor(context, clazz, args, types);
							}
						}).invoke();
				} catch (IllegalAccessException e0) {
					throw new PnutsException(e0, context);
				} catch (InvocationTargetException e1) {
					throw new PnutsException(e1.getTargetException(), context);
				}
			}
			return result;
		}

	public Object getField(final Context context,
						   final Object target,
						   final String name)
		{
			Object result = null;

			if (EventQueue.isDispatchThread()){
				result = super.getField(context, target, name);
			} else {
				try {
					result = (new Invoker(){
							public Object runSelf() throws Throwable {
								return EventQueueConfiguration.super.getField(context, target, name);
							}
						}).invoke();
				} catch (IllegalAccessException e0) {
					throw new PnutsException(e0, context);
				} catch (InvocationTargetException e1) {
					throw new PnutsException(e1.getTargetException(), context);
				}
			}
			return result;
		}

	public void putField(final Context context,
						 final Object target,
						 final String name,
						 final Object expr)
		{
			if (EventQueue.isDispatchThread()) {
				super.putField(context, target, name, expr);
			} else {
				try {
					(new Invoker (){
							public Object runSelf() throws Throwable {
								EventQueueConfiguration.super.putField(context, target, name, expr);
								return null;
							}
						}).invoke();
				} catch (IllegalAccessException e0) {
					throw new PnutsException(e0, context);
				} catch (InvocationTargetException e1) {
					throw new PnutsException(e1.getTargetException(), context);
				}
			}
		}

	public Object getStaticField(final Context context,
								 final Class clazz,
								 final String name){
		Object result = null;

		if (EventQueue.isDispatchThread()){
			result = super.getStaticField(context, clazz, name);
		} else {
			try {
				result = (new Invoker(){
						public Object runSelf() throws Throwable {
							return EventQueueConfiguration.super.getStaticField(context, clazz, name);
						}
					}).invoke();
			} catch (IllegalAccessException e0) {
				throw new PnutsException(e0, context);
			} catch (InvocationTargetException e1) {
				throw new PnutsException(e1, context);
			}
		}
		return result;
	}

	public void putStaticField(final Context context,
							   final Class clazz,
							   final String name,
							   final Object value)
		{
			if (EventQueue.isDispatchThread()) {
				super.putStaticField(context, clazz, name, value);
			} else {
				try {
					(new Invoker (){
							public Object runSelf() throws Throwable {
								EventQueueConfiguration.super.putStaticField(context, clazz, name, value);
								return null;
							}
						}).invoke();
				} catch (IllegalAccessException e0) {
					throw new PnutsException(e0, context);
				} catch (InvocationTargetException e1) {
					throw new PnutsException(e1.getTargetException(), context);
				}
			}
		}

	private abstract class Invoker implements Runnable {
		private Object result = null;
		private Throwable throwable = null;
		
		public Object invoke()
			throws InvocationTargetException, IllegalAccessException
			{
				try {
					EventQueue.invokeAndWait(this);
				} catch (InterruptedException e) {
					throwable = e;
				}

				if (throwable != null) {
					if (throwable instanceof IllegalAccessException){
						throw (IllegalAccessException)throwable;
					} else if (throwable instanceof InvocationTargetException){
						throw (InvocationTargetException)throwable;
					} else {
						throw new InvocationTargetException(throwable);
					}
				} else {
					return result;
				}
			}

		public void run() {
			try {
				result = runSelf();
			} catch (Throwable t) {
				throwable = t;
			}
		}

		protected abstract Object runSelf() throws Throwable;
	}
}
