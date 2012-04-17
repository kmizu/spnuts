/*
 * project.java
 *
 * Copyright (c) 2004-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.*;
import pnuts.lang.Runtime;
import java.util.*;

public class project extends PnutsFunction {

	public project(){
		super("project");
	}

	public boolean defined(int narg){
		return (narg == 2);
	}

	protected Object exec(final Object args[], final Context context){
		if (args.length != 2){
			undefined(args, context);
			return null;
		}
		final Object f = args[1];
		Object a1 = args[0];
	
		if (a1 instanceof Object[]){
			Object[] x = (Object[])a1;
			return new ProjectionList(new SimpleArrayList(x)){
				protected Object project(Object obj){
				    return Runtime.call(context, f, new Object[]{obj}, null);
				}
			    };
		} else if (a1 instanceof Set){
			final Set x = (Set)a1;
			return new CloneableSet(){
				public int size(){
				    return x.size();
				}
				public Iterator iterator(){
				    return new ProjectionIterator(x.iterator()){
					    public Object project(Object obj){
						return Runtime.call(context, f, new Object[]{obj}, null);
					    }
					};
				}
				public Object clone(){
				    Set s = new HashSet();
				    for (Iterator it = iterator(); it.hasNext();){
					s.add(it.next());
				    }
				    return s;
				}
			    };
		} else if (a1 instanceof List){
			final List x = (List)a1;
			return new ProjectionList(x){
				protected Object project(Object obj){
				    return Runtime.call(context, f, new Object[]{obj}, null);
				}
			    };
		} else if (a1 instanceof Iterator){
			return new ProjectionIterator((Iterator)a1){
					public Object project(Object obj){
						return Runtime.call(context, f, new Object[]{obj}, null);
					}
				};
		} else if (a1 instanceof Enumeration){
			return new ProjectionEnumeration((Enumeration)a1){
					public Object project(Object obj){
						return Runtime.call(context, f, new Object[]{obj}, null);
					}
				};
		} else if (a1 instanceof Generator){
			final Generator g = (Generator)a1;
			return new Generator(){
					public Object apply(final PnutsFunction closure, final Context context){
						return g.apply(new PnutsFunction(){
								protected Object exec(Object[] args, Context c){
									closure.call(new Object[]{Runtime.call(context, f, args, null)}, context);
									return null;
								}
							}, context);
					}
				};
		} else if (a1 instanceof Map){
		    final Map m = (Map)a1;
		    return new CloneableMap(){
			    public Object get(Object key){
				Object obj = m.get(key);
				return Runtime.call(context, f, new Object[]{obj}, null);
			    }
			    public Set entrySet(){
				return new CloneableSet(){
					public Iterator iterator(){
					    return new ProjectionIterator(m.entrySet().iterator()){
						    public Object project(Object obj){
							Map.Entry entry = (Map.Entry)obj;
							final Object key = entry.getKey();
							final Object value = entry.getValue();
							return new Map.Entry(){
								public Object getKey(){
								    return key;
								}
								public Object getValue(){
								    return Runtime.call(context, f, new Object[]{value}, null);
								}
								public Object setValue(Object value){
								    throw new UnsupportedOperationException();
								}
							    };
						    }
						};
					}
					public Object clone(){
					    Set s = new HashSet();
					    for (Iterator it = iterator(); it.hasNext();){
						s.add(it.next());
					    }
					    return s;
					}
					public int size(){
					    return m.size();
					}
				    };
			    }
			    public Object clone(){
				HashMap m = new HashMap();
				for (Iterator it = entrySet().iterator(); it.hasNext();){
				    Map.Entry entry = (Map.Entry)it.next();
				    m.put(entry.getKey(), entry.getValue());
				}
				return m;
			    }
			};
		} else if (a1 == null){
		    return null;
		} else {
			Enumeration e = context.getConfiguration().toEnumeration(a1);
			if (e != null){
			    return new ProjectionEnumeration(e){
				    public Object project(Object obj){
					return Runtime.call(context, f, new Object[]{obj}, null);
				    }
				};
			} else {
			    final Object m = a1;
			    return new pnuts.lang.Property(){
				    public Object get(String key, Context ctx){
					Object obj = context.getConfiguration().getElement(ctx, m, key);
					return Runtime.call(ctx, f, new Object[]{obj}, null);
				    }
				    public void set(String key, Object value, Context ctx){
					throw new UnsupportedOperationException();
				    }
				};
			}
		}
	}
		
	public String toString(){
		return "function project(elements, funcOrClass)";
	}
    static abstract class CloneableSet extends AbstractSet implements Cloneable {}
    static abstract class CloneableMap extends AbstractMap implements Cloneable {}
}
