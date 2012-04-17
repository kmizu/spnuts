/*
 * @(#)schedule.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import java.io.*;
import java.util.*;

public class schedule extends PnutsFunction {

	public schedule(){
		super("schedule");
	}

	public boolean defined(int nargs){
		return nargs == 2 || nargs == 3;
	}

	static Timer doSchedule(Date timeToRun,
							final PnutsFunction task,
							final Context context)
		{
			Timer timer = new Timer(true);
			timer.schedule(new TimerTask(){
					public void run(){
						task.call(new Object[]{}, context);
					}
				}, timeToRun);
			return timer;
		}

	static Timer doSchedule(Date timeToRun,
							long period,
							final PnutsFunction task,
							final Context context)
		{
			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimerTask(){
					public void run(){
						task.call(new Object[]{}, context);
					}
				}, timeToRun, period);
			return timer;
		}

	public Object exec(Object[] args, Context context){
		int nargs = args.length;
		switch (nargs){
		case 2: {
			PnutsFunction task;
			Object arg0 = args[0];
			if (arg0 instanceof PnutsFunction){
				task = (PnutsFunction)arg0;
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			Object arg1 = args[1];
			Date timeToRun;
			if (arg1 instanceof Date){
				timeToRun = (Date)arg1;
			} else if (arg1 instanceof Number){
				timeToRun = new Date(System.currentTimeMillis() +
									 ((Number)arg1).longValue());
			} else {
				throw new IllegalArgumentException(String.valueOf(arg1));
			}
			return doSchedule(timeToRun, task, context);
		}
		case 3:{
			Object arg0 = args[0];
			PnutsFunction task;
			if (arg0 instanceof PnutsFunction){
				task = (PnutsFunction)arg0;
			} else {
				throw new IllegalArgumentException(String.valueOf(arg0));
			}
			Object arg1 = args[1];
			Date timeToRun;
			if (arg1 instanceof Date){
				timeToRun = (Date)arg1;
			} else if (arg1 instanceof Number){
				timeToRun = new Date(System.currentTimeMillis() +
									 ((Number)arg1).longValue());
			} else {
				throw new IllegalArgumentException(String.valueOf(arg1));
			}
			Object arg2 = args[2];
			long period;
			if (arg2 instanceof Number){
				period = ((Number)arg2).longValue();
			} else {
				throw new IllegalArgumentException(String.valueOf(arg2));
			}
			return doSchedule(timeToRun, period, task, context);
		}
		default:
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function schedule(task(), timeToRun {, period})";
	}
}
