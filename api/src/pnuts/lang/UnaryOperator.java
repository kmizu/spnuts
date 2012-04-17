/*
 * @(#)UnaryOperator.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Abstract base class of unary operations.
 */
public abstract class UnaryOperator implements Serializable {

	/**
	 * operation on an int value
	 */
	protected Object op_int(int i) {
		throw new IllegalArgumentException(String.valueOf(i));
	}

	/**
	 * operation on a long value
	 */
	protected Object op_long(long l) {
		throw new IllegalArgumentException(String.valueOf(l));
	}

	/**
	 * operation on a float value
	 */
	protected Object op_float(float f) {
		throw new IllegalArgumentException(String.valueOf(f));
	}

	/**
	 * operation on a double value
	 */
	protected Object op_double(double d) {
		throw new IllegalArgumentException(String.valueOf(d));
	}

	/**
	 * operation on a BigDecimal
	 */
	protected Object op_bdec(BigDecimal d) {
		throw new IllegalArgumentException(String.valueOf(d));
	}

	/**
	 * operation on a BigInteger
	 */
	protected Object op_bint(BigInteger b) {
		throw new IllegalArgumentException(String.valueOf(b));
	}

	/**
	 * operation on a boolean value
	 */
	protected Object op_boolean(boolean b) {
		throw new IllegalArgumentException(String.valueOf(b));
	}

	/**
	 * operation on a Numeric
	 */
	protected Object op_numeric(Numeric b) {
		throw new IllegalArgumentException(String.valueOf(b));
	}

	/*
	 * Dispatches a unary expression to a particular operation.
	 */
	public Object operateOn(Object n) {
		if (n instanceof Integer) {
			return op_int(((Integer) n).intValue());
		} else if (n instanceof Character) {
			return op_int((int) ((Character) n).charValue());
		} else if (n instanceof Byte) {
			return op_int(((Byte) n).intValue());
		} else if (n instanceof Short) {
			return op_int(((Short) n).intValue());
		} else if (n instanceof Long) {
			return op_long(((Long) n).longValue());
		} else if (n instanceof Float) {
			return op_float(((Float) n).floatValue());
		} else if (n instanceof Double) {
			return op_double(((Double) n).doubleValue());
		} else if (n instanceof BigDecimal) {
			return op_bdec((BigDecimal) n);
		} else if (n instanceof BigInteger) {
			return op_bint((BigInteger) n);
		} else if (n instanceof Boolean) {
			return op_boolean(((Boolean) n).booleanValue());
		} else if (n instanceof Numeric) {
			return op_numeric((Numeric) n);
		} else {
			throw new IllegalArgumentException(String.valueOf(n));
		}
	}

	/**
	 * The default implementation of ++ operator
	 */
	public static class Add1 extends UnaryOperator {
		static Add1 instance = new Add1();

		public Object op_int(int i) {

			if (i == Integer.MAX_VALUE) {
				return new Long((long) i + 1);
			} else {
			    int l = i + 1;
			    if (l >= 0 && l < BinaryOperator.SmallIntSize) {
				return BinaryOperator.smallInt[l];
			    } else {
				return new Integer(l);
			    }
			}
		}

		public Object op_long(long i) {
			if (i == Long.MAX_VALUE) {
				return op_bint(BigInteger.valueOf(i));
			} else {
				return new Long(i + 1);
			}
		}

		public Object op_bint(BigInteger i) {
			return Runtime.compress(i.add(BigInteger.valueOf(1L)));
		}
	}

	/**
	 * The default implementation of -- operator
	 */
	public static class Subtract1 extends UnaryOperator {
		static Subtract1 instance = new Subtract1();

		public Object op_int(int i) {
			if (i > 0 && i <= BinaryOperator.SmallIntSize) {
				return BinaryOperator.smallInt[i - 1];
			} else if (i == Integer.MIN_VALUE) {
				return new Long((long) i - 1);
			} else {
				return new Integer(i - 1);
			}
		}

		public Object op_long(long i) {
			if (i == Long.MIN_VALUE) {
				return op_bint(BigInteger.valueOf(i));
			} else if (i == (long) Integer.MAX_VALUE + 1) {
				return new Integer(Integer.MAX_VALUE);
			} else {
				return new Long(i - 1);
			}
		}

		public Object op_bint(BigInteger i) {
			return Runtime.compress(i.add(BigInteger.valueOf(-1L)));
		}
	}

	/**
	 * The default implementation of unary - operator
	 */
	public static class Negate extends UnaryOperator {

		static Negate instance = new Negate();

		public Object op_int(int n) {
			int l = -n;
			if (l >= 0 && l < BinaryOperator.SmallIntSize) {
				return BinaryOperator.smallInt[l];
			}
			return new Integer(l);
		}

		public Object op_long(long n) {
			return new Long(-n);
		}

		public Object op_float(float n) {
			return new Float(-n);
		}

		public Object op_double(double n) {
			return new Double(-n);
		}

		public Object op_bdec(BigDecimal n) {
			return ((BigDecimal) n).negate();
		}

		public Object op_bint(BigInteger n) {
			return ((BigInteger) n).negate();
		}

		public Object op_numeric(Numeric b) {
			return b.negate();
		}
	}

	/**
	 * The default implementation of ~ operator
	 */
	public static class Not extends UnaryOperator {

		static Not instance = new Not();

		public Object op_int(int n) {
			int l = ~n;
			if (l >= 0 && l < BinaryOperator.SmallIntSize) {
				return BinaryOperator.smallInt[l];
			}
			return new Integer(l);
		}

		public Object op_long(long n) {
			return new Long(~n);
		}

		public Object op_bint(BigInteger n) {
			return n.not();
		}
	}
}
