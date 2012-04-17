/*
 * DefaultSchemaSetter.java
 */

package org.pnuts.xml;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import pnuts.lang.Context;

/**
 *
 */
public class DefaultSchemaSetter implements Util.SchemaSetter {
	
	/**
	 * Constructor
	 */
	public DefaultSchemaSetter() {
	}
	public void setSchema(Object schema, SAXParserFactory factory, Context context){
		factory.setValidating(true);
	}
	
	public void setSchema(Object schema, SAXParser parser, Context context){
		if (String.valueOf(schema).endsWith(".xsd")){
			try {
				parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
						   "http://www.w3.org/2001/XMLSchema");
				parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", schema);
			} catch (SAXException e){
				// skip
			}
		}
	}

	public void setSchema(Object schema, DocumentBuilderFactory factory, Context context){
		factory.setValidating(true);
		String s = String.valueOf(schema);
		if (s.endsWith(".xsd")){
			try {
				factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
							   "http://www.w3.org/2001/XMLSchema");
				factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", schema);
			} catch (IllegalArgumentException e){
				// skip
			}
		}
	}
}
