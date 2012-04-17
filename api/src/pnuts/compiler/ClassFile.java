/*
 * @(#)ClassFile.java 1.6 05/06/27
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a way of making Java class file image.
 */
public class ClassFile {

	private final static long MAGIC = 0xcafebabe0003002dL;

	private final static boolean DEBUG = false;

	/**
	 *  The effect on the operand stack of a given opcode.
	 */
	static final byte[] stackGrowth = {
		0, 	// nop
		1,	// aconst_null
		1,	// iconst_m1
		1,	// iconst_0
		1,	// iconst_1
		1,	// iconst_2
		1,	// iconst_3
		1,	// iconst_4
		1,	// iconst_5
		2,	// lconst_0
		2,	// lconst_1
		1,	// fconst_0
		1,	// fconst_1
		1,	// fconst_2
		2,	// dconst_0
		2,	// dconst_1
		1,	// bipush
		1,	// sipush
		1,	// ldc
		1,	// ldc_W
		2,	// ldc2_W
		1,	// iload
		2,	// lload
		1,	// fload
		2,	// dload
		1,	// aload
		1,	// iload_0
		1,	// iload_1
		1,	// iload_2
		1,	// iload_3
		2,	// lload_0
		2,	// lload_1
		2,	// lload_2
		2,	// lload_3
		1,	// fload_0
		1,	// fload_1
		1,	// fload_2
		1,	// fload_3
		2,	// dload_0
		2,	// dload_1
		2,	// dload_2
		2,	// dload_3
		1,	// aload_0
		1,	// aload_1
		1,	// aload_2
		1,	// aload_3
		-1,	// iaload
		0,	// laload
		-1,	// faload
		0,	// daload
		-1,	// aaload
		-1,	// baload
		-1,	// caload
		-1,	// saload
		-1,	// istore
		-2,	// lstore
		-1,	// fstore
		-2,	// dstore
		-1,	// astore
		-1,	// istore_0
		-1,	// istore_1
		-1,	// istore_2
		-1,	// istore_3
		-2,	// lstore_0
		-2,	// lstore_1
		-2,	// lstore_2
		-2,	// lstore_3
		-1,	// fstore_0
		-1,	// fstore_1
		-1,	// fstore_2
		-1,	// fstore_3
		-2,	// dstore_0
		-2,	// dstore_1
		-2,	// dstore_2
		-2,	// dstore_3
		-1,	// astore_0
		-1,	// astore_1
		-1,	// astore_2
		-1,	// astore_3
		-3,	// iastore
		-4,	// lastore
		-3,	// fastore
		-4,	// dastore
		-3,	// aastore
		-3,	// bastore
		-3,	// castore
		-3,	// sastore
		-1,	// pop
		-2,	// pop2
		1,	// dup
		1,	// dup_X1
		1,	// dup_X2
		2,	// dup2
		2,	// dup2_X1
		2,	// dup2_X2
		0,	// swap
		-1,	// iadd
		-2,	// ladd
		-1,	// fadd
		-2,	// dadd
		-1,	// isub
		-2,	// lsub
		-1,	// fsub
		-2,	// dsub
		-1,	// imul
		-2,	// lmul
		-1,	// fmul
		-2,	// dmul
		-1,	// idiv
		-2,	// ldiv
		-1,	// fdiv
		-2,	// ddiv
		-1,	// irem
		-2,	// lrem
		-1,	// frem
		-2,	// drem
		0,	// ineg
		0,	// lneg
		0,	// fneg
		0,	// dneg
		-1,	// ishl
		-1,	// lshl
		-1,	// ishr
		-1,	// lshr
		-1,	// iushr
		-1,	// lushr
		-1,	// iand
		-2,	// land
		-1,	// ior
		-2,	// lor
		-1,	// ixor
		-2,	// lxor
		0,	// iinc
		1,	// i2l
		0,	// i2f
		1,	// i2d
		-1,	// l2i
		-1,	// l2f
		0,	// l2d
		0,	// f2i
		1,	// f2l
		1,	// f2d
		-1,	// d2i
		0,	// d2l
		-1,	// d2f
		0,	// i2b
		0,	// i2c
		0,	// i2s
		-3,	// lcmp
		-1,	// fcmpl
		-1,	// fcmpg
		-3,	// dcmpl
		-3,	// dcmpg
		-1,	// ifeq
		-1,	// ifne
		-1,	// iflt
		-1,	// ifge
		-1,	// ifgt
		-1,	// ifle
		-2,	// if_icmpeq
		-2,	// if_icmpne
		-2,	// if_icmplt
		-2,	// if_icmpge
		-2,	// if_icmpgt
		-2,	// if_icmple
		-2,	// if_acmpeq
		-2,	// if_acmpne
		0,	// goto
		0,	// jsr
		0,	// ret
		-1,	// tableswitch
		-1,	// lookupswitch
		-1,	// ireturn
		-2,	// lreturn
		-1,	// freturn
		-2,	// dreturn
		-1,	// areturn
		0,	// return
		0,	// getstatic
		0,	// putstatic
		-1,	// getfield
		-1,	// putfield
		-1,	// invokevirtual
		-1,	// invokespecial
		0,	// invokestatic
		-1,	// invokeinterface
		0,	// UNUSED
		1,	// new
		0,	// newarray
		0,	// anewarray
		0,	// arraylength
		-1,	// athrow
		0,	// checkcast
		0,	// instanceof
		-1,	// monitorenter
		-1,	// monitorexit
		0,	// wide
		1,	// multianewarray
		-1,	// ifnull
		-1,	// ifnonnull
		0,	// goto_w
		1,	// jsr_w
		0,	// breakpoint
		1,	// ldc_quick
		1,	// ldc_w_quick
		2,	// ldc2_w_quick
		0,	// getfield_quick
		0,	// putfield_quick
		0,	// getfield2_quick
		0,	// putfield2_quick
		0,	// getstatic_quick
		0,	// putstatic_quick
		0,	// getstatic2_quick
		0,	// putstatic2_quick
		0,	// invokevirtual_quick
		0,	// invokenonvirtual_quick
		0,	// invokesuper_quick
		0,	// invokestatic_quick
		0,	// invokeinterface_quick
		0,	// invokevirtualobject_quick
		0,	// UNUSED
		1,	// new_quick
		1,	// anewarray_quick
		1,	// multianewarray_quick
		-1,	// checkcast_quick
		0,	// instanceof_quick
		0,	// invokevirtual_quick_w
		0,	// getfield_quick_w
		0	// putfield_quick_w
	};	
	
