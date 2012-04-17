/*
 * BinaryOperator.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.pnuts.lang.*;

/**
 * Abstract base class of binary operations
 * 
 * @see pnuts.lang.BooleanOperator
 */
public abstract class BinaryOperator implements Serializable {

	static BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
	static BigInteger minLong = BigInteger.valueOf(Long.MIN_VALUE);
	final static int SmallIntSize = 256;
	final static Integer smallInt[] = new Integer[SmallIntSize];
	static {
		for (int i = 0; i < SmallIntSize; i++) {
			smallInt[i] = new Integer(i);
		}
	}

	/**
	 * Operation on ints
	 */
	protected Object op_int(int i1, int i2) {
		throw new IllegalArgumentException(i1 + ", " + i2);
	}

	/**
	 * Operation on longs
	 */
	protected Object op_long(long l1, long l2) {
		throw new IllegalArgumentException(l1 + ", " + l2);
	}

	/**
	 * Operation on floats
	 */
	protected Object op_float(float f1, float f2) {
		throw new IllegalArgumentException(f1 + ", " + f2);
	}

	/**
	 * Operation on doubles
	 */
	protected Object op_double(double d1, double d2) {
		throw new IllegalArgumentException(d1 + ", " + d2);
	}

	/**
	 * Operation on BigDecimal's
	 */
	protected Object op_bdec(BigDecimal d1, BigDecimal d2) {
		throw new IllegalArgumentException(d1 + ", " + d2);
	}

	/**
	 * Operation on BigInteger's
	 */
	protected Object op_bint(BigInteger b1, BigInteger b2) {
		throw new IllegalArgumentException(b1 + ", " + b2);
	}

	/**
	 * Operation on a Numeric and an Object
	 */
	protected Object op_numeric(Numeric n1, Object n2) {
		throw new IllegalArgumentException(n1 + ", " + n2);
	}

	/**
	 * Operation on an Object and a Numeric
	 */
	protected Object op_numeric(Object n1, Numeric n2) {
		throw new IllegalArgumentException(n1 + ", " + n2);
	}

	/**
	 * Operation on Object's
	 */
	protected Object op_object(Object o1, Object o2) {
		throw new IllegalArgumentException(o1 + ", " + o2);
	}

	/**
	 * Operation on a String and an Object
	 */
	protected Object op_string(String n1, Object n2) {
		throw new IllegalArgumentException(n1 + ", " + n2);
	}

	/**
	 * Operation on an Object and a String
	 */
	protected Object op_string(Object n1, String n2) {
		throw new IllegalArgumentException(n1 + ", " + n2);
	}

	/**
	 * Operation on booleans
	 */
	protected Object op_boolean(boolean b1, boolean b2) {
		throw new IllegalArgumentException(b1 + ", " + b2);
	}

