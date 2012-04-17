/*
 * @(#)DigestHandler.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import java.util.*;
import java.io.*;

/**
 * <code>DigestHandler</code> provides a base implementation for <code>DigestReader</code> class.
 */
public class DigestHandler extends DefaultHandler {

	private SAXAttributeMap attributeMap;
	private boolean useLocalNames;
	private List stack;
	private StringBuffer sbuf;
	private Object value;

	private List valueStack;
	private List paths;
	private String path;

	Stack listPaths;
	Stack listValues;

	private RuleSet ruleSet;

	/**
	 * Constructor
	 */
	public DigestHandler(){
		this(new SimpleRuleSet());
	}

	public DigestHandler(RuleSet ruleSet){
		this.ruleSet = ruleSet;
	}

	void setRuleSet(RuleSet ruleSet){
		this.ruleSet = ruleSet;
	}

	/**
	 * Changes the setting of which of {qualified | local} names are used in
	 * callback handlers.  When localName is used, the SAX parser should be
	 * namespace aware.
	 *
	 * @param useLocalNames use local names instead of quarified names. 
	 *		If this method is called with true, a Map object that is passed to
	 *		DigestAction.start() method will contain {localName->value} mappings.
	 */
	public void setUseLocalNames(boolean useLocalNames){
		this.useLocalNames = useLocalNames;
	}

	/**
	 * Checks if the current setting uses local names instead of qualified names;
	 *
	 * @return true if the current setting uses local names instead of qualified names. 
	 */
	public boolean isUseLocalNames(){
		return useLocalNames;
	}

	/*
	 * The inital value.
	 */
	public Object getValue(){
		return value;
	}

	/**
	 * Sets the initial value, which is to be modified during the parsing.
	 *
	 * @param value the intial value
	 */
	public void setValue(Object value){
		this.value = value;
	}

	void initialize(){
		path = "";
		stack = new ArrayList();
		sbuf = new StringBuffer(64);
		paths = new ArrayList();
		valueStack = new ArrayList();
		listPaths = new Stack();
		listValues = new Stack();
		pushValue("", value);
		if (useLocalNames){
			attributeMap = new LocalNameMap();
		} else {
			attributeMap = new SAXAttributeMap();
		}
	}

	/**
	 * Adds a rule.
	 *
	 * @param action the action to be invoked at the begining/end of the element.
	 * @param path the path identifier. as a '/'-separated QName list.
	 */
	public void addRule(DigestAction action, String path){
		addRule(action, path, null);
	}

	/**
	 * Adds a rule.
	 *
	 * @param action the action to be invoked at the begining/end of the element.
	 * @param path the path identifier. as a '/'-separated QName list.
	 * @param keyword the key to access the intermediate/final result.
	 */
	public void addRule(DigestAction action, String path, String keyword){
		if (action != null){
			action.handler = this;
			ruleSet.add(path, action, keyword);
		}
	}

	String getKey(String path){
		String base = getStackTopPath();
		if (sameBranch(path, base) && base.length() + 1 < path.length()){
			return path.substring(base.length() + 1);
		} else {
			int idx = path.lastIndexOf('/');
			if (idx > 0){
				return path.substring(idx + 1);
			} else {
				return path;
			}
		}
	}

	static boolean sameBranch(String longer, String shorter){
		int idx = longer.indexOf(shorter);
		if (idx < 0){
			return false;
		}
		int len2 = shorter.length();
		if (longer.length() == len2){
			return true;
		}
		return (longer.charAt(len2) == '/');
	}

	public void startElement (String uri,
							  String localName,
							  String qName,
							  Attributes attributes)
		throws org.xml.sax.SAXException
		{
			stack.add(qName);

			String _path = this.path;
			StringBuffer sb = new StringBuffer(_path);
			sb.append('/');
			sb.append(qName);
			final String path = this.path = sb.toString();

			int pos = paths.size() - 1;
			while (pos > 0 && !sameBranch(path, (String)paths.get(pos))){
				popValue();
				pos--;
			}
			attributeMap.setAttributes(attributes);
			final String defaultKey = getKey(path);
			try {
				ruleSet.scan(path, stack, new TargetHandler(){
						public void handle(DigestAction action, String keyword) throws Exception {
							if (keyword == null){
								keyword = defaultKey;
							}
							action.start(path, keyword, attributeMap, getStackTopValue());
						}
					});
			} catch (Exception e){
				throw new SAXException(e);
			}
		}

	public void endElement (String uri, String localName, String qName)
		throws org.xml.sax.SAXException
		{
			final String path = this.path;
			String valuePath = getStackTopPath();

			int pos = paths.size() - 1;
			while (pos > 0 && !sameBranch(path, valuePath)){
				popValue();
				pos--;
				valuePath = getStackTopPath();
				break;
			}

			final String defaultKey = getKey(path);
			final String text = sbuf.toString().trim();
			try {
				ruleSet.scan(path, stack, new TargetHandler(){
						public void handle(DigestAction action, String keyword) throws Exception {
							if (keyword == null){
								keyword = defaultKey;
							}
							action.end(path, keyword, text, getStackTopValue());
						}
					});
			} catch (Exception e){
				throw new SAXException(e);
			}
			sbuf.setLength(0);

			stack.remove(stack.size() - 1);
			int idx = path.lastIndexOf('/');
			if (idx > 0){
				this.path = path.substring(0, idx);
			} else {
				this.path = "";
			}
		}

	Object getStackTopValue(){
		return valueStack.get(valueStack.size() - 1);
	}

	String getStackTopPath(){
		return (String)paths.get(paths.size() - 1);
	}

	void pushValue(String path, Object value){
		paths.add(path);
		valueStack.add(value);
	}

	Object popValue(){
		paths.remove(paths.size() - 1);
		Object value = valueStack.remove(valueStack.size() - 1);
		if (!listValues.isEmpty() && value == listValues.peek()){
			listPaths.pop();
			listValues.pop();
		}
		return value;
	}

	/**
	 * Registers <em>list</em> for the specified <em>path</em>.
	 * The registered <em>list</em> is unregistered when different branch from the one
	 * the list is registered with, or an element of parent path is found by the parser.
	 */
	public synchronized void registerListPath(String path, Object list){
		listPaths.push(path);
		listValues.push(list);
	}

	/**
	 * Checks if the list registered with <em>path</em> is still managed by the DigestReader.
	 *
	 * @param path the path
	 * @return true if it is still managed by the DigestReader.
	 */
	public boolean listAlive(String path){
		return listPaths.size() > 0 && listPaths.peek().equals(path);
	}

	/**
	 * Returns the most recent managed list.
	 *
	 * @return the list object
	 */
	public Object currentListValue(){
		return listValues.peek();
	}

	public void characters (char ch[], int start, int length)
		throws org.xml.sax.SAXException
		{
			sbuf.append(ch, start, length);
		}

	public void startDocument() throws org.xml.sax.SAXException {
		initialize();
	}
}
