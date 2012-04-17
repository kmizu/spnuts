/*
 * @(#)BooleanOperator.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Abstract base class of boolean operations
 * 
 * @see pnuts.lang.BinaryOperator
 */
public abstract class BooleanOperator implements Serializable {

	protected boolean op_boolean(boolean b1, boolean b2) {
		throw new IllegalArgumentException(b1 + ", " + b2);
	}

	protected boolean op_int(int i1, int i2) {
		throw new IllegalArgumentException(i1 + ", " + i2);
	}

	protected boolean op_long(long l1, long l2) {
		throw new IllegalArgumentException(l1 + ", " + l2);
	}

	protected boolean op_float(float f1, float f2) {
		throw new IllegalArgumentException(f1 + ", " + f2);
	}

	protected boolean op_double(double d1, double d2) {
		throw new IllegalArgumentException(d1 + ", " + d2);
	}

	protected boolean op_bdec(BigDecimal d1, BigDecimal d2) {
		throw new IllegalArgumentException(d1 + ", " + d2);
	}

	protected boolean op_bint(BigInteger b1, BigInteger b2) {
		throw new IllegalArgumentException(b1 + ", " + b2);
	}

	protected boolean op_numeric(Numeric n1, Object n2) {
		throw new IllegalArgumentException(n1 + ", " + n2);
	}

	protected boolean op_numeric(Object n1, Numeric n2) {
		throw new IllegalArgumentException(n1 + ", " + n2);
	}

	protected boolean op_object(Object o1, Object o2) {
		throw new IllegalArgumentException(o1 + ", " + o2);
	}

	protected boolean op_string(String o1, Object o2) {
		throw new IllegalArgumentException(o1 + ", " + o2);
	}

	protected boolean op_string(Object o1, String o2) {
		throw new IllegalArgumentException(o1 + ", " + o2);
	}

	public boolean operateOn(Object n1, Object n2) {
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
			return op_bdec(new BigDecimal((BigInteger) n1), doubleToDecimal(ff));
		}
		case (512 << 16) + 64: // bigint, double
		{
			double dd = ((Double) n2).doubleValue();
			return op_bdec(new BigDecimal((BigInteger) n1), doubleToDecimal(dd));
		}
		case (32 << 16) + 512: // float, bigint
		{
			float ff = ((Float) n1).floatValue();
			return op_bdec(doubleToDecimal(ff), new BigDecimal((BigInteger) n2));
		}
		case (64 << 16) + 512: // double, bigint
		{
			double dd = ((Double) n1).doubleValue();
			return op_bdec(doubleToDecimal(dd), new BigDecimal((BigInteger) n2));
		}
		case (1 << 16) + 256: // int, decimal
		case (4 << 16) + 256: // byte, decimal
		case (8 << 16) + 256: // short, decimal
		case (16 << 16) + 256: // long, decimal
			return op_bdec(new BigDecimal(((Number) n1).doubleValue()),
					(BigDecimal) n2);

		case (2 << 16) + 256: // char, decimal
			return op_bdec(new BigDecimal((double) ((Character) n1).charValue()),
					(BigDecimal) n2);

		case (32 << 16) + 256: // float, decimal
		{
			float ff = ((Float) n1).floatValue();
			return op_bdec(doubleToDecimal(ff), (BigDecimal) n2);
		}
		case (64 << 16) + 256: // double, decimal
		{
			double dd = ((Double) n1).doubleValue();
			return op_bdec(doubleToDecimal(dd), (BigDecimal) n2);
		}
		case (256 << 16) + 1: // decimal, int
		case (256 << 16) + 4: // decimal, byte
		case (256 << 16) + 8: // decimal, short
		case (256 << 16) + 16: // decimal, long
			return op_bdec((BigDecimal) n1, new BigDecimal(((Number) n2)
					.doubleValue()));

		case (256 << 16) + 2: // decimal, char
			return op_bdec((BigDecimal) n1, new BigDecimal(
					(double) ((Character) n2).charValue()));

		case (256 << 16) + 32: // decimal, float
			float ff = ((Float) n2).floatValue();
			return op_bdec((BigDecimal) n1, doubleToDecimal(ff));
		case (256 << 16) + 64: // decimal, double
			double dd = ((Double) n2).doubleValue();
			return op_bdec((BigDecimal) n1, doubleToDecimal(dd));

		case (256 << 16) + 256: // decimal, decimal
			return op_bdec((BigDecimal) n1, (BigDecimal) n2);

		case (1024 << 16) + 1024: // boolean, boolean
			return op_boolean(((Boolean) n1).booleanValue(), ((Boolean) n2)
					.booleanValue());

