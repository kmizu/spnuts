/*
 * @(#)MD.java 1.2 04/12/06
 *
 * Copyright (c) 2002-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.security;

import pnuts.lang.*;
import java.io.*;
import java.net.URL;
import java.security.*;

/*
 * Base class of Message Digest functions.
 */
public class MD extends PnutsFunction {

	String algorithm;
	String keysym;

	public MD(String name, String algorithm){
		super(name);
		this.algorithm = algorithm;
		this.keysym = ("pnuts.security.md." + algorithm).intern();
	}

	public boolean defined(int nargs){
		return (nargs == 1 || nargs == 3);
	}

	protected Object exec(Object args[], Context context){
		int nargs = args.length;
		MessageDigest md;
		try {
			md = (MessageDigest)context.get(keysym);
			if (md == null){
				md = MessageDigest.getInstance(algorithm);
				context.set(keysym, md);
			}
		} catch (NoSuchAlgorithmException e){
			throw new PnutsException(e, context);
		}
		if (nargs == 1){
			try {
				return MD.digest(md, args[0]);
			} catch (IOException e){
				throw new PnutsException(e, context);
			}
		} else if (nargs == 3){
			byte[] b = (byte[])args[0];
			int offset = ((Integer)args[1]).intValue();
			int size = ((Integer)args[2]).intValue();
			return MD.digest(md, b, offset, size);
		} else {
			undefined(args, context);
			return null;
		}
	}

	public static byte[] digest(MessageDigest md, Object input)
		throws IOException
		{
			md.reset();
			if (input instanceof InputStream){
				InputStream in = (InputStream)input;
				byte[] buf = new byte[512];
				int n;
				while ((n = in.read(buf, 0, 512)) != -1){
					md.update(buf, 0, n);
				}
			} else if (input instanceof File){
				InputStream in = new FileInputStream((File)input);
				byte[] buf = new byte[512];
				int n;
				try {
					while ((n = in.read(buf, 0, 512)) != -1){
						md.update(buf, 0, n);
					}
				} finally {
					in.close();
				}
			} else if (input instanceof URL){
				InputStream in = ((URL)input).openStream();
				byte[] buf = new byte[512];
				int n;
				try {
					while ((n = in.read(buf, 0, 512)) != -1){
						md.update(buf, 0, n);
					}
				} finally {
					in.close();
				}
			} else if (input instanceof String){
				String str = (String)input;
				char[] chars = str.toCharArray();
				int len = chars.length;
				byte[] b = new byte[len * 2];
				for (int i = 0; i < len; i++){
					b[i * 2] = (byte)((chars[i] >> 8) & 0xff);
					b[i * 2 + 1] = (byte)(chars[i] & 0xff);
				}
				md.update(b, 0, b.length);
			} else if (input instanceof byte[]){
				byte[] b = (byte[])input;
				md.update(b, 0, b.length);
			} else {
				throw new IllegalArgumentException();
			}
			return md.digest();
		}

	public static byte[] digest(MessageDigest md,
								byte[] input,
								int offset,
								int size)
		{
			md.reset();
			md.update(input, offset, size);
			return md.digest();
		}
}
