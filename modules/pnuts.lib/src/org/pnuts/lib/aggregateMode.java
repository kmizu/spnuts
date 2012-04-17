/*
 * @(#)aggregateMode.java 1.2 04/12/06
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.ext.*;

public class aggregateMode extends PnutsFunction {

	private static final String AGGREGATE_MODE = "pnuts.lib.aggregateMode".intern();

	public aggregateMode(){
		super("aggregateMode");
	}

	public boolean defined(int nargs){
		return nargs == 0 || nargs == 1;
	}

	static boolean getAggregateMode(Context context){
		Object mode = context.get(AGGREGATE_MODE);
		return (mode instanceof Configuration) && !(mode instanceof AggregateConfiguration);
	}

	static void setAggregateMode(Context context, boolean mode){
		Configuration conf = context.getConfiguration();
		if (mode){
			context.set(AGGREGATE_MODE, conf);
			context.setConfiguration(new AggregateConfiguration(conf));
		} else {
			if (conf instanceof Configuration){
				context.set(AGGREGATE_MODE, null);
				context.setConfiguration(conf);
			}
		}
	}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		if (nargs == 0){
			return Boolean.valueOf(getAggregateMode(context));
		} else if (nargs == 1){
			boolean mode = ((Boolean)args[0]).booleanValue();
			setAggregateMode(context, mode);
			return null;
		} else {
			undefined(args, context);
			return null;
		}
	}

	public String toString(){
		return "function aggregateMode({ mode })";
	}
}