		case (128 << 16) + 1: // String, int
		case (128 << 16) + 2: // String, char
		case (128 << 16) + 4: // String, byte
		case (128 << 16) + 8: // String, short
		case (128 << 16) + 16: // String, long
		case (128 << 16) + 32: // String, float
		case (128 << 16) + 64: // String, double
		case (128 << 16) + 128: // String, String
		case (128 << 16) + 256: // String, BigDecimal
		case (128 << 16) + 512: // String, BigInteger
		case (128 << 16) + 1024: // String, boolean
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

	/**
	 * The default implementation of == operator
	 */
	public static class EQ extends BooleanOperator {

		static EQ instance = new EQ();

		public boolean op_int(int i1, int i2) {
			return i1 == i2;
		}

		public boolean op_long(long l1, long l2) {
			return l1 == l2;
		}

		public boolean op_float(float f1, float f2) {
			return f1 == f2;
		}

		public boolean op_double(double d1, double d2) {
			return d1 == d2;
		}

		public boolean op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.compareTo(d2) == 0;
		}

		public boolean op_bint(BigInteger b1, BigInteger b2) {
			return b1.compareTo(b2) == 0;
		}

		public boolean op_boolean(boolean b1, boolean b2) {
			return b1 == b2;
		}

		public boolean op_object(Object n1, Object n2) {
			if (n1 == null) {
				return n2 == null;
			} else if (n2 == null) {
				return false;
			} else if (Runtime.isArray(n1) && Runtime.isArray(n2)) {
				int len = 0;
				if ((len = Runtime.getArrayLength(n2)) ==
				    Runtime.getArrayLength(n1)) 
				{
					for (int i = 0; i < len; i++) {
						Object e1 = Array.get(n1, i);
						Object e2 = Array.get(n2, i);
						if (e1 == null) {
							if (e2 != null) {
								return false;
							}
						} else {
							if (Runtime.isArray(e1)) {
								if (!operateOn(e1, e2)) {
									return false;
								}
							} else if (!e1.equals(e2)) {
								return false;
							}
						}
					}
					return true;
				} else {
					return false;
				}
			} else {
				return n1.equals(n2);
			}
		}

		public boolean op_numeric(Numeric n1, Object n2) {
			if (n1 == null) {
				return n2 == null;
			} else if (n2 == null) {
				return false;
			} else {
				return n1.equals(n2);
			}
		}

		public boolean op_numeric(Object n1, Numeric n2) {
			if (n1 == null) {
				return n2 == null;
			} else if (n2 == null) {
				return false;
			} else {
				return n1.equals(n2);
			}
		}

		public boolean op_string(String s, Object o) {
			return s.equals(o);
		}

