/*
 * @(#)ThreadPool.java 1.2 04/12/06
 *
 * Copyright (c) 2002,2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.multithread;

import java.util.Vector;
import java.util.Stack;
import java.util.Enumeration;

/**
 * A simple, general purpose thread pool implementation
 */
public class ThreadPool {
	private Queue tasks = new Queue();
	private Vector workers = new Vector();
	private Stack idleWorkers = new Stack();
	private int maxThreads;
	private int minThreads;
	private long timeout;
	private int taskRequests = 0;
	private int priority = Thread.NORM_PRIORITY;
	private ThreadGroup threadGroup;
	private volatile boolean terminated = false;

	public ThreadPool(int maxThreads){
		this(maxThreads, 1, -1);
	}

	public ThreadPool(int maxThreads, int minThreads, long timeout){
		this.maxThreads = maxThreads;
		this.minThreads = minThreads;
		this.timeout = timeout;
		this.threadGroup = new WorkerThreadGroup();
	}

	public void addTask(Runnable task){
		tasks.enqueue(task);
		if (idleWorkers.isEmpty()){
			int n_workers = workers.size();
			if (taskRequests == 0 && n_workers < maxThreads){
				Thread th = new WorkerThread();
				synchronized (this){
					workers.addElement(th);
				}
				th.setPriority(priority);
				th.setDaemon(true);
				th.start();
			}
		} else {
			WorkerThread th = (WorkerThread)idleWorkers.pop();
			synchronized (th){
				th.notify();
			}
		}
	}

	public synchronized void shutdown(){
		terminated = true;
		threadGroup.interrupt();
	}

	synchronized void removeWorker(WorkerThread th){
		workers.remove(th);
	}

	Runnable getTask(){
		try {
			synchronized (this){
				taskRequests++;
			}
			return (Runnable)tasks.dequeue(timeout);
		} catch (InterruptedException e){
			return null;
		} finally {
			synchronized (this){
				taskRequests--;
			}
		}
	}

	public synchronized void setPriority(int prio){
		this.priority = prio;
		for (Enumeration e = workers.elements(); e.hasMoreElements();){
			Thread th = (Thread)e.nextElement();
			th.setPriority(prio);
		}
	}

	static class WorkerThreadGroup extends ThreadGroup {

		public WorkerThreadGroup() {
			super("Worker");
		}
	
		public void uncaughtException(Thread t, Throwable e) {
			if (!(e instanceof ThreadDeath)) {
				System.err.println("Uncaught Exception: " + e + " by " + t);
			}
		}
	}

	static int worker_id = 0;
	class WorkerThread extends Thread {
		public WorkerThread(){
			super(threadGroup, "Worker-" + (worker_id++));
		}
		public void run(){
			while (true){
				if (tasks.isEmpty() && terminated){
					return;
				}
				Runnable task = getTask();
				if (task == null){
					if (terminated){
						return;
					}
					if (!idleWorkers.isEmpty() && workers.size() > minThreads){
						removeWorker(this);
						return;
					} else {
						idleWorkers.push(this);
						synchronized (this){
							try {
								wait();
							} catch (InterruptedException e){
								idleWorkers.remove(this);
								return;
							}
						}
					}
				} else {
					task.run();
				}
			}
		}
	}
}
