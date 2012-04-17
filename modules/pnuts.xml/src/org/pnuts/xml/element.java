/*
 * @(#)element.java 1.3 05/01/22
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import pnuts.lang.*;
import java.util.*;
import org.w3c.dom.*;

/*
 * function element(tag) {
 * function (args[]){
 *   function e() e(newDocument())
 *   function e(doc){ 
 *	 elem = doc.createElement(tag)
 *	  for (i: args) {
 *		if (i instanceof String){
 *		  elem.appendChild(doc.createTextNode(i))
 *		} else if (i instanceof java.util.Map){
 *		  for (entry : i) elem.setAttribute(entry.key, entry.value)
 *	      } else if (a instanceof Document){
 *             elem.appendChild(doc.importNode(a.documentElement, true));
 *          } else if (a instanceof Element){
 *             elem.appendChild(doc.importNode(a, true))
 *		} else {
 *		  elem.appendChild(i(doc))
 *		}
 *	  }
 *	  elem
 *	}
 *  }
 *}
 */
public class element extends PnutsFunction {
	final static String NEWDOCUMENT = "newDocument".intern();
	final static Object[] NOARG = new Object[]{};

	public element(){
		super("element");
	}

	public boolean defined(int args){
		return args == 1 || args == 2;
	}

	protected Object exec(Object[] args1, final Context context){
		int nargs = args1.length;
		if (nargs != 1 && nargs != 2){
			undefined(args1, context);
		}
		Object arg;
		String ns = null;
		if (nargs == 2){
			ns = String.valueOf(args1[0]);
			arg = args1[1];
		} else {
			arg = args1[0];
		}
		final String namespaceURI = ns;

		if (arg instanceof String){
			final String tag = (String)arg;
			class E extends PnutsFunction {
				String tag;
				E(String tag){
					this.tag = tag;
				}
				public String toString(){
					return "<" + tag + "/>";
				}

				protected Object exec(final Object[] args2, final Context context){

					class F extends PnutsFunction {
						void dispatch(Document doc, Element elem, Object a, Context context){
							if (a instanceof String){
								elem.appendChild(doc.createTextNode((String)a));
							} else if (a instanceof Map){
								for (Iterator it = ((Map)a).entrySet().iterator(); it.hasNext();){
									Map.Entry entry = (Map.Entry)it.next();
									Object key = entry.getKey();
									String value = (String)entry.getValue();
									if (key instanceof String){
										elem.setAttribute((String)key, value);
									} else {
										String ns = null;
										String attr = null;
										if (key instanceof Object[]){
											Object[] keyArray = (Object[])key;
											if (keyArray.length >= 2){
												ns = String.valueOf(keyArray[0]);
												attr = String.valueOf(keyArray[1]);
											}
										} else if (key instanceof List){
											List keyList = (List)key;
											if (keyList.size() >= 2){
												ns = String.valueOf(keyList.get(0));
												attr = String.valueOf(keyList.get(1));
											}
										}
										if (ns != null && attr != null){
											elem.setAttributeNS(ns, attr, value);
										} else {
											throw new IllegalArgumentException(String.valueOf(key));
										}
									}
								}
							} else if (a instanceof PnutsFunction){
								elem.appendChild((Node)((PnutsFunction)a).call(new Object[]{doc}, context));
							} else if (a instanceof Document){
								Element e = ((Document)a).getDocumentElement();
								elem.appendChild(doc.importNode(e, true));
							} else if (a instanceof Element){
								elem.appendChild(doc.importNode((Element)a, true));
							} else if (a instanceof Node){
								Node n = (Node)a;
								String value = null;
								do {
									value = n.getNodeValue();
									if (value != null){
										break;
									}
									n = n.getFirstChild();
								} while (n != null);
								if (value != null){
									elem.appendChild(doc.createTextNode(value));
								}
							}
						}
						
						protected Object exec(Object[] args3, Context context){
							int nargs = args3.length;
							Document doc;
							if (nargs == 0){
								PnutsFunction newDoc = (PnutsFunction)context.resolveSymbol(NEWDOCUMENT);
								doc = (Document)newDoc.call(NOARG, context);
							} else if (nargs == 1){
								doc = (Document)args3[0];
							} else {
								undefined(args3, context);
								return null;
							}
							Element elem;
							if (namespaceURI == null){
								elem = doc.createElement(tag);
							} else {
								elem = doc.createElementNS(namespaceURI, tag);
							}
							for (int i = 0; i < args2.length; i++){
								dispatch(doc, elem, args2[i], context);
							}
							doc.appendChild(elem);
							return elem;
						}
						public String toString(){
							return String.valueOf(exec(new Object[]{}, context));
						}
					}
					return new F();
				}
			}
			return new E(tag);
		} else {
			throw new IllegalArgumentException(String.valueOf(arg));
		}
	}

	public String toString(){
		return "function element( {namespaceURI,} tag)";
	}
}
