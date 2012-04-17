/*
 * @(#)DefaultRuleSet.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import java.util.*;

class DefaultRuleSet implements RuleSet {
	private static Comparator patternNodeComparator = new PatternNodeComparator();

	private PatternNode rootNode = new PatternNode();
	private PatternNode reverseNodes = new PatternNode();
	private int count = 0;

	/*
	 * Registers a rule that does not start with "//"
	 */
	private void addForwardPattern(String pattern, RuleTarget value){
		PatternNode node = rootNode;
		int pos = 1;
		while (true){
			int idx = pattern.indexOf('/', pos);
			if (idx < 0){
				break;
			}
			String s = pattern.substring(pos, idx);
			if (s.equals("*")){
				node.wildcardNode = new PatternNode();
			} else if (!node.containsKey(s)){
				node.put(s, node = new PatternNode());
			} else {
				node = (PatternNode)node.get(s);
			}
			pos = idx + 1;
		}
		String s = pattern.substring(pos);
		if (s.equals("*")){
			node.wildcardNode = node = new PatternNode();
		} else if (!node.containsKey(s)){
			node.put(s, node = new PatternNode());
		} else {
			node = (PatternNode)node.get(s);
		}
		node.value = value;
		node.id = ++count;
	}

	/*
	 * Registers a rule that starts with "//"
	 */
	private void addReversePattern(String pattern, RuleTarget value){
		ArrayList list = new ArrayList();
		int pos = 2;
		while (true){
			int idx = pattern.indexOf('/', pos);
			if (idx < 0){
				break;
			}
			list.add(pattern.substring(pos, idx));
			pos = idx + 1;
		}
		list.add(pattern.substring(pos));
		Collections.reverse(list);

		PatternNode node = reverseNodes;
		for (Iterator it = list.iterator(); it.hasNext(); ){
			String s = (String)it.next();
			if (s.equals("*")){
				node.wildcardNode = node = new PatternNode();
			} else if (!node.containsKey(s)){
				node.put(s, node = new PatternNode());
			} else {
				node = (PatternNode)node.get(s);
			}
		}
		if (node != null){
			node.value = value;
			node.id = ++count;
		}
	}

	/**
	 * Adds a rule
	 *
	 * @param pattern a pattern of a path
	 * @param value the target value
	 */
	public void add(String pattern, DigestAction action, String keyword){
		RuleTarget value = new RuleTarget(action, keyword);
		if (pattern.startsWith("//")){
			addReversePattern(pattern, value);
		} else {
			addForwardPattern(pattern, value);
		}
	}

	private void searchPattern(PatternNode nodes,
							   List paths,
							   int idx,
							   int endIndex,
							   boolean forward,
							   Set list)
		{
			PatternNode n = (PatternNode)nodes.get(paths.get(idx));
			int last = paths.size();
			if (n != null){
				if (n.id != 0){
					list.add(n);
				}
				if (idx != endIndex){
					searchPattern(n, paths, forward ? idx + 1 : idx - 1, endIndex, forward, list);
				}
			}
			n = nodes.wildcardNode;
			if (n != null){
				if (n.id !=  0){
					list.add(n);
				}
				if (idx != endIndex){
					searchPattern(n, paths, forward ? idx + 1 : idx - 1, endIndex, forward, list);
				}
			}
		}

	/**
	 * Searches an actual path
	 *
	 * @param path the actual path
	 * @return A list of target objects
	 */
	public void scan(String path, List paths, TargetHandler handler) throws Exception {
		Set nodeSet = new TreeSet(patternNodeComparator);
		int pathSize = paths.size();
		searchPattern(rootNode, paths, 0, pathSize - 1, true, nodeSet);
		searchPattern(reverseNodes, paths, pathSize - 1, 0, false, nodeSet);
		for (Iterator it = nodeSet.iterator(); it.hasNext();){
			PatternNode node = (PatternNode)it.next();
			if (node.id > 0){
				RuleTarget rule = node.value;
				handler.handle(rule.action, rule.keyword);
			}
		}
	}

	static class PatternNode extends HashMap {
		RuleTarget value;
		int id;
		PatternNode wildcardNode;
	}

	static class PatternNodeComparator implements Comparator {
		public int compare(Object o1, Object o2){
			PatternNode p1 = (PatternNode)o1;
			PatternNode p2 = (PatternNode)o2;
			return p1.id - p2.id;
		}
	}
}
