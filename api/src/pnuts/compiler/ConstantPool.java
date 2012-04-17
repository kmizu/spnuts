/*
 * @(#)ConstantPool.java 1.3 05/06/27
 * 
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

class ConstantPool {
	private static final int initialSize = 256;

	private byte constant_pool[] = new byte[initialSize];

	private int top = 0;

	private int index = 1;

	private ConstantSet utfSet = new ConstantSet(32);

	private ConstantSet classSet = new ConstantSet(32);

	private ConstantSet fieldRefSet = new ConstantSet(32);

	private ConstantSet methodRefSet = new ConstantSet(32);

	public short addConstant(int k) {
		ensure(5);
		constant_pool[top++] = Constants.CONSTANT_Integer;
		constant_pool[top++] = (byte) (k >> 24);
		constant_pool[top++] = (byte) (k >> 16);
		constant_pool[top++] = (byte) (k >> 8);
		constant_pool[top++] = (byte) k;
		return (short) index++;
	}

	public short addConstant(long k) {
		ensure(9);
		constant_pool[top++] = Constants.CONSTANT_Long;
		constant_pool[top++] = (byte) (k >> 56);
		constant_pool[top++] = (byte) (k >> 48);
		constant_pool[top++] = (byte) (k >> 40);
		constant_pool[top++] = (byte) (k >> 32);
		constant_pool[top++] = (byte) (k >> 24);
		constant_pool[top++] = (byte) (k >> 16);
		constant_pool[top++] = (byte) (k >> 8);
		constant_pool[top++] = (byte) k;
		short _index = (short) index;
		index += 2;
		return _index;
	}

	short addConstant(float k) {
		ensure(5);
		constant_pool[top++] = Constants.CONSTANT_Float;
		int bits = Float.floatToIntBits(k);
		constant_pool[top++] = (byte) (bits >> 24);
		constant_pool[top++] = (byte) (bits >> 16);
		constant_pool[top++] = (byte) (bits >> 8);
		constant_pool[top++] = (byte) bits;
		return (short) index++;
	}

	public short addConstant(double k) {
		ensure(9);
		constant_pool[top++] = Constants.CONSTANT_Double;
		long bits = Double.doubleToLongBits(k);
		constant_pool[top++] = (byte) (bits >> 56);
		constant_pool[top++] = (byte) (bits >> 48);
		constant_pool[top++] = (byte) (bits >> 40);
		constant_pool[top++] = (byte) (bits >> 32);
		constant_pool[top++] = (byte) (bits >> 24);
		constant_pool[top++] = (byte) (bits >> 16);
		constant_pool[top++] = (byte) (bits >> 8);
		constant_pool[top++] = (byte) bits;
		short _index = (short) index;
		index += 2;
		return _index;
	}

	public short addConstant(String k) {
		Slot slot = utfSet.getSlot(k);
		short[] _index = (short[]) slot.value;
		if (_index == null) {
			_index = new short[] { (short) index++, (short) -1 };
			slot.value = _index;
			try {
				addUTF(k);
			} catch (UTFDataFormatException e) {
				throw new ClassFileException();
			}
		}
		if (_index[1] == -1) {
			_index[1] = (short) index++;
			ensure(3);
			constant_pool[top++] = Constants.CONSTANT_String;
			constant_pool[top++] = (byte) (_index[0] >> 8);
			constant_pool[top++] = (byte) _index[0];
		}
		return _index[1];
	}

	public void addUTF(String str) throws UTFDataFormatException {
		char[] chars = str.toCharArray();
		int strlen = chars.length;
		int utflen = 0;

		for (int i = 0; i < strlen; i++) {
			int c = (int) chars[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}
		if (utflen > 0xFFFF) {
			throw new UTFDataFormatException(String.valueOf(str));
		}
		ensure(utflen + 3);
		constant_pool[top++] = Constants.CONSTANT_Utf8;
		constant_pool[top++] = (byte) ((utflen >>> 8) & 0xFF);
		constant_pool[top++] = (byte) ((utflen >>> 0) & 0xFF);

		for (int i = 0; i < strlen; i++) {
			int c = (int) chars[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				constant_pool[top++] = (byte) c;
			} else if (c > 0x07FF) {
				constant_pool[top++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				constant_pool[top++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				constant_pool[top++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				constant_pool[top++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				constant_pool[top++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}
	}

	public short addUTF8(String str) {
		Slot slot = utfSet.getSlot(str);
		short[] _index = (short[]) slot.value;
		if (_index == null) {
			_index = new short[] { (short) index++, (short) -1 };
			slot.value = _index;
			try {
				addUTF(str);
			} catch (UTFDataFormatException e) {
				throw new ClassFileException();
			}
		}
		return _index[0];
	}

	public short addNameAndType(short nameIndex, short typeIndex) {
		ensure(5);
		constant_pool[top++] = Constants.CONSTANT_NameAndType;
		constant_pool[top++] = (byte) (nameIndex >> 8);
		constant_pool[top++] = (byte) nameIndex;
		constant_pool[top++] = (byte) (typeIndex >> 8);
		constant_pool[top++] = (byte) typeIndex;
		return (short) index++;
	}

	short addClass(short classIndex) {
		Slot slot = classSet.getSlot(new Short(classIndex));
		Short _index = (Short) slot.value;
		if (_index == null) {
			ensure(3);
			constant_pool[top++] = Constants.CONSTANT_Class;
			constant_pool[top++] = (byte) (classIndex >> 8);
			constant_pool[top++] = (byte) (classIndex);
			_index = new Short((short) index++);
			slot.value = _index;
		}
		return _index.shortValue();
	}

	public short addClass(String className) {
		return addClass(addUTF8(className.replace('.', '/')));
	}

	public short addFieldRef(String className, String fieldName,
			String fieldType) {
		Slot slot = fieldRefSet.getSlot(className + " " + fieldName + " "
				+ fieldType);

		Short _index = (Short) slot.value;
		if (_index == null) {
			short nameIndex = addUTF8(fieldName);
			short typeIndex = addUTF8(fieldType);
			short nameAndTypeIndex = addNameAndType(nameIndex, typeIndex);
			short classIndex = addClass(className);
			ensure(5);
			constant_pool[top++] = Constants.CONSTANT_Fieldref;
			constant_pool[top++] = (byte) (classIndex >> 8);
			constant_pool[top++] = (byte) classIndex;
			constant_pool[top++] = (byte) (nameAndTypeIndex >> 8);
			constant_pool[top++] = (byte) nameAndTypeIndex;
			_index = new Short((short) index++);
			slot.value = _index;
		}
		return _index.shortValue();
	}

	public short addMethodRef(String className, String methodName,
			String fieldType) {
		Slot slot = methodRefSet.getSlot(className + " " + methodName + " "
				+ fieldType);
		Short _index = (Short) slot.value;
		if (_index == null) {
			short nameIndex = addUTF8(methodName);
			short typeIndex = addUTF8(fieldType);
			short nameAndTypeIndex = addNameAndType(nameIndex, typeIndex);
			short classIndex = addClass(className);
			ensure(5);
			constant_pool[top++] = Constants.CONSTANT_Methodref;
			constant_pool[top++] = (byte) (classIndex >> 8);
			constant_pool[top++] = (byte) classIndex;
			constant_pool[top++] = (byte) (nameAndTypeIndex >> 8);
			constant_pool[top++] = (byte) nameAndTypeIndex;
			_index = new Short((short) index++);
			slot.value = _index;
		}
		return _index.shortValue();
	}

	short addInterfaceMethodRef(String className, String methodName,
			String methodType) {
		short nameIndex = addUTF8(methodName);
		short typeIndex = addUTF8(methodType);
		short nameAndTypeIndex = addNameAndType(nameIndex, typeIndex);
		short classIndex = addClass(className);
		ensure(5);
		constant_pool[top++] = Constants.CONSTANT_InterfaceMethodref;
		constant_pool[top++] = (byte) (classIndex >> 8);
		constant_pool[top++] = (byte) classIndex;
		constant_pool[top++] = (byte) (nameAndTypeIndex >> 8);
		constant_pool[top++] = (byte) nameAndTypeIndex;
		return (short) index++;
	}

	public void write(ByteBuffer buf) {
		buf.add((short) index);
		buf.add(constant_pool, 0, top);
	}

	public void write(DataOutputStream out) throws IOException {
		out.writeShort((short) index);
		out.write(constant_pool, 0, top);
	}

	final void ensure(int room) {
		if ((top + room) >= constant_pool.length) {
			byte tmp[] = constant_pool;
			constant_pool = new byte[(constant_pool.length * 2) + top + room + 1];
			System.arraycopy(tmp, 0, constant_pool, 0, top);
		}
	}
}