	private ConstantPool constantPool;

	private List methods = new ArrayList(); // MethodInfo

	private MethodInfo currentMethod;

	private List fields = new ArrayList(); // FieldInfo

	private List exceptionTable;

	private List interfaces = new ArrayList();

	private short thisClassIndex;

	private short superClassIndex;

	private short sourceFileNameIndex;

	private short accessFlags;

	private short maxStack;

	private short stackTop;

	private boolean locals[];

	public ClassFile parent;

	private String className;

	private int maxLocal;

	ByteBuffer codeBuffer;

	public ClassFile(String thisClass, String superClass, String sourceFile,
			short accessFlags) {

		constantPool = new ConstantPool();
		thisClassIndex = constantPool.addClass(thisClass);
		superClassIndex = constantPool.addClass(superClass);
		if (sourceFile != null) {
			sourceFileNameIndex = constantPool.addUTF8(sourceFile);
		}
		this.accessFlags = accessFlags;
		this.className = thisClass;
	}

	public String getClassName() {
		return className;
	}

	public void setCodeBuffer(ByteBuffer cbuf){
		this.codeBuffer = cbuf;
	}

	public ByteBuffer getCodeBuffer(){
		return codeBuffer;
	}

	public int codeSize() {
		return codeBuffer.size();
	}

	public void addInterface(String interfaceName) {
		interfaces.add(new Short(constantPool.addClass(interfaceName)));
	}

	public void addField(String fieldName, String type, short accessFlags) {
		fields.add(new FieldInfo(constantPool.addUTF8(fieldName),
				constantPool.addUTF8(type), accessFlags));
	}

	public short addConstant(String value) {
		return constantPool.addConstant(value);
	}

	public void addConstant(String fieldName, String type, short flags,
			short valueIndex) {
		short fieldNameIndex = constantPool.addUTF8(fieldName);
		short typeIndex = constantPool.addUTF8(type);
		short cvAttr[] = new short[4];
		cvAttr[0] = constantPool.addUTF8("ConstantValue");
		cvAttr[1] = 0;
		cvAttr[2] = 2;
		cvAttr[3] = valueIndex;
		fields.add(new FieldInfo(fieldNameIndex, typeIndex, flags,
				cvAttr));
	}

	public void addConstant(String fieldName, String type, short flags,
			int value) {
		addConstant(fieldName, type, flags, constantPool.addConstant(value));
	}

