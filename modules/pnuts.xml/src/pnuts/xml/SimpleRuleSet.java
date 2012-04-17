/*
 * @(#)SimpleRuleSet.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import java.util.*;

class SimpleRuleSet implements RuleSet {
	HashMap rules = new HashMap();

	public void add(String pattern, DigestAction action, String keyword){
		rules.put(pattern, new RuleTarget(action, keyword));
	}

	public void scan(String path, List paths, TargetHandler handler) throws Exception {
		RuleTarget target = (RuleTarget)rules.get(path);
		if (target != null){
			handler.handle(target.action, target.keyword);
		}
	}
}
