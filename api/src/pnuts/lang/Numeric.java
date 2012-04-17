/*
 * @(#)Numeric.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.lang;

/**
 * In Pnuts, arithmetic operations for objects implements this interface causes
 * a call of the corresponding methods in this interface. See <a
 * href="http://pnuts.org/doc/lang.html#sugar">Pnuts Language Specification </a>
 * for details.
 * 
 * @version 1.1
 * @author Toyokazu Tomatsu
 */
public interface Numeric {
	/**
	 * Adds the value of parameter to itself
	 */
	Object add(Object obj);

	/**
	 * Subtracts the value of parameter from the object
	 */
	Object subtract(Object obj);

	/**
	 * Multiplies itself with the value of parameter
	 */
	Object multiply(Object o);

	/**
	 * Divides itself by the value of parameter
	 */
	Object divide(Object obj);

	/**
	 * Negates itself by the value of parameter
	 */
	Object negate();

	/**
	 * Inverts itself
	 */
	Object inverse();

	/**
	 * Compares the object with the parameter. returns one of the followings:
	 * NOT_EQUAL, LEFT_IS_BIGGER, RIGHT_IS_BIGGER, EQUAL
	 */
	int compareTo(Object o); // >, <, <=, >=, ==, !=

	int NOT_EQUAL = 2;

	int LEFT_IS_BIGGER = 1;

	int RIGHT_IS_BIGGER = -1;

	int EQUAL = 0;
}