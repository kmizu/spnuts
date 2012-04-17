/*
 * @(#)ZipWriterHandler.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class is a concrete class of ClassFileHandler.
 * When this is passed to Compiler.compile(..., ClassFileHandler),
 * compiled class files are added to the ZipOutputStream specified with the constructor.
 */
public class ZipWriterHandler implements ClassFileHandler {
	private ZipOutputStream zout;
	private ByteArrayOutputStream bout;
	private DataOutputStream dout;
	private boolean verbose;

	public ZipWriterHandler(ZipOutputStream zout){
		this.zout = zout;
		bout = new ByteArrayOutputStream();
		dout = new DataOutputStream(bout);
	}

	public void setVerbose(boolean flag){
		this.verbose = flag;
	}

	protected void handleException(Exception e){
		e.printStackTrace();
	}

	public Object handle(ClassFile cf) {
		try {
			bout.reset();
			cf.write(dout);
			ZipEntry entry = new ZipEntry(cf.getClassName().replace('.', '/') + ".class");
			zout.putNextEntry(entry);
			bout.writeTo(zout);
			if (verbose){
				System.out.println(entry);
			}
		} catch (IOException e){
			handleException(e);
		}
		return null;
	}
}
