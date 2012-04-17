/*
 * @(#)ClassFileLoader.java 1.3 05/04/29
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import pnuts.compiler.ClassFile;
import pnuts.compiler.ClassFileHandler;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.PnutsException;

public class ClassFileLoader
    extends ClassLoader
    implements ClassFileHandler
{
	private final static boolean DEBUG = true;
	private static long ID = 0L;

	private Package pkg;
	private Context context;
	private String id;
	private int classes = 0;
	
	public ClassFileLoader(ClassLoader parent){
		super(parent);
		this.id = Long.toHexString(ID++);
	}

	/*
	 * setup for subclass()
	 */
	public void setup(Package pkg, Context context){
		this.pkg = pkg;
		this.context = context;
	}

	public Object handle(ClassFile cf){
		return define(cf);
	}

	protected Class define(ClassFile cf){
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			cf.write(bout);
			byte[] barray = bout.toByteArray();
			return define(cf.getClassName(), barray, 0, barray.length);
		} catch (IOException e){
			e.printStackTrace();
			return null;
		} finally {
			classes++;
		}
	}

	protected Class define(String name, byte[] barray, int offset, int len){
		try {
			Class cls = defineClass(name, barray, offset, len, getClass().getProtectionDomain());
			resolveClass(cls);
			if (pkg != null && context != null){
				try{
					Method m = cls.getMethod("attach", new Class[]{Context.class});
					m.invoke(null, new Object[]{context});
				} catch (Exception e){
				    try {
					Method m = cls.getMethod("attach",
								 new Class[]{Context.class, Package.class});
					m.invoke(null, new Object[]{context, pkg});
				    } catch (Exception e2){
					throw new PnutsException(e2, context);
				    }
				}
			} else if (context != null){
				try{
					Method m = cls.getMethod("attach", new Class[]{Context.class});
					m.invoke(null, new Object[]{context});
				} catch (Exception e3){
				    throw new PnutsException(e3, context);
				}
			}
			return cls;
		} catch (LinkageError e){
			if (DEBUG){
				try {
					String temp = System.getProperty("java.io.tmpdir");
					FileOutputStream out = new FileOutputStream(new File(temp, name + ".class"));
					out.write(barray, offset, len);
					out.close();
				} catch (IOException ioe){
				}
			}
			throw e;
		}
	}

	public int getClassCount(){
		return classes;
	}

	public String getId(){
		return id;
	}
}
