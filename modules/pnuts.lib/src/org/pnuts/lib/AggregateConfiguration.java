/*
 * AggregateConfiguration.java
 *
 * Copyright (c) 2004-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import pnuts.ext.*;
import java.util.*;
import java.lang.reflect.Array;

/**
 * Aggregate mode
 *
 */
public class AggregateConfiguration extends ConfigurationAdapter {

	public AggregateConfiguration(){
	}

	public AggregateConfiguration(Configuration conf){
		super(conf);
	}

	public Object getElement(Context context, Object target, Object key){
		if (target instanceof Collection){
			if (key instanceof PnutsFunction){
				return filter((Collection)target, (PnutsFunction)key, context);
			}
			if (target instanceof List){
				if (key instanceof Number){
					return ((List)target).get(((Number)key).intValue());
				} else {
					ArrayList lst = new ArrayList();
					for (Iterator it = ((List)target).iterator(); it.hasNext();){
						lst.add(getElement(context, it.next(), key));
					}
					return lst;
				}
			} else {
				HashSet set = new HashSet();
				for (Iterator it = ((Set)target).iterator(); it.hasNext();){
					set.add(getElement(context, it.next(), key));
				}
				return set;
			}
		} else {
			return super.getElement(context, target, key);
		}
	}

	public void setElement(Context context,
						   Object target,
						   Object key,
						   Object value)
		{
			if ((target instanceof Collection) && (key instanceof PnutsFunction)){
				PnutsFunction f = (PnutsFunction)key;
				if (target instanceof List){
					List lst = (List)target;
					int n = lst.size();
					for (int i = 0; i < n; i++){
						Object ret = f.call(new Object[]{lst.get(i)}, context);
						if (Boolean.TRUE.equals(ret)){
							lst.set(i, value);
						}
					}
				} else {
					Collection c = (Collection)target;
					ArrayList lst = new ArrayList();
					for (Iterator it = c.iterator(); it.hasNext(); ){
						Object elem = it.next();
						Object ret = f.call(new Object[]{elem}, context);
						if (Boolean.TRUE.equals(ret)){
							lst.add(elem);
						}
					}
					for (Iterator it = lst.iterator(); it.hasNext();){
						if (c.remove(it.next())){
							c.add(value);
						}
					}
				}
			} else {
				super.setElement(context, target, key, value);
			}
		}

	public Object getField(Context context, Object target, String name){
		if (target instanceof Collection){
			Collection col = (Collection)target;
			Collection c;
			try {
				c = (Collection)target.getClass().newInstance();
			} catch (Exception e){
				if (target instanceof List){
					c = new ArrayList();
				} else {
					c = new HashSet();
				}
			}
			for (Iterator it = col.iterator(); it.hasNext();){
				Object elem = it.next();
				c.add(Runtime.getField(context, elem, name));
			}
			return c;
		} else {
			return super.getField(context, target, name);
		}
	}

	public void putField(Context context, Object target, String name, Object value) {
		if (target instanceof Collection){
			Collection col = (Collection)target;
			for (Iterator it = col.iterator(); it.hasNext();){
				Object elem = it.next();
				Runtime.putField(context, elem, name, value);
			}
		} else {
			super.putField(context, target, name, value);
		}
	}

	protected Object filter(Collection c, PnutsFunction f, Context context){
		Collection col;
		try {
			Class cls = c.getClass();
			col = (Collection)cls.newInstance();
		} catch (Exception e){
			if (c instanceof List){
				col = new ArrayList();
			} else {
				col = new HashSet();
			}
		}
		for (Iterator it = c.iterator(); it.hasNext();){
			Object elem = it.next();
			Object ret = f.call(new Object[]{elem}, context);
			if (ret instanceof Boolean){
				if (((Boolean)ret).booleanValue()){
					col.add(elem);
				}
			}
		}
		return col;
	}
}