	/**
	 * Dispatches a binary expression to a particular operation
	 */
	public Object operateOn(Object n1, Object n2) {
		int t1;
		int t2;
		if (n1 instanceof Integer) {
			t1 = 1;
		} else if (n1 instanceof Character) {
			t1 = 2;
		} else if (n1 instanceof Byte) {
			t1 = 4;
		} else if (n1 instanceof Short) {
			t1 = 8;
		} else if (n1 instanceof Long) {
			t1 = 16;
		} else if (n1 instanceof Float) {
			t1 = 32;
		} else if (n1 instanceof Double) {
			t1 = 64;
		} else if (n1 instanceof String) {
			t1 = 128;
		} else if (n1 instanceof BigDecimal) {
			t1 = 256;
		} else if (n1 instanceof BigInteger) {
			t1 = 512;
		} else if (n1 instanceof Boolean) {
			t1 = 1024;
		} else if (n1 instanceof Numeric) {
			t1 = 2048;
		} else {
			t1 = 0;
		}
		if (n2 instanceof Integer) {
			t2 = 1;
		} else if (n2 instanceof Character) {
			t2 = 2;
		} else if (n2 instanceof Byte) {
			t2 = 4;
		} else if (n2 instanceof Short) {
			t2 = 8;
		} else if (n2 instanceof Long) {
			t2 = 16;
		} else if (n2 instanceof Float) {
			t2 = 32;
		} else if (n2 instanceof Double) {
			t2 = 64;
		} else if (n2 instanceof String) {
			t2 = 128;
		} else if (n2 instanceof BigDecimal) {
			t2 = 256;
		} else if (n2 instanceof BigInteger) {
			t2 = 512;
		} else if (n2 instanceof Boolean) {
			t2 = 1024;
		} else if (n2 instanceof Numeric) {
			t2 = 2048;
		} else {
			t2 = 0;
		}

		switch ((t1 << 16) | t2) {

		case (1 << 16) + 4: // int, byte
		case (1 << 16) + 8: // int, short
		case (4 << 16) + 1: // byte, int
		case (4 << 16) + 4: // byte, byte
		case (4 << 16) + 8: // byte, short
		case (8 << 16) + 1: // short, int
		case (8 << 16) + 4: // short, byte
		case (8 << 16) + 8: // short, short
			return op_int(((Number) n1).intValue(), ((Number) n2).intValue());

		case (1 << 16) + 1: // int, int
			return op_int(((Integer) n1).intValue(), ((Integer) n2).intValue());

		case (4 << 16) + 2: // byte, char
		case (8 << 16) + 2: // short, char
			return op_int(((Number) n1).intValue(), (int) ((Character) n2)
					.charValue());

		case (1 << 16) + 2: // int, char
			return op_int(((Integer) n1).intValue(), (int) ((Character) n2)
					.charValue());

		case (1 << 16) + 16: // int, long
		case (16 << 16) + 1: // long, int
		case (16 << 16) + 16: // long, long
			return op_long(((Number) n1).longValue(), ((Number) n2).longValue());

		case (2 << 16) + 4: // char, byte
		case (2 << 16) + 8: // char, short
			return op_int((int) ((Character) n1).charValue(), ((Number) n2)
					.intValue());

		case (2 << 16) + 1: // char, int
			return op_int((int) ((Character) n1).charValue(), ((Integer) n2)
					.intValue());

		case (2 << 16) + 2: // char, char
			return op_int((int) ((Character) n1).charValue(),
					(int) ((Character) n2).charValue());

		case (2 << 16) + 16: // char, long
			return op_long((long) ((Character) n1).charValue(), ((Long) n2)
					.longValue());

		case (16 << 16) + 2: // long, char
			return op_long(((Long) n1).longValue(), (long) ((Character) n2)
					.charValue());

		case (1 << 16) + 32: // int,float
		case (16 << 16) + 32: // long,float
		case (32 << 16) + 1: // float, int
		case (32 << 16) + 16: // float, long
		case (32 << 16) + 32: // float, float
			return op_float(((Number) n1).floatValue(), ((Number) n2)
					.floatValue());

		case (1 << 16) + 64: // int, double
		case (16 << 16) + 64: // long, double
		case (32 << 16) + 64: // float, double
		case (64 << 16) + 1: // double , int
		case (64 << 16) + 16: // double, long
		case (64 << 16) + 32: // double, float
		case (64 << 16) + 64: // double, double
			return op_double(((Number) n1).doubleValue(), ((Number) n2)
					.doubleValue());

		case (2 << 16) + 32: // char, float
			return op_float((float) ((Character) n1).charValue(), ((Float) n2)
					.floatValue());

		case (32 << 16) + 2: // float, char
			return op_float(((Float) n1).floatValue(), (float) ((Character) n2)
					.charValue());

		case (2 << 16) + 64: // char, double
			return op_double((double) ((Character) n1).charValue(),
					((Double) n2).doubleValue());

		case (64 << 16) + 2: // double, char
			return op_double(((Double) n1).doubleValue(),
					(double) ((Character) n2).charValue());

		case (1 << 16) + 512: // int, bigint
		case (4 << 16) + 512: // byte, bigint
		case (8 << 16) + 512: // short, bigint
		case (16 << 16) + 512: // long, bigint
			return op_bint(BigInteger.valueOf(((Number) n1).longValue()),
					(BigInteger) n2);

		case (2 << 16) + 512: // char, bigint
			return op_bint(BigInteger.valueOf((long) ((Character) n1)
					.charValue()), (BigInteger) n2);

		case (512 << 16) + 1: // bigint, int
		case (512 << 16) + 4: // bigint, byte
		case (512 << 16) + 8: // bigint, short
		case (512 << 16) + 16: // bigint, long
			return op_bint((BigInteger) n1, BigInteger.valueOf(((Number) n2)
					.longValue()));

		case (512 << 16) + 2: // bigint, char
			return op_bint((BigInteger) n1, BigInteger
					.valueOf((long) ((Character) n2).charValue()));

		case (512 << 16) + 512: // bigint, bigint
			return op_bint((BigInteger) n1, (BigInteger) n2);

		case (512 << 16) + 32: // bigint, float
		{
			float ff = ((Float) n2).floatValue();
			if (Float.isInfinite(ff) || Float.isNaN(ff)) {
				return n2;
			}
			return op_bdec(new BigDecimal((BigInteger) n1), doubleToDecimal(ff));
		}
		case (512 << 16) + 64: // bigint, double
		{
			double dd = ((Double) n2).doubleValue();
			if (Double.isInfinite(dd) || Double.isNaN(dd)) {
				return n2;
			}
			return op_bdec(new BigDecimal((BigInteger) n1), doubleToDecimal(dd));
		}
		case (32 << 16) + 512: // float, bigint
		{
			float ff = ((Float) n1).floatValue();
			if (Float.isInfinite(ff) || Float.isNaN(ff)) {
				return n1;
			}
			return op_bdec(doubleToDecimal(ff), new BigDecimal((BigInteger) n2));
		}
		case (64 << 16) + 512: // double, bigint
		{
			double dd = ((Double) n1).doubleValue();
			if (Double.isInfinite(dd) || Double.isNaN(dd)) {
				return n1;
			}
			return op_bdec(doubleToDecimal(dd), new BigDecimal((BigInteger) n2));
		}
		case (1 << 16) + 256: // int, decimal
		case (4 << 16) + 256: // byte, decimal
		case (8 << 16) + 256: // short, decimal
		case (16 << 16) + 256: // long, decimal
		       BigDecimal b1 =
			   Configuration.normalConfiguration.longToBigDecimal(((Number) n1).longValue());
		       return op_bdec(b1, (BigDecimal) n2);

		case (2 << 16) + 256: // char, decimal
		       BigDecimal d1 =
			   Configuration.normalConfiguration.longToBigDecimal((long)((Character) n1).charValue());
		       return op_bdec(d1, (BigDecimal) n2);

		case (32 << 16) + 256: // float, decimal
		{
			float ff = ((Float) n1).floatValue();
			if (Float.isInfinite(ff) || Float.isNaN(ff)) {
				return n1;
			}
			return op_bdec(doubleToDecimal(ff), (BigDecimal) n2);
		}
		case (64 << 16) + 256: // double, decimal
		{
			double dd = ((Double) n1).doubleValue();
			if (Double.isInfinite(dd) || Double.isNaN(dd)) {
				return n1;
			}
			return op_bdec(doubleToDecimal(dd), (BigDecimal) n2);
		}
		case (256 << 16) + 1: // decimal, int
		case (256 << 16) + 4: // decimal, byte
		case (256 << 16) + 8: // decimal, short
		case (256 << 16) + 16: // decimal, long
		       BigDecimal b2 =
			   Configuration.normalConfiguration.longToBigDecimal(((Number) n2).longValue());
			return op_bdec((BigDecimal) n1, b2);

		case (256 << 16) + 2: // decimal, char
		       BigDecimal d2 =
			   Configuration.normalConfiguration.longToBigDecimal((long)((Character) n2).charValue());
		       return op_bdec((BigDecimal) n1, d2);

		case (256 << 16) + 32: // decimal, float
			float ff = ((Float) n2).floatValue();
			if (Float.isInfinite(ff) || Float.isNaN(ff)) {
				return n2;
			}
			return op_bdec((BigDecimal) n1, doubleToDecimal(ff));
		case (256 << 16) + 64: // decimal, double
			double dd = ((Double) n2).doubleValue();
			if (Double.isInfinite(dd) || Double.isNaN(dd)) {
				return n2;
			}
			return op_bdec((BigDecimal) n1, doubleToDecimal(dd));

		case (256 << 16) + 256: // decimal, decimal
			return op_bdec((BigDecimal) n1, (BigDecimal) n2);

		case (1024 << 16) + 1024: // boolean, boolean
			return op_boolean(((Boolean) n1).booleanValue(), ((Boolean) n2)
					.booleanValue());

		case (128 << 16) + 1: // String, int
		case (128 << 16) + 2: // String, char
		case (128 << 16) + 4: // String, byte
		case (128 << 16) + 8: // String, shorc
		case (128 << 16) + 16: // String, long
		case (128 << 16) + 32: // String, float
		case (128 << 16) + 64: // String, double
		case (128 << 16) + 128: // String, String
		case (128 << 16) + 256: // String, BigDecimal
		case (128 << 16) + 512: // String, BigInteger
		case (128 << 16) + 1024: // String, booolean
		case (128 << 16) + 2048: // String, Numeric
		case (128 << 16) + 0: // String, Object
			return op_string((String) n1, n2);

		case (1 << 16) + 128: // int, String
		case (2 << 16) + 128: // char, String
		case (4 << 16) + 128: // byte, String
		case (8 << 16) + 128: // short, String
		case (16 << 16) + 128: // long, String
		case (32 << 16) + 128: // float, String
		case (64 << 16) + 128: // double, String
		case (256 << 16) + 128: // BigDecimal, String
		case (512 << 16) + 128: // BigInteger, String
		case (1024 << 16) + 128: // boolean, String
		case (2048 << 16) + 128: // Numeric, String
		case (0 << 16) + 128: // Object, String
			return op_string(n1, (String) n2);

		case (2048 << 16) + 1: // Numeric, int
		case (2048 << 16) + 2: // Numeric, char
		case (2048 << 16) + 4: // Numeric, byte
		case (2048 << 16) + 8: // Numeric, short
		case (2048 << 16) + 16: // Numeric, long
		case (2048 << 16) + 32: // Numeric, float
		case (2048 << 16) + 64: // Numeric, double
		case (2048 << 16) + 256: // Numeric, BigDecimal
		case (2048 << 16) + 512: // Numeric, BigInteger
		case (2048 << 16) + 2048: // Numeric, Numeric
			return op_numeric((Numeric) n1, n2);

		case (1 << 16) + 2048: // int, Numeric
		case (2 << 16) + 2048: // char, Numeric
		case (4 << 16) + 2048: // byte, Numeric
		case (8 << 16) + 2048: // short, Numeric
		case (16 << 16) + 2048: // long, Numeric
		case (32 << 16) + 2048: // float, Numeric
		case (64 << 16) + 2048: // double, Numeric
		case (256 << 16) + 2048: // BigDecimal, Numeric
		case (512 << 16) + 2048: // BigInteger, Numeric
			return op_numeric(n1, (Numeric) n2);

		default:
			return op_object(n1, n2);
		}
	}

