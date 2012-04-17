/*
 * @(#)DigestReader.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import org.xml.sax.*;
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;

/**
 * DigestReader is used to retrieve useful information, for the application, from a XML document.
 * <p>
 * XML documents are processed based on user-defined 'rules', which consists of [Action, Path, Key].
 * </p>
 * <BLOCKQUOTE><DL>
 * <DT>Action
 * <DD>A DigestAction object, or the associated name of the action.
 * <DT>Path
 *  <DD>Qualified Names separated by slash (/). Wildcard (*) can be used instead of actual names. 
 *	  When <em>Path</em> starts with '//', it is used to match sub-elements.
 *	   (e.g., /a/b/c, /a/ * /c, //b/c, // *)
 * <DT>Key
 *  <DD>the key to access the intermediate result.  If omitted, the relative path to the
 *	  nearest ancestor node is implicitly defined.
 * </BLOCKQUOTE></DL>
 * <p>
 * Those rules can be defined with the setRules() method or addRule() method.
 * <p>
 * User can define the action names with setAlias() method, passing a Map of String->DigestAction.
 * <p>
 *
 * One of <code>parse()</code> methods processes a XML document based on the rules.
 * <p>An example:
 * <pre>
 *  import pnuts.xml.*;
 *  import pnuts.xml.action.*;
 *
 *  DigestReader dr = new DigestReader();
 *  DigestAction text = new TextAction();
 *  DigestAction list = new ListAction();
 *  DigestAction map = new MapAction();
 *  Object[][] rules = {{text, "/rss/channel/title", "title"},
 *					  {text, "/rss/channel/link", "link"},
 *					  {text, "/rss/channel/description", "description"},
 *					  {list, "/rss/channel/item", "item"},
 *					  {text, "/rss/channel/item/title"},
 *					  {text, "/rss/channel/item/link"},
 *					  {text, "/rss/channel/item/description"}};
 *  dr.setRules(rules);
 *  Map doc = (Map)dr.parse(new FileInputStream("rss.xml"));
 * </pre>
 */
public class DigestReader extends DigestHandler {

	private SAXParser parser;
	private Map aliasMap = new HashMap();
	private EntityResolver entityResolver;

	/**
	 * Constructor
	 */
	public DigestReader(){
		this(getDefaultParser(), null);
	}

	/**
	 * Constructor
	 *
	 * @param defs the rules that consist of  'Action', 'Path', and optional 'Key'.
	 * <DL>
	 * <DT>Action
	 *  <DD>A DigestAction object, or the associated name of the action.
	 * <DT>Path
	 *  <DD>A path identifier, which is a '/'-separated qualified names
	 * <DT>Key
	 *  <DD>the key to access the intermediate result.
	 * </DL>
	 */
	public DigestReader(Object[][] defs){
		this(getDefaultParser(), defs);
	}

	/**
	 * Constructor
	 *
	 * @param parser a SAX parser
	 */
	public DigestReader(SAXParser parser){
		this.parser = parser;
	}

	/**
	 * Constructor
	 *
	 * @param parser a SAX parser
	 * @param defs the rules that consist of  'Action', 'Path', and optional 'Key'.
	 * <DL>
	 * <DT>Action
	 *  <DD>A DigestAction object, or the associated name of the action.
	 * <DT>Path
	 *  <DD>A path identifier, which is a '/'-separated qualified names
	 * <DT>Key
	 *  <DD>the key to access the intermediate result.
	 * </DL>
	 */
	public DigestReader(SAXParser parser, Object[][] defs){
		this.parser = parser;
		if (defs != null){
			setRules(defs);
		}
	}

	/**
	 * Defines the alias map; ActionName -> DigestAction
	 *
	 * @param map the alias map
	 */
	public void setAliases(Map map){
		aliasMap = map;
	}

	/**
	 * Retrieves the alias map
	 *
	 * @return the alias map; ActionName -> DigestAction
	 */
	public Map getAliases(){
		return aliasMap;
	}

	/**
	 * Sets the rules
	 *
	 * @param defs the rules that consist of  'Action', 'Path', and optional 'Key'.
	 * <DL>
	 * <DT>Action
	 *  <DD>A DigestAction object, or the associated name of the action.
	 * <DT>Path
	 *  <DD>A path identifier, which is a '/'-separated qualified names
	 * <DT>Key
	 *  <DD>the key to access the intermediate result.
	 * </DL>
	 */
	public void setRules(Object[][] defs){
		boolean useDefaultRuleSet = false;
		for (int i = 0; i < defs.length; i++){
			String path = (String)defs[i][1];
			if (path.startsWith("//") || path.indexOf("/*") >= 0){
				useDefaultRuleSet = true;
				break;
			}
		}
		if (useDefaultRuleSet){
			setRuleSet(new DefaultRuleSet());
		} else {
			setRuleSet(new SimpleRuleSet());
		}
		for (int i = 0; i < defs.length; i++){
			Object[] tpl = defs[i];
			Object type = tpl[0];
			String path = (String)tpl[1];
			DigestAction action = null;
			if (type instanceof DigestAction){
				action = (DigestAction)type;
			} else {
				action = (DigestAction)aliasMap.get(type);
			}
			if (action != null){
				String key = null;
				if (tpl.length > 2){
					key = (String)tpl[2];
				}
				addRule(action, path, key);
			}
		}
	}

	/**
	 * Processes a XML document with the registered rules, and return the result.
	 *
	 * @param input an input source
	 * @param value an initial value passed to the digest handler
	 * @return the object specified to <em>value</em>
	 */
	public Object parse(InputSource input, Object value) throws org.xml.sax.SAXException, IOException {
		setValue(value);
		if (!parser.isValidating()){
			entityResolver = new EntityResolver(){
					public InputSource resolveEntity(String publicId, String systemId)
						throws org.xml.sax.SAXException
						{
							return new InputSource(new NullInputStream());
						}
				};
		}
		parser.parse(input, this);
		return getValue();
	}

	/**
	 * Processes a XML document with the registered rules, and return the result.
	 *
	 * @param input an input stream
	 * @param value an initial value passed to the digest handler
	 * @return the object specified to <em>value</em>
	 */
	public Object parse(InputStream input, Object value) throws org.xml.sax.SAXException, IOException {
		return parse(new InputSource(input), value);
	}

	/**
	 * Processes a XML document with the registered rules, and return the result.
	 *
	 * @param input an input source
	 * @return a Map object implicitly given.
	 */
	public Object parse(InputSource input) throws org.xml.sax.SAXException, IOException {
		return parse(input, new HashMap());
	}

	/**
	 * Processes a XML document with the registered rules, and return the result.
	 *
	 * @param input an input stream
	 * @return a Map object implicitly given.
	 */
	public Object parse(InputStream input) throws org.xml.sax.SAXException, IOException {
		return parse(new InputSource(input));
	}

	public InputSource resolveEntity(String publicId, String systemId)
		throws org.xml.sax.SAXException
		{
			if (entityResolver != null){
				try {
					return entityResolver.resolveEntity(publicId, systemId);
				} catch (IOException e){
					return null;
				}
			} else {
				return null;
			}
		}

	static SAXParser getDefaultParser(){
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			return factory.newSAXParser();
		} catch (org.xml.sax.SAXException e1){
		} catch (ParserConfigurationException e2){
		}
		return null;
	}

	static class NullInputStream extends InputStream {

		public int read() throws IOException {
			return -1;
		}

		public int read(byte[] buf, int offse, int size) throws IOException {
			return -1;
		}
	}
}
