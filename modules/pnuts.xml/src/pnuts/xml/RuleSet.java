/*
 * @(#)RuleSet.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import java.util.*;

interface RuleSet {

	/**
	 * Add a rule to this rule set
	 *
	 * @param pattern a path pattern
	 * @param target the corresponding structure that consists of the action and the keyword.
	 */
	public void add(String pattern, DigestAction action, String keyword);

	/**
	 * Searches paths that match the registed patterns and executes the action with the key.
	 * 
	 * @param path the path to search
	 * @param paths a List of path elements.
	 * @param handler an object that defines how to handle the matched rule.
	 */
	public void scan(String path, List paths, TargetHandler handler) throws Exception;
}
