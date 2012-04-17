/*
 * @(#)LRUcache.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import org.pnuts.util.LRUCacheMap;
import org.pnuts.util.LRUCache;
import java.util.*;

/**
 * function LRUCache(size {, func(key)})
 */
public class LRUcache extends PnutsFunction {

	public LRUcache(){
		super("LRUcache");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2 || nargs == 3;
	}

	public Object exec(Object[] args, final Context context){
		int nargs = args.length;
		if (nargs == 1){
			int max = ((Integer)args[0]).intValue();
			return new FixedSizedCache(max, null, null, context);
		} else if (nargs == 2){
			int max = ((Integer)args[0]).intValue();
			return new FixedSizedCache(max, (PnutsFunction)args[1], null, context);
		} else if (nargs == 3){
			int max = ((Integer)args[0]).intValue();
			return new FixedSizedCache(max,
						   (PnutsFunction)args[1],
						   (PnutsFunction)args[2],
						   context);
		} else {
			undefined(args, context);
			return null;
		}
	}

	static class FixedSizedCache extends LRUCacheMap {
		PnutsFunction cf, df;
		Context context;

		FixedSizedCache(int max, PnutsFunction cf, PnutsFunction df, Context c){
			super(max);
			this.cf = cf;
			this.df = df;
			this.context = c;
		}

		protected Object construct(Object key){
			return cf.call(new Object[]{key}, context);
		}

		public void expired(Object old){
			if (df != null){
				df.call(new Object[]{old}, context);
			}
		}
	}

	public String toString(){
		return "function LRUcache(size { , function(key) {, function (old)}})";
	}
}
