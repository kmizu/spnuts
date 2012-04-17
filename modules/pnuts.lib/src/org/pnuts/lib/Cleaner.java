/*
 * @(#)Cleaner.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import java.lang.ref.Reference;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provides an alternative to Object.finalize() method.
 */
public class Cleaner extends PhantomReference {

	private static final ReferenceQueue rq = new ReferenceQueue();
	private static Set set = new HashSet();

	static {
		startHandler();
	}

	private final Runnable command;

	private Cleaner(Object referent, Runnable command) {
		super(referent, rq);
		this.command = command;
	}

	/**
	 * Registers a command as the finalizer of the target object.
	 * When the target is ready to be reclaimed, the registered command is executed.
	 *
	 * @param target the target object
	 * @param command the command to be registered
	 * @return A Cleaner object that should be assigned to a member variable of the target object.
	 */
	public static Cleaner create(Object target, Runnable command){
		if (command != null){
			Cleaner cleaner = new Cleaner(target, command);
			set.add(cleaner);
			return cleaner;
		}
		return null;
	}

	protected void error(Throwable t){
		// ignore
	}

	void clean(){
		if (!set.remove(this)){
			return;
		}
		try {
			command.run();
		} catch (Throwable t){
			error(t);
		}
	}

	private static synchronized boolean remove(Cleaner cleaner){
		if (set.isEmpty()){
			return false;
		} else {
			set.remove(cleaner);
			return true;
		}
	}

	private static void startHandler(){
		Handler handler = new Handler();
		handler.setDaemon(true);
		handler.setPriority(Thread.MAX_PRIORITY - 1);
		handler.start();
	}

	private static class Handler extends Thread {
		public void run(){
			for (;;) {
				try {
					Reference r = rq.remove();
					if (r instanceof Cleaner) {
						((Cleaner)r).clean();
						continue;
					} 
				} catch (InterruptedException e){
				}
			}
		}
	}
}