	public void addConstant(String fieldName, String type, short flags,
			long value) {
		addConstant(fieldName, type, flags, constantPool.addConstant(value));
	}

	public void addConstant(String fieldName, String type, short flags,
			double value) {
		addConstant(fieldName, type, flags, constantPool.addConstant(value));
	}

	public void addConstant(String fieldName, String type, short flags,
			String value) {
		addConstant(fieldName, type, flags, constantPool.addConstant(value));
	}

	public void openMethod(String methodName, String type, short flag) {
		openMethod(methodName, type, flag, null);
	}

	public void openMethod(String methodName, String type, short flag,
			String[] exceptions) {
		codeBuffer = new ByteBuffer();
		int sig = sizeOfParameters(type);
		int nlocals = sig & 0xff;
		if ((flag & Constants.ACC_STATIC) != Constants.ACC_STATIC) {
			nlocals++;
		}

		locals = new boolean[nlocals + 32];
		for (int i = 0; i < nlocals; i++) {
			this.locals[i] = true;
		}
		maxLocal = nlocals;

		ExceptionAttr exceptionAttr = null;
		if (exceptions != null) {
			short exceptionAttributeIndex = constantPool.addUTF8("Exceptions");
			short[] e = new short[exceptions.length];
			for (int i = 0; i < exceptions.length; i++) {
				e[i] = constantPool.addClass(exceptions[i]);
			}
			exceptionAttr = new ExceptionAttr(exceptionAttributeIndex, e);
		}
		currentMethod = new MethodInfo(constantPool.addUTF8(methodName),
				constantPool.addUTF8(type), flag, exceptionAttr);
		methods.add(currentMethod);
	}

	public void closeMethod() {
		if ((currentMethod.accessFlags & Constants.ACC_ABSTRACT) == Constants.ACC_ABSTRACT) {
			return;
		}
		int codeSize = codeSize();
		if (DEBUG) {
			System.out.println("codeSize = " + codeSize);
		}
		if (codeSize > 65535) {
			throw new ClassFormatError("code size is too large " + codeSize);
		}
		int exceptionSize = 0;
		if (exceptionTable != null) {
			exceptionSize = exceptionTable.size();
		}
		LineNumberTable lineNumberTable = currentMethod.lineNumberTable;
		int lineNumberSize = 0;
		if (lineNumberTable != null){
			lineNumberSize = 8 + 4 * lineNumberTable.size();
		}

		int attrLength = 2 + // attribute_name_index
				4 + // attribute_length
				2 + // max_stack
				2 + // max_locals
				4 + // code_length
				codeSize + 2 + // exception_table_length
				(exceptionSize * 8) + 2 + // attributes_count
			      lineNumberSize; // LineNumberTable

		ByteBuffer codeAttr = new ByteBuffer(attrLength);
		int codeAttrIndex = constantPool.addUTF8("Code");
		codeAttr.add((short) codeAttrIndex);

		int attr_len = attrLength - 6;
		codeAttr.add(attr_len);
		codeAttr.add((short) maxStack);
		codeAttr.add((short) maxLocal);

		codeAttr.add(codeSize);
		codeAttr.append(codeBuffer);

		if (exceptionSize > 0) {
			codeAttr.add((short) exceptionTable.size());
			for (int i = 0, n = exceptionTable.size(); i < n; i++){
			    ExceptionTableEntry entry = (ExceptionTableEntry)exceptionTable.get(i);
				codeAttr.add(entry.start.getPC());
				codeAttr.add(entry.end.getPC());
				codeAttr.add(entry.handler.getPC());
				codeAttr.add(entry.catchType);
			}
			/*
			for (Enumeration e = exceptionTable.elements(); e.hasMoreElements();) {
				ExceptionTableEntry entry = (ExceptionTableEntry) e
						.nextElement();
				codeAttr.add(entry.start.getPC());
				codeAttr.add(entry.end.getPC());
				codeAttr.add(entry.handler.getPC());
				codeAttr.add(entry.catchType);
			}
			*/
		} else {
			codeAttr.add((short) 0);
		}
		
		if (lineNumberTable != null){
			codeAttr.add((short) 1);
		} else {
			codeAttr.add((short) 0);
		}

		currentMethod.setCodeAttribute(codeAttr);

		exceptionTable = null;
		codeBuffer.setSize(0);
		currentMethod = null;
		maxStack = 0;
		maxLocal = 0;
		stackTop = 0;
	}

