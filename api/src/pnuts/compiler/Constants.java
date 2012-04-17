/*
 * @(#)Constants.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

public interface Constants {
	public static final byte CONSTANT_Class = 7;

	public static final byte CONSTANT_Fieldref = 9;

	public static final byte CONSTANT_Methodref = 10;

	public static final byte CONSTANT_InterfaceMethodref = 11;

	public static final byte CONSTANT_String = 8;

	public static final byte CONSTANT_Integer = 3;

	public static final byte CONSTANT_Float = 4;

	public static final byte CONSTANT_Long = 5;

	public static final byte CONSTANT_Double = 6;

	public static final byte CONSTANT_NameAndType = 12;

	public static final byte CONSTANT_Utf8 = 1;

	public static final short ACC_PUBLIC = 0x0001;

	public static final short ACC_PRIVATE = 0x0002;

	public static final short ACC_PROTECTED = 0x0004;

	public static final short ACC_STATIC = 0x0008;

	public static final short ACC_FINAL = 0x0010;

	public static final short ACC_VOLATILE = 0x0040;

	public static final short ACC_TRANSIENT = 0x0080;

	public static final short ACC_SYNCHRONIZED = 0x0020;

	public static final short ACC_NATIVE = 0x0100;

	public static final short ACC_INTERFACE = 0x0200;

	public static final short ACC_ABSTRACT = 0x0400;
}
