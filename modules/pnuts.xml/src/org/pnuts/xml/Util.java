/*
 * Util.java
 *
 * Copyright (c) 2004,2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.xml;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.dom.*;
import javax.xml.parsers.*;
import java.util.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import org.pnuts.lib.PathHelper;
import pnuts.lang.*;
import pnuts.beans.*;
import org.pnuts.util.LRUCacheMap;
import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Node;

public class Util {

	private static final String DOCUMENT_BUILDER_KEY = "pnuts$xml$documentBuilderPool".intern();
	private static final String TRANSFORMER_KEY = "pnuts$xml$transformerPool".intern();
	private static final String SAX_PARSER_KEY = "pnuts$xml$saxParserPool".intern();
	private final static String JAVA_ADAPTER = "javaAdapter".intern();
	final static Object KEY_SCHEMA = new Object();

	private static final Set documentBuilderFactoryProperties = new HashSet();
	private static final Set documentBuilderProperties = new HashSet();
	static {
		String[] attributes = {"coalescing",
							   "ignoringComments",
							   "expandEntityReferences",
							   "ignoringElementContentWhitespace",
							   "validating",
							   "namespaceAware"};
		for (int i = 0; i < attributes.length; i++){
			documentBuilderFactoryProperties.add(attributes[i]);
		}
		documentBuilderProperties.add("entityResolver");
		documentBuilderProperties.add("errorHandler");
	}

	private static final Set saxParserFactoryProperties = new HashSet();
	static {
		saxParserFactoryProperties.add("namespaceAware");
		saxParserFactoryProperties.add("validating");
	}

	static SAXParser createSAXParser(Map properties, Context context)
		throws ParserConfigurationException, FactoryConfigurationError, SAXException
		{
			SAXParserFactory factory = SAXParserFactory.newInstance();
			Object schema = null;
			if (properties != null) {
				schema = properties.get(KEY_SCHEMA);
				if (schema != null){
					setSchema(schema, factory, context);
				}
				try {
					BeanUtil.setProperties(factory, properties);
				} catch (Exception e){
					// skip
				}
			}
			SAXParser parser = factory.newSAXParser();
			if (schema != null){
				setSchema(schema, parser, context);
			}
			return parser;
		}
	
	static Transformer getTransformer(Map outputProperties) throws TransformerConfigurationException {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		if (outputProperties != null){
			for (Iterator it = outputProperties.entrySet().iterator(); it.hasNext();){
				Map.Entry entry = (Map.Entry)it.next();
				t.setOutputProperty((String)entry.getKey(), (String)entry.getValue());
			}
		}
		return t;
	}

	static DocumentBuilder createDocumentBuilder(Map properties, Context context) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		HashMap builderProperties = new HashMap();
		if (properties != null) {
			for (Iterator it = properties.entrySet().iterator(); it.hasNext();){
				Map.Entry entry = (Map.Entry)it.next();
				Object key = entry.getKey();
				Object value = entry.getValue();
				if (key == KEY_SCHEMA){
					setSchema(value, factory, context);
				} else if (key instanceof String){
					String prop = (String)key;
					if (documentBuilderFactoryProperties.contains(prop)){
					    try {
						BeanUtil.setProperty(factory, prop, value);
					    } catch (IllegalAccessException ea){
						// skip
					    } catch (InvocationTargetException ite){
						// skip
					    }
					} else if (documentBuilderProperties.contains(prop)){
						builderProperties.put(prop, value);
					} else {
						factory.setAttribute(prop, value);
					}
				}
			}
		}
		DocumentBuilder builder = factory.newDocumentBuilder();
		try {
			BeanUtil.setProperties(builder, builderProperties);
		} catch (Exception e){
			// skip
		}
		return builder;
	}

	static synchronized LRUCacheMap createSAXParserPool(int size, final Context context){
		LRUCacheMap map = (LRUCacheMap)context.get(SAX_PARSER_KEY);
		if (map == null){
			map = new LRUCacheMap(size){
					protected Object construct(Object key){
						try {
							return createSAXParser((Map)key, context);
						} catch (Exception e){
							throw new PnutsException(e, context);
						}
					}
				};
			context.set(SAX_PARSER_KEY, map);
		}
		return map;
	}

	static synchronized LRUCacheMap createTransformerPool(int size, final Context context){
		LRUCacheMap map = (LRUCacheMap)context.get(TRANSFORMER_KEY);
		if (map == null){
			map = new LRUCacheMap(size){
					protected Object construct(Object key){
						try {
							return getTransformer((Map)key);
						} catch (Exception e){
							throw new PnutsException(e, context);
						}
					}
				};
			context.set(TRANSFORMER_KEY, map);
		}
		return map;
	}

	static synchronized LRUCacheMap createDocumentBuilderPool(int size, final Context context){
		LRUCacheMap map = (LRUCacheMap)context.get(DOCUMENT_BUILDER_KEY);
		if (map == null){
			map = new LRUCacheMap(size){
					protected Object construct(Object key){
						try {
							return createDocumentBuilder((Map)key, context);
						} catch (Exception e){
							throw new PnutsException(e, context);
						}
					}
				};
			context.set(DOCUMENT_BUILDER_KEY, map);
		}
		return map;
	}

	static InputSource inputSource(Object in, Context context) throws IOException {
		if (in instanceof InputSource) {
			return (InputSource)in;
		} else if (in instanceof InputStream){
			return new InputSource((InputStream)in);
		} else if (in instanceof Reader){
			return new InputSource((Reader)in);
		} else if (in instanceof URL){
			return new InputSource(((URL)in).toString());
		} else if (in instanceof File){
			return new InputSource(((File)in).toURL().toString());
		} else if (in instanceof String){
			return new InputSource(PathHelper.getFile((String)in, context).toURL().toString());
		} else {
			throw new IllegalArgumentException(String.valueOf(in));
		}
	}

	static ContentHandler contentHandler(Object handler, Context context){
		if (handler instanceof ContentHandler){
			return (ContentHandler)handler;
		} else {
			PnutsFunction javaAdapter = (PnutsFunction)context.resolveSymbol(JAVA_ADAPTER);
			if (javaAdapter != null){
				if (handler == null){
					handler = new HashMap();
				}
				return (ContentHandler)javaAdapter.call(new Object[]{DefaultHandler.class, handler}, context);
			} else {
				throw new PnutsException("pnuts.lib module is missing", context);
			}
		}
	}

	static ErrorHandler errorHandler(Object handler, Context context){
		if (handler instanceof ErrorHandler){
			return (ErrorHandler)handler;
		} else {
			PnutsFunction javaAdapter = (PnutsFunction)context.resolveSymbol(JAVA_ADAPTER);
			if (javaAdapter != null){
				if (handler == null){
					handler = new HashMap();
				}
				return (ErrorHandler)javaAdapter.call(new Object[]{ErrorHandler.class, handler}, context);
			} else {
				throw new PnutsException("pnuts.lib module is missing", context);
			}
		}
	}

	static DocumentBuilder getDocumentBuilder(Map properties, Context context){
		LRUCacheMap map = createDocumentBuilderPool(Constant.DOCUMENT_BUILDER_POOLSIZE, context);
		if (properties == null){
			properties = new HashMap();
		}
		return (DocumentBuilder)map.get(properties);
	}

	static Transformer getTransformer(Map properties, Context context){
		LRUCacheMap map = Util.createTransformerPool(Constant.TRANSFORMER_POOLSIZE, context);
		if (properties == null){
			properties = new HashMap();
		}
		return (Transformer)map.get(properties);
	}

	static SAXParser getSAXParser(Map properties, Context context){
		LRUCacheMap map = Util.createSAXParserPool(Constant.SAX_PARSER_POOLSIZE, context);
		if (properties == null){
			properties = new HashMap();
		}
		return (SAXParser)map.get(properties);
	}

	static DefaultHandler getDefaultErrorHandler(Context context){
		return new DefaultErrorHandler(context);
	}

	static class DefaultErrorHandler extends DefaultHandler {
		PrintWriter stream;

		DefaultErrorHandler(Context context){
			stream = context.getErrorWriter();
		}

		public void warning (SAXParseException exception){
			stream.println(exception);
		}

		public void error (SAXParseException exception) throws SAXException {
			throw exception;
		}

		public void fatalError (SAXParseException exception) throws SAXException {
			throw exception;
		}
	}

	static Result getResult(Object out, Context context) throws IOException {
		if (out instanceof Result){
			return (Result)out;
		} else if (out instanceof Writer){
			return new StreamResult((Writer)out);
		} else if (out instanceof OutputStream){
			return new StreamResult((OutputStream)out);
		} else if (out instanceof File){
			return new StreamResult(new FileOutputStream((File)out));
		} else if (out instanceof String){
			return new StreamResult(new FileOutputStream(PathHelper.getFile((String)out, context)));
		} else if (out instanceof Node){
			return new DOMResult((Node)out);
		} else {
			throw new IllegalArgumentException(String.valueOf(out));
		}
	}

	static Source streamSource(Object in, Context context) throws IOException {
		if (in instanceof Source){
			return (Source)in;
		} else if (in instanceof Reader){
			return new StreamSource((Reader)in);
		} else if (in instanceof InputStream) {
			return new StreamSource((InputStream)in);
		} else if (in instanceof URL){
			return new StreamSource(((URL)in).openStream());
		} else if (in instanceof InputSource){
			return new SAXSource((InputSource)in);
		} else if (in instanceof Node){
			return new DOMSource((Node)in);
		} else if (in instanceof File){
			return new StreamSource(new FileInputStream((File)in));
		} else if (in instanceof String){
			return new StreamSource(new FileInputStream(PathHelper.getFile((String)in, context)));
		} else {
			throw new IllegalArgumentException(String.valueOf(in));
		}
	}

	static Transformer getTransformerXSL(Object xsl, Context context) {
		try {
			return TransformerFactory.newInstance().newTemplates(streamSource(xsl, context)).newTransformer();
		} catch (Exception e){
			throw new PnutsException(e, context);
		}
	}
	
	static SchemaSetter schemaSetter;
	static {
		try {
			Class.forName("javax.xml.validation.Schema");
			schemaSetter = new TigerSchemaSetter();
		} catch (ClassNotFoundException e){
			schemaSetter = new DefaultSchemaSetter();
		}
	}
	
	
	static void setSchema(Object schema, SAXParserFactory factory, Context context){
		schemaSetter.setSchema(schema, factory, context);
	}
	
	static void setSchema(Object schema, DocumentBuilderFactory factory, Context context){
		schemaSetter.setSchema(schema, factory, context);
	}
	
	static void setSchema(Object schema, SAXParser parser, Context context){
		schemaSetter.setSchema(schema, parser, context);
	}
	
	static interface SchemaSetter {
		void setSchema(Object schema, SAXParserFactory factory, Context context);
		void setSchema(Object schema, SAXParser parser, Context context);
		void setSchema(Object schema, DocumentBuilderFactory factory, Context context);
	}
}