	public void shift(int offset) {
		if (exceptionTable != null) {
		    for (int i = 0, n = exceptionTable.size(); i < n; i++){
				ExceptionTableEntry entry = (ExceptionTableEntry)exceptionTable.get(i);
				entry.start = entry.start.shift(offset);
				entry.end = entry.end.shift(offset);
				entry.handler = entry.handler.shift(offset);
		    }
		}
		LineNumberTable lineNumberTable = this.currentMethod.lineNumberTable;
		if (lineNumberTable != null){
			lineNumberTable.shift(offset);
		}
	}

	void updateStack(byte opcode) {
		int idx = opcode & 0xff;
		int growth = stackGrowth[idx];
		stackTop += growth;
		if (stackTop > maxStack) {
			maxStack = stackTop;
		}
	}

	public void popStack() {
		--stackTop;
	}

	public void add(byte opcode) {
		codeBuffer.add(opcode);
		updateStack(opcode);
	}

	public void addByte(byte val) {
		codeBuffer.add(val);
	}

	public void addInt(int ival) {
		codeBuffer.add((byte) ((ival >> 24) & 0xff));
		codeBuffer.add((byte) ((ival >> 16) & 0xff));
		codeBuffer.add((byte) ((ival >> 8) & 0xff));
		codeBuffer.add((byte) ((ival >> 0) & 0xff));
	}

	public Label getLabel() {
		return getLabel(false);
	}

	public Label getLabel(boolean fixed) {
		Label label = new Label(this);
		if (fixed) {
			label.fix();
		}
		return label;
	}

	public int declareLocal() {
		if (maxLocal >= locals.length) {
			boolean[] new_locals = new boolean[locals.length * 2];
			System.arraycopy(locals, 0, new_locals, 0, locals.length);
			locals = new_locals;
		}
		locals[maxLocal] = true;
		return maxLocal++;
	}

	public int getLocal() {
		for (int i = 0; i < maxLocal; i++) {
			if (!locals[i]) {
				locals[i] = true;
				return i;
			}
		}
		if (maxLocal >= locals.length) {
			boolean[] new_locals = new boolean[locals.length * 2];
			System.arraycopy(locals, 0, new_locals, 0, locals.length);
			locals = new_locals;
		}
		locals[maxLocal] = true;
		return maxLocal++;
	}

	public void freeLocal(int index) {
		locals[index] = false;
	}

	public void istoreLocal(int index) {
		if (index >= 0 && index < 4) {
			add((byte) (Opcode.ISTORE_0 + index));
		} else {
			add(Opcode.ISTORE, index);
		}
	}

	public void iloadLocal(int index) {
		if (index >= 0 && index < 4) {
			add((byte) (Opcode.ILOAD_0 + index));
		} else {
			add(Opcode.ILOAD, index);
		}
	}

	public void lloadLocal(int index) {
		if (index >= 0 && index < 4) {
			add((byte) (Opcode.LLOAD_0 + index));
		} else {
			add(Opcode.LLOAD, index);
		}
	}

	public void floadLocal(int index) {
		if (index >= 0 && index < 4) {
			add((byte) (Opcode.FLOAD_0 + index));
		} else {
			add(Opcode.FLOAD, index);
		}
	}

	public void dloadLocal(int index) {
		if (index >= 0 && index < 4) {
			add((byte) (Opcode.DLOAD_0 + index));
		} else {
			add(Opcode.DLOAD, index);
		}
	}

	public void loadLocal(int index) {
		if (index >= 0 && index < 4) {
			add((byte) (Opcode.ALOAD_0 + index));
		} else {
			add(Opcode.ALOAD, index);
		}
	}

	public void storeLocal(int index) {
		if (index >= 0 && index < 4) {
			add((byte) (Opcode.ASTORE_0 + index));
		} else {
			add(Opcode.ASTORE, index);
		}
	}

	public void add(byte opcode, Label label) {
		switch (opcode) {
		case Opcode.GOTO: // fall-through
		case Opcode.IFEQ:
		case Opcode.IFNE:
		case Opcode.IFLT:
		case Opcode.IFGE:
		case Opcode.IFGT:
		case Opcode.IFLE:
		case Opcode.IF_ICMPEQ:
		case Opcode.IF_ICMPNE:
		case Opcode.IF_ICMPLT:
		case Opcode.IF_ICMPGE:
		case Opcode.IF_ICMPGT:
		case Opcode.IF_ICMPLE:
		case Opcode.IF_ACMPEQ:
		case Opcode.IF_ACMPNE:
		case Opcode.JSR:
		case Opcode.IFNULL:
		case Opcode.IFNONNULL:
			int pc = codeBuffer.size();
			codeBuffer.add(opcode);
			label.register(pc, 2);
			int growth = stackGrowth[opcode & 0xff];
			stackTop += growth;
			break;
		}
	}

