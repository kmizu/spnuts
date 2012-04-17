/*
 * MerlinConfiguration.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import pnuts.lang.Package;

/**
 * This class defines the interface of runtime configuration, such as how to
 * find method/field candidates, how to get the field value, how to get indexed
 * elements, and so on. This class also provides the default implementation for
 * J2SDK1.4 of this interface.
 * 
 * @see pnuts.lang.Configuration
 */
class MerlinConfiguration extends Java2Configuration {

	MerlinConfiguration() {
	}

	MerlinConfiguration(Class stopClass) {
		super(stopClass);
	}

	public Object getElement(Context context, Object target, Object key) {
		if (target instanceof Object[]) {
			if (key instanceof Number) {
				int idx = ((Number)key).intValue();
				Object[] array = (Object[])target;
				int sz = array.length;
				if (idx < 0 && idx >= -sz){
					idx += sz;
				}			    
				return array[idx];
			} else if (key instanceof PnutsFunction) {
				return filterGenerator((Object[]) target, (PnutsFunction) key,
						context);
			}
		} else if (target instanceof Indexed) {
			if (key instanceof Number) {
				return ((Indexed) target).get(((Number) key).intValue());
			} else if (key instanceof String) {
				return getBeanProperty(context, target, (String) key);
			}
		} else if (target instanceof Package) {
			return ((Property) target).get(((String) key).intern(), context);
		} else if (target instanceof Property) {
			return ((Property) target).get((String) key, context);
		} else if (target instanceof Map) {
			return ((Map) target).get(key);
		} else if (target instanceof Context) {
			return ((Context) target).get(((String) key).intern());
		} else if (target instanceof Generator) {
			if (key instanceof Number) {
				return generateNth((Generator) target, ((Number) key)
						.intValue(), context);
			} else if (key instanceof PnutsFunction) {
				return filterGenerator((Generator) target, (PnutsFunction) key,
						context);
			}
		} else {
			if (key instanceof PnutsFunction) {
				if (target instanceof Collection) {
					return filterGenerator((Collection) target,
							(PnutsFunction) key, context);
				} else if ((target instanceof int[])
						|| (target instanceof byte[])
						|| (target instanceof short[])
						|| (target instanceof char[])
						|| (target instanceof long[])
						|| (target instanceof float[])
						|| (target instanceof double[])
						|| (target instanceof boolean[])) {
					return filterGenerator((Object) target,
							(PnutsFunction) key, context);
				}
			} else if (key instanceof String) {
				return getBeanProperty(context, target, (String) key);
			} else if (key instanceof Number) {
				int idx = ((Number)key).intValue();
				if (target instanceof List) {
					List list = (List) target;
					int sz = list.size();
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return list.get(idx);
				} else if (target instanceof String) {
					String s = (String)target;
					int sz = s.length();
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Character(s.charAt(idx));
				} else if (target instanceof CharSequence) {
					CharSequence s = (CharSequence)target;
					int sz = s.length();
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Character(s.charAt(idx));
				} else if (target instanceof int[]) {
					int[] array = (int[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Integer(array[idx]);
				} else if (target instanceof byte[]) {
					byte[] array = (byte[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Byte(array[idx]);
				} else if (target instanceof char[]) {
					char[] array = (char[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Character(array[idx]);
				} else if (target instanceof float[]) {
					float[] array = (float[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Float(array[idx]);
				} else if (target instanceof double[]) {
					double[] array = (double[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Double(array[idx]);
				} else if (target instanceof boolean[]) {
					boolean[] array = (boolean[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return Boolean.valueOf(array[idx]);
				} else if (target instanceof long[]) {
					long[] array = (long[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Long(array[idx]);
				} else if (target instanceof short[]) {
					short[] array = (short[])target;
					int sz = array.length;
					if (idx < 0 && idx >= -sz){
						idx += sz;
					}
					return new Short(array[idx]);
				} else if (target instanceof Map.Entry) {
					if (idx == 0){
					    return ((Map.Entry)target).getKey();
					} else if (idx == 1){
					    return ((Map.Entry)target).getValue();
					} else {
					    return null;
					}
				}
			}
		}
		if (target == null){
		    throw new NullPointerException();
		}
		throw new PnutsException("illegal argument [key: " + String.valueOf(key) + ", target: " + target + "]", context);
	}

	/**
	 * Defines the semantices of an expression like:
	 * <p>
	 * 
	 * <pre>
	 * target[idx1..idx2]
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param context
	 *            the context
	 * @param target
	 *            the target object
	 * @param idx1
	 *            the start index
	 * @param idx2
	 *            the end index, which can be null
	 */
	public Object getRange(Context context, Object target, Object idx1,
			Object idx2) {
		int from = ((Number) idx1).intValue();
		int to = -1;
		if (idx2 != null) {
			to = ((Number) idx2).intValue();
		}
		if (target instanceof CharSequence) {
			CharSequence s = (CharSequence) target;
			int len = s.length();
			if (from > len - 1) {
				return s.subSequence(0, 0);
			}
			if (idx2 != null) {
				if (from > to || to < 0) {
					return s.subSequence(0, 0);
				}
				if (from < 0) {
					from = 0;
				}
				if (to > len - 1) {
					to = len - 1;
				}
				return s.subSequence(from, to + 1);
			} else {
				if (from < 0) {
					from = 0;
				}
				return s.subSequence(from, len);
			}
		} else if (Runtime.isArray(target)) {
			Class c = target.getClass().getComponentType();
			int len = Runtime.getArrayLength(target);
			if (from > len - 1) {
				return Array.newInstance(c, 0);
			}
			if (idx2 == null) {
				to = len - 1;
			} else {
				if (from > to || to < 0) {
					return Array.newInstance(c, 0);
				}
				if (to > len - 1) {
					to = len - 1;
				}
			}
			if (from < 0) {
				from = 0;
			}
			int size = to - from + 1;
			if (size < 0) {
				size = 0;
			} else if (from + size > len) {
				size = len - from;
			}
			Object ret = Array.newInstance(c, size);
			if (size > 0) {
				System.arraycopy(target, from, ret, 0, size);
			}
			return ret;
		} else if (target instanceof List) {
			List list = (List) target;
			try {
				int size = list.size();
				if (from < 0) {
					from = 0;
				} else if (from > size) {
					from = size;
				}
				if (idx2 == null) {
					to = size;
				} else {
					if (from > to || to < 0) {
						to = from;
					} else {
						to++;
						if (to > size) {
							to = size;
						}
					}
				}
			} catch (Exception e){
				// allow size() to throw an exception
			}
			return list.subList(from, to);
		} else if (target instanceof Generator) {
			if (idx2 != null) {
				if (from > to || to < 0) {
					from = -1;
					to = -1;
				} else if (from < 0) {
					from = 0;
				}
			} else {
				to = -1;
				if (from < 0) {
					from = 0;
				}
			}
			return new Runtime.RangeGenerator((Generator) target, from, to);
		} else {
			throw new PnutsException("illegal.type", new Object[]{target},
					context);
		}
	}

	static class CharSequenceEnum implements Enumeration {
		CharSequence seq;

		int len;

		int pos = 0;

		CharSequenceEnum(CharSequence seq) {
			this.seq = seq;
			this.len = seq.length();
		}

		public boolean hasMoreElements() {
			return len > pos;
		}

		public Object nextElement() {
			return new Character(seq.charAt(pos++));
		}
	}

	public Enumeration toEnumeration(Object obj) {
		Enumeration en = super.toEnumeration(obj);
		if (en == null) {
			if (obj instanceof CharSequence) {
				en = new CharSequenceEnum((CharSequence) obj);
			}
		}
		return en;
	}
}
