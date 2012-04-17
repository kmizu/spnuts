/*
 * @(#)RuleTarget.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

class RuleTarget {
	DigestAction action;
	String keyword;
	
	RuleTarget(DigestAction action, String keyword){
		this.action = action;
		this.keyword = keyword;
	}
}