	public void pushInteger(int number) {
		if (number >= -1 && number <= 5) {
			add((byte) (Opcode.ICONST_0 + number));
		} else if (number > -0x80 && number < 0x80) {
			add(Opcode.BIPUSH, number);
		} else if (number > -0x8000 && number < 0x8000) {
			add(Opcode.SIPUSH, number);
		} else {
			add(Opcode.LDC, constantPool.addConstant(number));
		}
	}

	public void pushLong(long number) {
		if (number == 0) {
			add(Opcode.LCONST_0);
		} else if (number == 1) {
			add(Opcode.LCONST_1);
		} else {
			add(Opcode.LDC2_W, constantPool.addConstant(number));
		}
	}

	public void pushFloat(float number) {
		if (number == 0.0f) {
			add(Opcode.FCONST_0);
		} else if (number == 1.0f) {
			add(Opcode.FCONST_1);
		} else if (number == 2.0f) {
			add(Opcode.FCONST_2);
		} else {
			add(Opcode.LDC_W, constantPool.addConstant(number));
		}
	}

	public void pushDouble(double number) {
		if (number == 0) {
			add(Opcode.DCONST_0);
		} else if (number == 1) {
			add(Opcode.DCONST_1);
		} else {
			add(Opcode.LDC2_W, constantPool.addConstant(number));
		}
	}

	public void pushString(String str){
		int len = str.length();
		if (len > 32767 / 3){
			pushLargeString(str);
		} else {
			add(Opcode.LDC, addConstant(str));
		}
	}

	void pushLargeString(String str){
		add(Opcode.NEW, "java.lang.StringBuffer");
		add(Opcode.DUP);
		add(Opcode.INVOKESPECIAL, "java.lang.StringBuffer", "<init>", "()", "V");
		int x = 32767 / 3;
		int len = str.length();
		int n = len / x;
		int m = len % x;
		for (int i = 0; i < n; i++){
			add(Opcode.LDC, addConstant(str.substring(i * x, (i + 1) * x)));
			add(Opcode.INVOKEVIRTUAL,
			    "java.lang.StringBuffer",
			    "append",
			    "(Ljava/lang/String;)",
			    "Ljava/lang/StringBuffer;");
		}
		if (m > 0){
			add(Opcode.LDC, addConstant(str.substring(n * x)));
			add(Opcode.INVOKEVIRTUAL,
			    "java.lang.StringBuffer",
			    "append",
			    "(Ljava/lang/String;)",
			    "Ljava/lang/StringBuffer;");			
		}
		add(Opcode.INVOKEVIRTUAL, "java.lang.StringBuffer", "toString", "()", "Ljava/lang/String;");
	}

	public void add(byte opcode, int operand) {
		updateStack(opcode);
		switch (opcode) {
		case Opcode.BIPUSH:
			codeBuffer.add(opcode);
			codeBuffer.add((byte) operand);
			break;
		case Opcode.SIPUSH:
			codeBuffer.add(opcode);
			codeBuffer.add((short) operand);
			break;
		case Opcode.NEWARRAY:
			codeBuffer.add(opcode);
			codeBuffer.add((byte) operand);
			break;
		case Opcode.GETFIELD:
		case Opcode.PUTFIELD:
			codeBuffer.add(opcode);
			codeBuffer.add(operand);
			break;
		case Opcode.LDC:
		case Opcode.LDC_W:
		case Opcode.LDC2_W:
			if ((operand >= 256) || (opcode == Opcode.LDC_W)
					|| (opcode == Opcode.LDC2_W)) {

				if (opcode == Opcode.LDC) {
					codeBuffer.add(Opcode.LDC_W);
				} else {
					codeBuffer.add(opcode);
				}
				codeBuffer.add((short) operand);
			} else {
				codeBuffer.add(opcode);
				codeBuffer.add((byte) operand);
			}
			break;
		case Opcode.RET:
		case Opcode.ILOAD:
		case Opcode.LLOAD:
		case Opcode.FLOAD:
		case Opcode.DLOAD:
		case Opcode.ALOAD:
		case Opcode.ISTORE:
		case Opcode.LSTORE:
		case Opcode.FSTORE:
		case Opcode.DSTORE:
		case Opcode.ASTORE:
			if (operand >= 256) {
				codeBuffer.add(Opcode.WIDE);
				codeBuffer.add(opcode);
				codeBuffer.add((short)operand);
			} else {
				codeBuffer.add(opcode);
				codeBuffer.add((byte) operand);
			}
			break;
		default:
			throw new ClassFileException("Unexpected opcode for 1 operand");
		}
	}

