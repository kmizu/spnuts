/*
 * @(#)Reference.java 1.3 05/06/21
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

final class Reference {

	/**
	 * Name of the variable
	 */
	String symbol;

	/**
	 * Register number (aload_<index>)
	 * or -1 if the variable is in the class field.
	 */
	int index;

	/**
	 * 0 if the value is stored in an array. (ICONST_<offset>, AALOAD)
	 * Otherwise, -1 (ALOAD <offset>)
	 */
	int offset;

	boolean initialized;
	
	Frame frame;
	
	Reference(String symbol, int index, int offset){
		this(symbol, index, offset, false);
	}

	Reference(String symbol, int index, int offset, boolean initialized){
		this.symbol = symbol;
		this.index = index;
		this.offset = offset;
		this.initialized = initialized;
	}
	
	Reference(String symbol, int index, int offset, boolean initialized, Frame frame){
		this.symbol = symbol;
		this.index = index;
		this.offset = offset;
		this.initialized = initialized;
		this.frame = frame;
	}
	
	Reference(String symbol, LocalInfo info){
		this(symbol, info.map, info.index, info.initialized, info.frame);
	}
	
	void set(ClassFile cf, int tgt){
		if (index >= 0){
			if (offset < 0){
				cf.loadLocal(tgt);
				cf.storeLocal(index);
			} else {
				if (!initialized){
					cf.loadLocal(index);
					Label cont = cf.getLabel();
					cf.add(Opcode.IFNONNULL, cont);
					cf.add(Opcode.ICONST_1);
					cf.add(Opcode.ANEWARRAY, "java.lang.Object");
					cf.storeLocal(index);
					cont.fix();
				}
				cf.loadLocal(index);
				cf.pushInteger(offset);
				cf.loadLocal(tgt);
				cf.add(Opcode.AASTORE);
			}
		} else {
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.GETFIELD, cf.getClassName(), symbol, "[Ljava/lang/Object;");
			cf.pushInteger(offset);
			cf.loadLocal(tgt);
			cf.add(Opcode.AASTORE);
		}
	}

	void get(ClassFile cf, boolean local, int contextIndex){
		if (index >= 0){
			if (offset < 0){
				cf.loadLocal(index);
			} else {
				if (local && !initialized){
					cf.loadLocal(index);
					Label cont = cf.getLabel();
					Label next = cf.getLabel();
					cf.add(Opcode.IFNONNULL, cont);
					cf.loadLocal(contextIndex);
					cf.add(Opcode.LDC, cf.addConstant(symbol));
					cf.add(Opcode.INVOKEVIRTUAL,
						   "pnuts.lang.Context",
						   "getId",
						   "(Ljava/lang/String;)",
						   "Ljava/lang/Object;");
					cf.add(Opcode.GOTO, next);
					cont.fix();

					cf.loadLocal(index);
					cf.pushInteger(offset);
					cf.add(Opcode.AALOAD);
					next.fix();
				} else {
					cf.loadLocal(index);
					cf.pushInteger(offset);
					cf.add(Opcode.AALOAD);
				}
			}
		} else {
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.GETFIELD, cf.getClassName(), symbol, "[Ljava/lang/Object;");
			if (local && !initialized){
				Label next = cf.getLabel();
				Label cont = cf.getLabel();
				cf.add(Opcode.IFNONNULL, cont);
				cf.loadLocal(contextIndex);
				cf.add(Opcode.LDC, cf.addConstant(symbol));
				cf.add(Opcode.INVOKEVIRTUAL,
					   "pnuts.lang.Context",
					   "getId",
					   "(Ljava/lang/String;)",
					   "Ljava/lang/Object;");
				cf.add(Opcode.GOTO, next);
				cont.fix();
				cf.popStack();
				cf.add(Opcode.ALOAD_0);
				cf.add(Opcode.GETFIELD, cf.getClassName(), symbol, "[Ljava/lang/Object;");
				if (offset >= 0){
					cf.pushInteger(offset);
					cf.add(Opcode.AALOAD);
				}
				next.fix();
			} else {
				if (offset >= 0){
					cf.pushInteger(offset);
					cf.add(Opcode.AALOAD);
				}
			}
		}
	}

	/**
	 * Returns a hash code value for the object. 
	 */
	public int hashCode(){
		return ((symbol.hashCode() * 31) + index) * 31 + offset;
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 */
	public boolean equals(Object object){
		if (object instanceof Reference){
			Reference r = (Reference)object;
			if ((symbol == null && symbol == r.symbol ||
				 symbol != null && symbol.equals(r.symbol)) &&
				index == r.index && offset == r.offset){
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a string representation of the object.
	 */
	public String toString(){
		return getClass().getName() + "[" + symbol + "," + index + "," + offset + "," + initialized + "," + frame + "]";
	}
}