	static BigDecimal doubleToDecimal(double d) {
		return (BigDecimal) Runtime.decimalNumber("" + d, 10);
	}

	protected static Number compressNumber(Number n) {
		return Runtime.compress(n);
	}

	/**
	 * The default implementation of + operator
	 */
	public static class Add extends BinaryOperator {

		static Add instance = new Add();

		public Object op_int(int i1, int i2) {
			long l = (long) i1 + i2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			} else if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
				return new Integer((int) l);
			} else {
				return new Long(l);
			}
		}

		public Object op_long(long d1, long d2) {
			long d3 = d1 + d2;
			if (d3 > d1 && d2 < 0 || d3 < d1 && d2 > 0) {
				return Runtime.compress(BigInteger.valueOf(d1).add(
						BigInteger.valueOf(d2)));
			}
			if (d3 >= 0 && d3 < SmallIntSize) {
				return smallInt[(int) d3];
			} else if (d3 <= Integer.MAX_VALUE && d3 >= Integer.MIN_VALUE) {
				return new Integer((int) d3);
			} else {
				return new Long(d3);
			}
		}

		public Object op_float(float f1, float f2) {
			return new Float(f1 + f2);
		}

		public Object op_double(double d1, double d2) {
			return new Double(d1 + d2);
		}

		public Object op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.add(d2);
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.add(b2));
		}

		public Object op_numeric(Numeric n1, Object n2) {
			return n1.add(n2);
		}

		public Object op_numeric(Object n1, Numeric n2) {
			return n2.add(n1);
		}

		public Object op_string(String n1, Object n2) {
			StringBuffer sbuf = new StringBuffer(n1);
			concat(n2, sbuf);
			return sbuf.toString();
		}

		public Object op_string(Object n1, String n2) {
			StringBuffer sbuf = new StringBuffer();
			concat(n1, sbuf);
			sbuf.append(n2);
			return sbuf.toString();
		}

		public Object op_object(Object o1, Object o2) {
			boolean array1 = Runtime.isArray(o1);
			boolean array2 = Runtime.isArray(o2);
			if (array1){
				return appendArray(o1, o2);
			} else if (o1 instanceof Collection){
				if (array2){
					Class cls = o1.getClass();
					try {
						Collection c = (Collection)cls.newInstance();
						c.addAll((Collection)o1);
						int len = Array.getLength(o2);
						for (int i = 0; i < len; i++){
							c.add(Array.get(o2, i));
						}
						return c;
					} catch (IllegalAccessException e0){
						throw new IllegalArgumentException(o1 + ", " + o2);
					} catch (InstantiationException e){
						throw new IllegalArgumentException(o1 + ", " + o2);
					}
				} else if (o2 instanceof Collection){
					try {
						Class cls = o1.getClass();
						Collection c = (Collection)cls.newInstance();
						c.addAll((Collection)o1);
						c.addAll((Collection)o2);
						return c;
					} catch (IllegalAccessException e0){
						throw new IllegalArgumentException(o1 + ", " + o2);
					} catch (InstantiationException e){
						throw new IllegalArgumentException(o1 + ", " + o2);
					} 
				}
			}
			if (o1 instanceof Map && o2 instanceof Map){
			    Class cls = o1.getClass();
			    try {
				Map m = (Map)cls.newInstance();
				m.putAll((Map)o1);
				m.putAll((Map)o2);
				return m;
			    } catch (IllegalAccessException e0){
				throw new IllegalArgumentException(o1 + ", " + o2);
			    } catch (InstantiationException e){
				throw new IllegalArgumentException(o1 + ", " + o2);
			    }
			}
			if (o1 instanceof Generator && o2 instanceof Generator){
				return appendGenerator((Generator)o1, (Generator)o2);
			}
			return super.op_object(o1, o2);
		}
	}

	static void concat(Object o, StringBuffer sbuf) {
		String s;
		if (Runtime.isArray(o)) {
		    s = Pnuts.format(o);
		} else {
		    s = String.valueOf(o);
		}
		sbuf.append(s);
	}

	static Object appendGenerator(Generator g1, Generator g2) {
	    return new CompositeGenerator(g1, g2);
	}

	static Object appendArray(Object p1, Object p2) {
		int l1 = Runtime.getArrayLength(p1);
		int l2;

		boolean notList = false;
		if (p2 instanceof Collection){
		    l2 = ((Collection)p2).size();
		    if (!(p2 instanceof List)){
			notList = true;
		    }
		} else{
		    l2 = Runtime.getArrayLength(p2);
		}
		Class componentType = p1.getClass().getComponentType();
		Object p3 = Array.newInstance(componentType, l1 + l2);
		System.arraycopy(p1, 0, p3, 0, l1);
		if (notList){
		    int i = 0;
		    for (Iterator it = ((Collection)p2).iterator(); it.hasNext();){
			Object o2 = it.next();
			if (!componentType.isInstance(o2)){
			    o2 = Runtime.transform(componentType, o2);
			}
			Array.set(p3, i++ + l1, o2);
		    }
		} else {
		    for (int i = 0; i < l2; i++) {
			Object o2;
			if (p2 instanceof List){
			    o2 = ((List)p2).get(i);
			} else {
			    o2 = Array.get(p2, i);
			}
			if (!componentType.isInstance(o2)){
			    o2 = Runtime.transform(componentType, o2);
			}
			Array.set(p3, i + l1, o2);
		    }
		}
		return p3;
	}

	/**
	 * The default implementation of - operator
	 */
	public static class Subtract extends BinaryOperator {

		static Subtract instance = new Subtract();

		public Object op_int(int i1, int i2) {
			long l = (long) i1 - i2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			} else if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
				return new Integer((int) l);
			} else {
				return new Long(l);
			}
		}

		public Object op_long(long d1, long d2) {
			long d3 = d1 - d2;
			if (d3 > d1 && d2 > 0 || d3 < d1 && d2 < 0) {
				return op_bint(BigInteger.valueOf(d1), BigInteger.valueOf(d2));
			}
			if (d3 <= Integer.MAX_VALUE && d3 >= Integer.MIN_VALUE) {
				return new Integer((int) d3);
			} else {
				return new Long(d3);
			}
		}

		public Object op_float(float f1, float f2) {
			return new Float(f1 - f2);
		}

		public Object op_double(double d1, double d2) {
			return new Double(d1 - d2);
		}

		public Object op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.subtract(d2);
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.subtract(b2));
		}

		public Object op_numeric(Numeric n1, Object n2) {
			return n1.subtract(n2);
		}

		public Object op_numeric(Object n1, Numeric n2) {
			return Add.instance.operateOn(n2.negate(), n1);
		}

		public Object op_object(Object o1, Object o2) {
			boolean array1 = false;
			boolean array2 = false;
			Collection c1 = null;
			Collection c2 = null;
			if (o1 instanceof Collection){
				c1 = (Collection)o1;
			} else if (Runtime.isArray(o1)){
				array1 = true;
			} else {
				return super.op_object(o1, o2);
			}
			if (o2 instanceof Collection){
				c2 = (Collection)o2;
			} else if (Runtime.isArray(o2)){
				array2 = true;
			} else {
				return super.op_object(o1, o2);
			}
			
			if (array1){
				c1 = new ArrayList();
				for (int i = 0; i < Runtime.getArrayLength(o1); i++){
					c1.add(Array.get(o1, i));
				}
			}
			if (array2){
				c2 = new ArrayList();
				for (int i = 0; i < Runtime.getArrayLength(o2); i++){
					c2.add(Array.get(o2, i));
				}
			}
		    
			if (c1 != null && c2 != null){
				Class cls = c1.getClass();
				try {
					Collection c = (Collection)cls.newInstance();
					c.addAll(c1);
					c.removeAll(c2);
					if (array1){
						Object array = Array.newInstance(o1.getClass().getComponentType(),
									 c.size());
						int i = 0;
						for (Iterator it = c.iterator(); it.hasNext();){
							Array.set(array, i++, it.next());
						}
						return array;
					} else {
						return c;
					}
				} catch (Exception e){
					throw new IllegalArgumentException(e.getMessage());
				}
			}
			return super.op_object(o1, o2);
		}
	}

	/**
	 * The default implementation of * operator
	 */
	public static class Multiply extends BinaryOperator {

		static Multiply instance = new Multiply();

		public Object op_int(int i1, int i2) {
			long l = (long) i1 * i2;
			if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
				return new Integer((int) l);
			} else {
				return new Long(l);
			}
		}

		public Object op_long(long d1, long d2) {
			long d3 = d1 * d2;
			if (d2 == 0) {
				return new Long(0L);
			}
			if (d3 / d2 == d1) {
				if (d3 <= Integer.MAX_VALUE && d3 >= Integer.MIN_VALUE) {
					return new Integer((int) d3);
				} else {
					return new Long(d3);
				}
			} else {
				return op_bint(BigInteger.valueOf(d1), BigInteger.valueOf(d2));
			}
		}

		public Object op_float(float f1, float f2) {
			return new Float(f1 * f2);
		}

		public Object op_double(double d1, double d2) {
			return new Double(d1 * d2);
		}

		public Object op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.multiply(d2);
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.multiply(b2));
		}

		public Object op_numeric(Numeric n1, Object n2) {
			return n1.multiply(n2);
		}

		public Object op_numeric(Object n1, Numeric n2) {
			return n2.multiply(n1);
		}

		public Object op_object(Object o1, Object o2) {
			if (o1 instanceof Set && o2 instanceof Set){
				Set c1 = (Set)o1;
				Set c2 = (Set)o2;
				Class cls = c1.getClass();
				try {
					Collection c = (Collection)cls.newInstance();
					c.addAll(c1);
					c.retainAll(c2);
					return c;
				} catch (Exception e){
					throw new IllegalArgumentException(e.getMessage());
				}
			} else {
				return super.op_object(o1, o2);
			}
		}
	}

	/**
	 * The default implementation of / operator
	 */
	public static class Divide extends BinaryOperator {

		static Divide instance = new Divide();

		public Object op_int(int i1, int i2) {
			int l = i1 / i2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			}
			return new Integer(l);
		}

		public Object op_long(long d1, long d2) {
			long d3 = d1 / d2;
			if (d3 <= Integer.MAX_VALUE && d3 >= Integer.MIN_VALUE) {
				return new Integer((int) d3);
			} else {
				return new Long(d3);
			}
		}

		public Object op_float(float f1, float f2) {
			return new Float(f1 / f2);
		}

		public Object op_double(double d1, double d2) {
			return new Double(d1 / d2);
		}

		public Object op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.divide(d2, BigDecimal.ROUND_HALF_DOWN);
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.divide(b2));
		}

		public Object op_numeric(Numeric n1, Object n2) {
			return n1.divide(n2);
		}

		public Object op_numeric(Object n1, Numeric n2) {
			return Multiply.instance.operateOn(n2.inverse(), n1);
		}
	}

	/**
	 * The default implementation of % operator
	 */
	public static class Mod extends BinaryOperator {

		static Mod instance = new Mod();

		public Object op_int(int i1, int i2) {
			int l = i1 % i2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			}
			return new Integer(l);
		}

		public Object op_long(long d1, long d2) {
			long d3 = d1 % d2;
			if (d3 <= Integer.MAX_VALUE && d3 >= Integer.MIN_VALUE) {
				return new Integer((int) d3);
			} else {
				return new Long(d3);
			}
		}

		public Object op_float(float f1, float f2) {
			throw new IllegalArgumentException();
		}

		public Object op_double(double d1, double d2) {
			throw new IllegalArgumentException();
		}

		public Object op_bdec(BigDecimal d1, BigDecimal d2) {
			throw new IllegalArgumentException();
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.mod(b2));
		}
	}

	/**
	 * The default implementation of >>> operator
	 */
	public static class ShiftArithmetic extends BinaryOperator {

		static ShiftArithmetic instance = new ShiftArithmetic();

		public Object op_int(int d1, int d2) {
			int l = d1 >>> d2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			}
			return new Integer(l);
		}

		public Object op_long(long d1, long d2) {
			long l = d1 >>> d2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			} else if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
				return new Integer((int) l);
			} else {
				return new Long(l);
			}
		}
	}

	/**
	 * The default implementation of < operator
	 */
	public static class ShiftLeft extends BinaryOperator {

		static ShiftLeft instance = new ShiftLeft();

		public Object op_int(int d1, int d2) {
			int m = 0;
			long dd = (long) d1;
			while (dd > 0) {
				dd >>= 1;
				m++;
			}
			if (d2 + m > 63) {
				return Runtime.compress(BigInteger.valueOf(d1).shiftLeft(
						(int) d2));
			} else {
				long l = (long) d1 << d2;
				if (l >= 0 && l < SmallIntSize) {
					return smallInt[(int) l];
				} else if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
					return new Integer((int) l);
				} else {
					return new Long(l);
				}
			}
		}

		public Object op_long(long d1, long d2) {
			int m = 0;
			long dd = d1;
			while (dd > 0) {
				dd >>= 1;
				m++;
			}
			if (d2 + m > 63) {
				return Runtime.compress(BigInteger.valueOf(d1).shiftLeft(
						(int) d2));
			} else {
				long l = d1 << d2;
				if (l >= 0 && l < SmallIntSize) {
					return smallInt[(int) l];
				} else if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
					return new Integer((int) l);
				} else {
					return new Long(l);
				}
			}
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.shiftLeft(b2.intValue()));
		}
	}

	/**
	 * The default implementation of > operator
	 */
	public static class ShiftRight extends BinaryOperator {

		static ShiftRight instance = new ShiftRight();

		public Object op_int(int d1, int d2) {
			int l = d1 >> d2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			}
			return new Integer(l);
		}

		public Object op_long(long d1, long d2) {
			long l = d1 >> d2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			} else if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
				return new Integer((int) l);
			} else {
				return new Long(l);
			}
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.shiftRight(b2.intValue()));
		}
	}

	/**
	 * The default implementation of & operator
	 */
	public static class And extends BinaryOperator {

		static And instance = new And();

		public Object op_int(int i1, int i2) {
			int l = i1 & i2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			}
			return new Integer(l);
		}

		public Object op_long(long d1, long d2) {
			return new Long(d1 & d2);
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.and(b2));
		}

		public Object op_boolean(boolean b1, boolean b2) {
			return (b1 & b2) ? Boolean.TRUE : Boolean.FALSE;
		}
	}

	/**
	 * The default implementation of | operator (bitwise OR)
	 */
	public static class Or extends BinaryOperator {

		static Or instance = new Or();

		public Object op_int(int i1, int i2) {
			int l = i1 | i2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			}
			return new Integer(l);
		}

		public Object op_long(long d1, long d2) {
			return new Long(d1 | d2);
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.or(b2));
		}

		public Object op_boolean(boolean b1, boolean b2) {
			return (b1 | b2) ? Boolean.TRUE : Boolean.FALSE;
		}
	}

	/**
	 * The default implementation of ^ operator
	 */
	public static class Xor extends BinaryOperator {

		static Xor instance = new Xor();

		public Object op_int(int i1, int i2) {
			int l = i1 ^ i2;
			if (l >= 0 && l < SmallIntSize) {
				return smallInt[(int) l];
			}
			return new Integer(l);
		}

		public Object op_long(long d1, long d2) {
			return new Long(d1 ^ d2);
		}

		public Object op_bint(BigInteger b1, BigInteger b2) {
			return Runtime.compress(b1.xor(b2));
		}

		public Object op_boolean(boolean b1, boolean b2) {
			return (b1 ^ b2) ? Boolean.TRUE : Boolean.FALSE;
		}
	}
}