	public void add(byte opcode, int operand1, int operand2) {
		updateStack(opcode);
		if (opcode == Opcode.IINC) {
			if ((operand1 > 255) || (operand2 < -128) || (operand2 > 127)) {
				codeBuffer.add(Opcode.WIDE);
				codeBuffer.add(Opcode.IINC);
				codeBuffer.add((short) operand1);
				codeBuffer.add((short) operand2);
			} else {
				codeBuffer.add(Opcode.IINC);
				codeBuffer.add((byte) operand1);
				codeBuffer.add((byte) operand2);
			}
		} else {
			if (opcode == Opcode.MULTIANEWARRAY) {
				codeBuffer.add(Opcode.MULTIANEWARRAY);
				codeBuffer.add((short) operand1);
				codeBuffer.add((byte) operand2);
			} else {
				throw new ClassFileException("Unexpected opcode for 2 operands");
			}
		}
	}

	public void add(byte opcode, String className) {
		updateStack(opcode);
		switch (opcode) {
		case Opcode.NEW:
		case Opcode.ANEWARRAY:
		case Opcode.CHECKCAST:
		case Opcode.INSTANCEOF:
			short classIndex = constantPool.addClass(className);
			codeBuffer.add(opcode);
			codeBuffer.add(classIndex);
			break;
		default:
			throw new ClassFileException("bad opcode for class reference:"
					+ opcode);
		}
	}

	public void add(byte opcode, String className, String fieldName,
			String fieldType) {
		int growth = stackGrowth[opcode & 0xFF];
		stackTop += growth;

		char fieldTypeChar = fieldType.charAt(0);
		int fieldSize = ((fieldTypeChar == 'J') || (fieldTypeChar == 'D')) ? 2
				: 1;

		switch (opcode) {
		case Opcode.GETFIELD:
		case Opcode.GETSTATIC:
			stackTop += fieldSize;
			break;
		case Opcode.PUTSTATIC:
		case Opcode.PUTFIELD:
			stackTop -= fieldSize;
			break;
		default:
			throw new ClassFileException("bad opcode for field reference:"
					+ opcode);
		}
		short fieldRefIndex = constantPool.addFieldRef(className, fieldName,
				fieldType);
		codeBuffer.add(opcode);
		codeBuffer.add(fieldRefIndex);

		if (stackTop > maxStack) {
			maxStack = stackTop;
		}
	}

	public void add(byte opcode, String className, String methodName,
			String parametersType, String returnType) {
		int info = sizeOfParameters(parametersType);
		int arg_size = info & 0xffff;

		int growth = stackGrowth[opcode & 0xFF] - arg_size;
		stackTop += growth;
		/*
		 * if (stackTop > maxStack){ maxStack = stackTop; }
		 */
		switch (opcode) {
		case Opcode.INVOKEVIRTUAL:
		case Opcode.INVOKESPECIAL:
		case Opcode.INVOKESTATIC:
		case Opcode.INVOKEINTERFACE:
			stackTop += sizeOfReturn(returnType);
			codeBuffer.add(opcode);
			if (opcode == Opcode.INVOKEINTERFACE) {
				short ifMethodRefIndex = constantPool.addInterfaceMethodRef(
						className, methodName, parametersType + returnType);
				codeBuffer.add(ifMethodRefIndex);
				codeBuffer.add((byte) (arg_size + 1));
				codeBuffer.add((byte) 0);
			} else {
				short methodRefIndex = constantPool.addMethodRef(className,
						methodName, parametersType + returnType);
				codeBuffer.add(methodRefIndex);
			}
			break;
		default:
			throw new ClassFileException("bad opcode for method reference:"
					+ opcode);
		}

		if (stackTop > maxStack) {
			maxStack = stackTop;
		}
	}

	public void reserveStack(int size) {
		stackTop += size;
		if (stackTop > maxStack) {
			maxStack = stackTop;
		}
	}

