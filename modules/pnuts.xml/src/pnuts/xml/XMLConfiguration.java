/*
 * XMLConfiguration.java
 *
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.xml;

import pnuts.lang.Context;
import pnuts.lang.Configuration;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Element;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.pnuts.lib.AggregateConfiguration;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;


/**
 * This class provides an easier way to access NodeList.
 *
 * (1) Index access to NodeList.
 *	  e.g. nodeList[i]
 *
 * (2) Iterate NodeList with for/foreach statement
 *	 e.g. for (i : nodeList) ...
 *
 * (3) Attributes
 *	 e.g. node["attributeName"]
 *
 * (4) Navigation
 *	 e.g. document.project.target.name
 */
public class XMLConfiguration extends AggregateConfiguration {

	private final static String TEXT = "text".intern();
	private final static String TEXT_TRIM = "textTrim".intern();
	private final static String GET_CHILDREN = "getChildren".intern();
	private final static String GET_CHILD = "getChild".intern();
	private final static String ATTRIBUTES = "attributes".intern();

	public XMLConfiguration(){
	}

	public XMLConfiguration(Configuration base){
		super(base);
	}

	static class NodeListEnum implements Enumeration {
		NodeList nodeList;
		int idx;
		int len;

		NodeListEnum(NodeList nodeList){
			this.nodeList = nodeList;
			this.idx = 0;
			this.len = nodeList.getLength();
		}

		public boolean hasMoreElements(){
			return idx < len;
		}

		public Object nextElement(){
			return nodeList.item(idx++);
		}
	}

	static List findNodes(Node node, String name){
		NodeList nodeList = node.getChildNodes();
		ArrayList lst = new ArrayList();
		for (int i = 0; i < nodeList.getLength(); i++){
			Node n = nodeList.item(i);
			if (n.getNodeName().equals(name)){
				lst.add(n);
			}
		}
		return lst;
	}

	static String getText(Node node){
		NodeList nodeList = node.getChildNodes();
		int len = nodeList.getLength();
		for (int i = 0; i < len; i++){
			Node n = nodeList.item(i);
			if (n instanceof Text){
				return n.getNodeValue();
			}
		}
		return "";
	}

	static Element getElement(NodeList nodeList, String skey){
		int len = nodeList.getLength();
		for (int i = 0; i < len; i++){
			Node n = nodeList.item(i);
			if (n instanceof Element){
				Element e = (Element)n;
				if (e.getTagName().equals(skey)){
					return e;
				}
			}
		}
		return null;
	}

	static Element getElement(NodeList nodeList, int idx){
		int len = nodeList.getLength();
		int count = 0;
		for (int i = 0; i < len; i++){
			Node n = nodeList.item(i);
			if (n instanceof Element){
				if (idx == count){
					return (Element)n;
				}
				count++;
			}
		}
		return null;
	}

	public Object getElement(Context context, Object target, Object key){
		String skey = null;
		int idx = 0;

		if (key instanceof Number){
			idx = ((Number)key).intValue();
		} else if (key instanceof String){
			skey = (String)key;
		} else {
			return super.getElement(context, target, key);
		}
		if ((target instanceof Element) && skey != null){
			if (skey.charAt(0) == '@'){
				return ((Element)target).getAttribute(skey.substring(1));
			} else {
		   	return getElement(((Element)target).getChildNodes(), skey);
			}
		} else if (target instanceof NodeList){
			NodeList nodeList = (NodeList)target;
			if (skey == null){
				return getElement(nodeList, idx);
			} else {
				return getElement(nodeList, skey);
			}
		} else if (target instanceof NamedNodeMap){
			NamedNodeMap nodeMap = (NamedNodeMap)target;
			if (skey != null){
				return nodeMap.getNamedItem(skey);
			} else {
				return nodeMap.item(idx);
			}
		}
		return super.getElement(context, target, key);
	}

	public void setElement(Context context, Object target, Object key, Object value){
		String skey = null;
		if (key instanceof String){
			skey = (String)key;
			if (target instanceof Element){
				if (skey.charAt(0) == '@') skey = skey.substring(1);
				((Element)target).setAttribute(skey, (String)value);
				return;
			}
		} 
		super.setElement(context, target, key, value);
	}

	public Object getField(Context context, Object target, String name){
		if (target instanceof Node){
			if (name == TEXT){
				return getText((Node)target);
			} else if (name == TEXT_TRIM){
				return getText((Node)target).trim();
			} else if (name == ATTRIBUTES){
				NamedNodeMap nodeMap = ((Node)target).getAttributes();
				Element element = null;
				if (target instanceof Element){
					element = (Element)target;
				}
				return new DOMAttributeMap(element, nodeMap);
			}
			List lst = findNodes((Node)target, name);
			int size = lst.size();
			if (size == 0) {
				if (name.charAt(0) == '@' && target instanceof Element){
					return ((Element)target).getAttribute(name.substring(1));
				}
				return null;
			} else {
				return lst;
			}
		} else if (target instanceof NamedNodeMap){
			NamedNodeMap m = (NamedNodeMap)target;
			return m.getNamedItem(name);
		} else {
			return super.getField(context, target, name);
		}
	}

	public void putField(Context context, Object target, String name, Object value){
		if (target instanceof Element){
			if (name == TEXT){
				if (value instanceof String){
					((Element)target).setTextContent((String)value);
				}
			} else if (name == ATTRIBUTES){
				if (value instanceof Map){
					Element element = (Element)target;
					NamedNodeMap m = element.getAttributes();
					int len = m.getLength();
					for (int i = 0; i < len; i++){
						Attr attr = (Attr)m.item(i);
						element.removeAttributeNode(attr);
					}
					for (Iterator it = ((Map)value).entrySet().iterator(); it.hasNext();){
						Map.Entry entry = (Map.Entry)it.next();
						element.setAttribute((String)entry.getKey(), (String)entry.getValue());
					}
				}
			} else if (name.charAt(0) == '@' && value instanceof String) {
				((Element)target).setAttribute(name.substring(1), (String)value);
			} else {
				super.putField(context, target, name, value);
			}
		} else {
			super.putField(context, target, name, value);
		}
	}

	public Object callMethod(Context context, Class c, String name,
			Object args[], Class types[], Object target)
	{
		if (target instanceof Node){
			Node node = (Node)target;
			if (name == GET_CHILD && args.length == 1){
				String tag = (String)args[0];
				NodeList nodeList = node.getChildNodes();
				int len = nodeList.getLength();
				for (int i = 0; i < len; i++){
					Node n = nodeList.item(i);
					if (n instanceof Element){
						if (((Element)n).getTagName().equals(tag)){
							return n;
						}
					}
				}
				return null;
			} else if (name == GET_CHILDREN){
				List list = new ArrayList();
				NodeList nodeList = node.getChildNodes();
				if (args.length == 1){
					String tag = (String)args[0];
					for (int i = 0; i < nodeList.getLength(); i++){
						Node n = nodeList.item(i);
						if (n instanceof Element){
							if (((Element)n).getTagName().equals(tag)){
								list.add(n);
							}
						}
					}
				} else if (args.length == 0){
					for (int i = 0; i < nodeList.getLength(); i++){
						Node n = nodeList.item(i);
						if (n instanceof Element){
							list.add(n);
						}
					}
				}
				return list;
			}
		}
		return super.callMethod(context, c, name, args, types, target);
	}	

	public Enumeration toEnumeration(Object object){
		if (object instanceof NodeList){
			return new NodeListEnum((NodeList)object);
		} else {
			return super.toEnumeration(object);
		}
	}
}