		public boolean op_string(Object o, String s) {
			return s.equals(o);
		}
	}

	/**
	 * The default implementation of >= operator
	 */
	public static class GE extends BooleanOperator {

		static GE instance = new GE();

		public boolean op_int(int i1, int i2) {
			return i1 >= i2;
		}

		public boolean op_long(long l1, long l2) {
			return l1 >= l2;
		}

		public boolean op_float(float f1, float f2) {
			return f1 >= f2;
		}

		public boolean op_double(double d1, double d2) {
			return d1 >= d2;
		}

		public boolean op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.compareTo(d2) >= 0;
		}

		public boolean op_bint(BigInteger b1, BigInteger b2) {
			return b1.compareTo(b2) >= 0;
		}

		public boolean op_numeric(Numeric n1, Object n2) {
			return n1.compareTo(n2) >= 0;
		}

		public boolean op_numeric(Object n1, Numeric n2) {
			return n2.compareTo(n1) <= 0;
		}

		public boolean op_string(Object o, String s) {
			if (s != null && o != null) {
				return s.compareTo((String)o) < 0;
			} else {
				return super.op_string(o, s);
			}
		}

		public boolean op_string(String s, Object o) {
			if (s != null && o != null) {
				return s.compareTo((String)o) >= 0;
			} else {
				return super.op_string(s, o);
			}
		}

		public boolean op_object(Object n1, Object n2) {
		    /*
			if (n1 instanceof Comparable) {
				return ((Comparable) n1).compareTo(n2) >= 0;
			} else if (n2 instanceof Comparable) {
				return ((Comparable) n2).compareTo(n1) < 0;
			} else {
				return super.op_object(n1, n2);
			}
		    */
		    return Runtime.compareObjects(n1, n2) >= 0;
		}
	}

	/**
	 * The default implementation of > operator
	 */
	public static class GT extends BooleanOperator {

		static GT instance = new GT();

		public boolean op_int(int i1, int i2) {
			return i1 > i2;
		}

		public boolean op_long(long l1, long l2) {
			return l1 > l2;
		}

		public boolean op_float(float f1, float f2) {
			return f1 > f2;
		}

		public boolean op_double(double d1, double d2) {
			return d1 > d2;
		}

		public boolean op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.compareTo(d2) > 0;
		}

		public boolean op_bint(BigInteger b1, BigInteger b2) {
			return b1.compareTo(b2) > 0;
		}

		public boolean op_numeric(Numeric n1, Object n2) {
			return n1.compareTo(n2) > 0;
		}

		public boolean op_numeric(Object n1, Numeric n2) {
			return n2.compareTo(n1) < 0;
		}

		public boolean op_string(Object o, String s) {
			if (s != null && o != null) {
				return s.compareTo((String)o) < 0;
			} else {
				return super.op_string(o, s);
			}
		}

		public boolean op_string(String s, Object o) {
			if (s != null && o != null) {
				return s.compareTo((String)o) > 0;
			} else {
				return super.op_string(s, o);
			}
		}

		public boolean op_object(Object n1, Object n2) {
		    /*
			if (n1 instanceof Comparable) {
				return ((Comparable) n1).compareTo(n2) > 0;
			} else if (n2 instanceof Comparable) {
				return ((Comparable) n2).compareTo(n1) <= 0;
			} else {
				return super.op_object(n1, n2);
			}
		    */
		    return Runtime.compareObjects(n1, n2) > 0;
		}
	}

	/**
	 * The default implementation of <= operator
	 */
	public static class LE extends BooleanOperator {

		static LE instance = new LE();

		public boolean op_int(int i1, int i2) {
			return i1 <= i2;
		}

		public boolean op_long(long l1, long l2) {
			return l1 <= l2;
		}

		public boolean op_float(float f1, float f2) {
			return f1 <= f2;
		}

		public boolean op_double(double d1, double d2) {
			return d1 <= d2;
		}

		public boolean op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.compareTo(d2) <= 0;
		}

		public boolean op_bint(BigInteger b1, BigInteger b2) {
			return b1.compareTo(b2) <= 0;
		}

		public boolean op_numeric(Numeric n1, Object n2) {
			return n1.compareTo(n2) <= 0;
		}

		public boolean op_numeric(Object n1, Numeric n2) {
			return n2.compareTo(n1) >= 0;
		}

		public boolean op_string(Object o, String s) {
			if (s != null && o != null) {
				return s.compareTo((String)o) >= 0;
			} else {
				return super.op_string(o, s);
			}
		}

		public boolean op_string(String s, Object o) {
			if (s != null && o != null) {
				return s.compareTo((String)o) <= 0;
			} else {
				return super.op_string(s, o);
			}
		}

		public boolean op_object(Object n1, Object n2) {
		    /*
			if (n1 instanceof Comparable) {
				return ((Comparable) n1).compareTo(n2) <= 0;
			} else if (n2 instanceof Comparable) {
				return ((Comparable) n2).compareTo(n1) > 0;
			} else {
				return super.op_object(n1, n2);
			}
		    */
			return Runtime.compareObjects(n1, n2) <= 0;

		}
	}

	/**
	 * The default implementation of < operator
	 */
	public static class LT extends BooleanOperator {

		static LT instance = new LT();

		public boolean op_int(int i1, int i2) {
			return i1 < i2;
		}

		public boolean op_long(long l1, long l2) {
			return l1 < l2;
		}

		public boolean op_float(float f1, float f2) {
			return f1 < f2;
		}

		public boolean op_double(double d1, double d2) {
			return d1 < d2;
		}

		public boolean op_bdec(BigDecimal d1, BigDecimal d2) {
			return d1.compareTo(d2) < 0;
		}

		public boolean op_bint(BigInteger b1, BigInteger b2) {
			return b1.compareTo(b2) < 0;
		}

		public boolean op_numeric(Numeric n1, Object n2) {
			return n1.compareTo(n2) < 0;
		}

		public boolean op_numeric(Object n1, Numeric n2) {
			return n2.compareTo(n1) > 0;
		}

		public boolean op_string(Object o, String s) {
			if (s != null && o != null) {
				return s.compareTo((String)o) > 0;
			} else {
				return super.op_string(o, s);
			}
		}

		public boolean op_string(String s, Object o) {
			if (s != null && o != null) {
				return s.compareTo((String)o) < 0;
			} else {
				return super.op_string(s, o);
			}
		}

		public boolean op_object(Object n1, Object n2) {
		    /*
			if (n1 instanceof Comparable) {
				return ((Comparable) n1).compareTo(n2) < 0;
			} else if (n2 instanceof Comparable) {
				return ((Comparable) n2).compareTo(n1) >= 0;
			} else {
				return super.op_object(n1, n2);
			}
		    */
		    return Runtime.compareObjects(n1, n2) < 0;
		}
	}
}