	public static final int sizeOfReturn(String sig) {
		int size = 0;
		char c = sig.charAt(0);
		if (c != 'V') {
			if ((c == 'J') || (c == 'D')) {
				size += 2;
			} else {
				size++;
			}
		}
		return size;
	}

	public static final int sizeOfParameters(String sig) {
		return sizeOfParameters(sig.toCharArray(), 1);
	}

	static int sizeOfParameters(char c[], int offset) {
		int index = offset;
		int size = 0;
		int narg = 0;
		loop: while (index < c.length) {
			switch (c[index]) {
			case 'J':
			case 'D':
				size += 2;
				index++;
				narg++;
				break;
			case 'B':
			case 'S':
			case 'C':
			case 'I':
			case 'Z':
			case 'F':
				size++;
				index++;
				narg++;
				break;
			case '[':
				while (c[index] == '[') {
					index++;
				}
				if (c[index] != 'L') {
					size++;
					index++;
					narg++;
					break;
				} // fall through
			case 'L':
				size++;
				narg++;
				while (c[index++] != ';') {
				}
				break;
			case ')':
				break loop;
			default:
				throw new ClassFileException("Bad signature character:"
						+ c[index]);
			}
		}
		return (narg << 16) | size;
	}

	public void addExceptionHandler(Label startLabel, Label endLabel,
			Label handlerLabel, String catchClassName) {
		short catch_type;
		if (catchClassName != null) {
			catch_type = constantPool.addClass(catchClassName);
		} else {
			catch_type = 0;
		}
		ExceptionTableEntry newEntry = new ExceptionTableEntry(startLabel,
				endLabel, handlerLabel, catch_type);

		if (exceptionTable == null) {
			exceptionTable = new ArrayList();
		}
		exceptionTable.add(newEntry);
	}

	public void write(OutputStream stream) throws IOException {
		DataOutputStream out;
		if (stream instanceof DataOutputStream) {
			out = (DataOutputStream) stream;
		} else {
			out = new DataOutputStream(stream);
		}
		short sourceFileIndex = 0;
		if (sourceFileNameIndex != 0) {
			sourceFileIndex = constantPool.addUTF8("SourceFile");
		}
		out.writeLong(MAGIC);
		constantPool.write(out);
		out.writeShort(this.accessFlags);
		out.writeShort(thisClassIndex);
		out.writeShort(superClassIndex);
		out.writeShort(interfaces.size());
		for (int i = 0; i < interfaces.size(); i++) {
			out.writeShort(((Short) interfaces.get(i)).shortValue());
		}
		out.writeShort(fields.size());
		for (int i = 0; i < fields.size(); i++) {
			((FieldInfo) fields.get(i)).write(out);
		}
		out.writeShort(methods.size());
		for (int i = 0; i < methods.size(); i++) {
			((MethodInfo) methods.get(i)).write(out);
		}
		if (sourceFileNameIndex != 0) {
			out.writeShort(1); // attributes count
			out.writeShort(sourceFileIndex);
			out.writeInt(2);
			out.writeShort(sourceFileNameIndex);
		} else {
			out.writeShort(0); // no attributes
		}
	}

	public static String signature(Class[] paramTypes) {
		StringBuffer buf = new StringBuffer();
		buf.append('(');
		for (int i = 0; i < paramTypes.length; i++) {
			buf.append(signature(paramTypes[i]));
		}
		buf.append(')');
		return buf.toString();
	}

	public static String signature(Class clazz) {
		if (clazz == int.class) {
			return "I";
		} else if (clazz == short.class) {
			return "S";
		} else if (clazz == byte.class) {
			return "B";
		} else if (clazz == char.class) {
			return "C";
		} else if (clazz == long.class) {
			return "J";
		} else if (clazz == float.class) {
			return "F";
		} else if (clazz == double.class) {
			return "D";
		} else if (clazz == boolean.class) {
			return "Z";
		} else if (clazz == void.class) {
			return "V";
		} else if (clazz.isArray()) {
			return "[" + signature(clazz.getComponentType());
		} else {
			return "L" + clazz.getName().replace('.', '/') + ";";
		}
	}

	public void addLineNumber(int line){
		LineNumberTable lineNumberTable = this.currentMethod.lineNumberTable;
		if (lineNumberTable == null){
			short idx = constantPool.addUTF8("LineNumberTable");
			lineNumberTable = new LineNumberTable(idx);
			this.currentMethod.lineNumberTable = lineNumberTable;
		}
		lineNumberTable.addLine(codeSize(), line);
	}

