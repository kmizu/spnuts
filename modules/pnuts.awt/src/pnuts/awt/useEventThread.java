/*
 * @(#)useEventThread.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import pnuts.lang.*;

public class useEventThread extends PnutsFunction {

	private final static String BACKUP_CONF = "pnuts.awt.conf.backup".intern();
	private final static String EVENT_CONF = "pnuts.awt.conf.event".intern();

	public useEventThread(){
		super("useEventThread");
	}

	public boolean defined(int nargs){
		return nargs < 2;
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			if (context.getConfiguration() instanceof EventQueueConfiguration){
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		} else if (nargs == 1){
			if (((Boolean)args[0]).booleanValue()){
				Configuration c1 = (Configuration)context.get(EVENT_CONF);
				if (c1 == null){
					Configuration c2 = context.getConfiguration();
					c1 = new EventQueueConfiguration(c2);
					context.set(EVENT_CONF, c1);
					context.set(BACKUP_CONF, c2);
				}
				context.setConfiguration(c1);
			} else {
				Configuration c = (Configuration)context.get(BACKUP_CONF);
				if (c != null){
					context.set(EVENT_CONF, null);
					context.set(BACKUP_CONF, null);
					context.setConfiguration(c);
				}
			}
			return null;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function useEventThread({boolean})";
	}
}
