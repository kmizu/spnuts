/*
 * CompilerPnutsImpl.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import pnuts.lang.Context;
import pnuts.lang.PnutsImpl;
import pnuts.lang.SimpleNode;

/**
 * A subclass of PnutsImpl that always compiles scripts.
 *
 * @see pnuts.lang.PnutsImpl
 * @see pnuts.ext.CachedPnutsImpl
 */
public class CompilerPnutsImpl extends PnutsImpl {
    
    Compiler compiler;
    
    public CompilerPnutsImpl() {
        this.compiler = new Compiler();
        compiler.setConstantFolding(true);
    }
    
    public CompilerPnutsImpl(boolean includeLineNo) {
        compiler.includeLineNo(includeLineNo);
    }
    
    public CompilerPnutsImpl(boolean includeLineNo, boolean useDynamicProxy) {
        this(includeLineNo, includeLineNo, useDynamicProxy);
    }
    
    public CompilerPnutsImpl(boolean includeLineNo,
            boolean includeColumnNo,
            boolean useDynamicProxy) {
	compiler.automatic = true;
	compiler.useDynamicProxy(useDynamicProxy);
        compiler.includeLineNo(includeLineNo);
        compiler.includeColumnNo(includeColumnNo);
        compiler.setConstantFolding(true);
    }
    
    public void setProperty(String key, String value){
        if ("pnuts.compiler.useDynamicProxy".equals(key)){
            compiler.useDynamicProxy("true".equalsIgnoreCase(value));
        } else if ("pnuts.compiler.traceMode".equals(key)){
            compiler.setTraceMode("true".equalsIgnoreCase(value));
        } else if ("pnuts.compiler.optimize".equals(key)){
            compiler.includeLineNo(!"true".equalsIgnoreCase(value));
        }
        super.setProperty(key, value);
    }
    
    public void includeLineNo(boolean flag) {
        compiler.includeLineNo(flag);
    }
    
    public void includeColumnNo(boolean flag) {
        compiler.includeColumnNo(flag);
    }
    
    protected Object acceptNode(SimpleNode node, Context context) {
	return node.accept(compiler, context);
    }
}
