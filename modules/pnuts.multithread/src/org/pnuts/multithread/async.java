/*
 * @(#)async.java 1.2 04/12/06
 *
 * Copyright (c) 2001-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import pnuts.lang.PnutsFunction;
import pnuts.lang.Context;
import pnuts.lang.PnutsException;

/*
 * thread_pool = threadPool()
 * handle = async(function ()..., thread_pool)
 * ...
 * handle()
 */
public class async extends PnutsFunction {

	public async(){
		super("async");
	}

	public boolean defined(int nargs){
		return nargs == 2;
	}

	protected Object exec(Object[] args, final Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		final PnutsFunction func = (PnutsFunction)args[0];
		final ThreadPool pool = (ThreadPool)args[1];

		class Env {
			Object result;
			Throwable throwable;
			int state = 0; // 1==success, 2==exception thrown
			Task task;

			Env(){
				this.task = new Task();
				pool.addTask(task);
			}

			protected synchronized Object getResult(Context context) throws InterruptedException {
				while (state == 0){
					wait();
				}
				if (state == 1){
					return result;
				}
				throw new PnutsException(throwable, context);
			}

			class Task implements Runnable {
				public void run(){
					try {
						result = func.call(new Object[]{}, new Context(context));
						state = 1;
					} catch (Throwable t){
						throwable = t;
						state = 2;
					}
					synchronized (Env.this){
						Env.this.notifyAll();
					}
				}
			}
		}
			
		class Handle extends PnutsFunction {
			Env env = new Env();

			public boolean defined(int nargs){
				return nargs == 0;
			}

			protected Object exec(Object[] args, Context context){
				if (args.length != 0){
					undefined(args, context);
					return null;
				}
				try {
					return env.getResult(context);
				} catch (InterruptedException e){
					throw new PnutsException(e, context);
				}
			}

			public String toString(){
				return "<asynchronous task for \"" + func.getName() + "\">";
			}
		}
		return new Handle();
	}

	public String toString(){
		return "function async(function ()..., threadpool)";
	}
}
