/*
 * @(#)NodeEditingConfiguration.java 1.2 04/12/06
 *
 * Copyright (c) 2003, 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import pnuts.lang.Context;
import pnuts.lang.PnutsFunction;
import pnuts.lang.Configuration;
import org.pnuts.xml.element;
import java.util.*;

public class NodeEditingConfiguration extends XMLConfiguration {

	final static PnutsFunction elementFunc = new element();

	public NodeEditingConfiguration(){
	}

	public NodeEditingConfiguration(Configuration base){
		super(base);
	}
	
	public Object handleUndefinedSymbol(String symbol, Context context){
		return elementFunc.call(new Object[]{symbol}, context);
	}

	public Map createMap(int size, Context context){
		return new LinkedHashMap(size);
	}
}
