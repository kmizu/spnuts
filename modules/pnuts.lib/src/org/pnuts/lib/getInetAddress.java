/*
 * @(#)getInetAddress.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lib;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.PnutsException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class getInetAddress extends PnutsFunction {

	public getInetAddress(){
		super("getInetAddress");
	}

	public boolean defined(int nargs){
		return nargs == 1 || nargs == 2;
	}
	
	static InetAddress getInetAddress(String hostName, String address)
		throws UnknownHostException
		{
			StringTokenizer st = new StringTokenizer(address, ".");
			if (st.countTokens() == 4){
				try {
					byte[] bytes = new byte[4];
					for (int i = 0; i < 4; i++){
						bytes[i] = (byte)Integer.parseInt(st.nextToken());
					}
					if (hostName != address){
						return InetAddress.getByAddress(hostName, bytes);
					} else {
						return InetAddress.getByAddress(bytes);
					}
				} catch (NumberFormatException num){
					return InetAddress.getByName(hostName);
				}
			} else {
				return InetAddress.getByName(hostName);
			}
		}

	protected Object exec(Object[] args, Context context){
		int nargs = args.length;
		try {
			if (nargs == 1){
				Object hostOrAddr = args[0];
				if (hostOrAddr instanceof InetAddress){
					return (InetAddress)hostOrAddr;
				} else if (hostOrAddr instanceof String){
					String s = (String)hostOrAddr;
					return getInetAddress(s, s);
				} else {
					throw new IllegalArgumentException(String.valueOf(hostOrAddr));
				}
			} else if (nargs == 2){
				String host = (String)args[0];
				String addr = (String)args[1];
				return getInetAddress(host, addr);
			} else {
				undefined(args, context);
				return null;
			}
		} catch (UnknownHostException e){
			throw new PnutsException(e, context);
		}
	}

	public String toString(){
		return "function getInetAddress(host, addr) or (hostOrAddr)";
	}
}