	public String toString() {
		return getClass().getName() + "[" + className + "]";
	}

	static class ExceptionAttr {
		private short nameIndex;

		private short[] exceptionIndex;

		ExceptionAttr(short nameIndex, short[] exceptionIndex) {
			this.nameIndex = nameIndex;
			this.exceptionIndex = exceptionIndex;
		}

		void write(DataOutputStream out) throws IOException {
			out.writeShort(nameIndex);
			int n = exceptionIndex.length;
			out.writeInt(n * 2 + 2);
			out.writeShort(n);
			for (int i = 0; i < n; i++) {
				out.writeShort(exceptionIndex[i]);
			}
		}
	}

	static class MethodInfo {

		short nameIndex;

		short descriptorIndex;

		short accessFlags;

		ExceptionAttr exceptions;

		ByteBuffer codeAttribute;
		
		LineNumberTable lineNumberTable;

		MethodInfo(short nameIndex, short descriptorIndex, short accessFlags,
				ExceptionAttr exceptions) {
			this.nameIndex = nameIndex;
			this.descriptorIndex = descriptorIndex;
			this.accessFlags = accessFlags;
			this.exceptions = exceptions;
		}

		ByteBuffer getCodeAttribute() {
			return this.codeAttribute;
		}

		void setCodeAttribute(ByteBuffer codeAttribute) {
			this.codeAttribute = codeAttribute;
		}

		void write(DataOutputStream out) throws IOException {
			out.writeShort(accessFlags);
			out.writeShort(nameIndex);
			out.writeShort(descriptorIndex);
			int count = 0;
			if (codeAttribute != null) {
				count++;
			}
			if (exceptions != null) {
				count++;
			}
			out.writeShort(count);
			if (exceptions != null) {
				exceptions.write(out);
			}
			if (codeAttribute != null) {
				codeAttribute.writeTo(out);
			}
			if (lineNumberTable != null){
				lineNumberTable.write(out);
			}
		}
	}

	static class FieldInfo {

		private short nameIndex;

		private short descriptorIndex;

		private short accessFlags;

		private short attributes[];

		FieldInfo(short nameIndex, short descriptorIndex, short accessFlags) {
			this.nameIndex = nameIndex;
			this.descriptorIndex = descriptorIndex;
			this.accessFlags = accessFlags;
		}

		FieldInfo(short nameIndex, short descriptorIndex, short accessFlags,
				short attributes[]) {
			this.nameIndex = nameIndex;
			this.descriptorIndex = descriptorIndex;
			this.accessFlags = accessFlags;
			this.attributes = attributes;
		}

		void write(DataOutputStream out) throws IOException {
			out.writeShort(accessFlags);
			out.writeShort(nameIndex);
			out.writeShort(descriptorIndex);
			if (attributes == null) {
				out.writeShort(0); // no attributes
			} else {
				out.writeShort(1);
				out.writeShort(attributes[0]);
				out.writeShort(attributes[1]);
				out.writeShort(attributes[2]);
				out.writeShort(attributes[3]);
			}
		}
	}

	static class ExceptionTableEntry {

		Label start;

		Label end;

		Label handler;

		short catchType;

		ExceptionTableEntry(Label start, Label end, Label handler,
				short catchType) {
			this.start = start;
			this.end = end;
			this.handler = handler;
			this.catchType = catchType;
		}
	}

	static class LineNumberTable {
		IntArray pcArray;
		IntArray lineArray;
		short idx;

		LineNumberTable(short attributeNameIndex){
			this.pcArray = new IntArray();
			this.lineArray = new IntArray();
			this.idx = attributeNameIndex;
		}

		public void addLine(int pc, int line){
			pcArray.add(pc);
			lineArray.add(line);
		}

		public int size(){
			return pcArray.size();
		}

		public void shift(int offset){
			int size = pcArray.size();
			int[] pa = pcArray.getArray();
			for (int i = 0; i < size; i++){
				pa[i] += offset;
			}
		}

		void write(DataOutputStream out) throws IOException {
			out.writeShort(idx);
			int size = pcArray.size();
			out.writeInt(2 + 4 * size);
			out.writeShort((short)size);
			int[] pa = pcArray.getArray();
			int[] la = lineArray.getArray();
			for (int i = 0; i < size; i++){
				out.writeShort((short)pa[i]);
				out.writeShort((short)la[i]);
			}
		}
	}
}
