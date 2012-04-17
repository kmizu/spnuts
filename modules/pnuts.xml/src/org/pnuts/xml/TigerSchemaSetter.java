/*
 * TigerSchemaSetter.java
 */

package org.pnuts.xml;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import pnuts.lang.Context;

/**
 *
 */
public class TigerSchemaSetter implements Util.SchemaSetter {
	static String SCHEMA_DICTIONARY = "schema.properties";
	static Properties dic = new Properties();
	static {
		InputStream in = TigerSchemaSetter.class.getResourceAsStream(SCHEMA_DICTIONARY);
		if (in != null){
			try {
				dic.load(in);
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Constructor
	 */
	public TigerSchemaSetter() {
	}
	
	static Schema getSchema(Object schema, Context context){
		if (schema instanceof Schema){
			return (Schema)schema;
		}
		String str = String.valueOf(schema);
		String ext = getExtension(str);
		String uri = (String)dic.get(ext);
		if (uri != null){
			SchemaFactory factory = SchemaFactory.newInstance(uri);
			try {
				Source source = Util.streamSource(schema, context);
				return factory.newSchema(source);
			} catch (Exception e){
				// skip
			}
		}
		return null;
	}
	
	static String getExtension(String str){
		int idx = str.lastIndexOf('.');
		if (idx >= 0){
			return str.substring(idx + 1);
		} else {
			return "";
		}
	}
	
	public void setSchema(Object schema, SAXParser parser, Context context){
		// nothing
	}
	
	public void setSchema(Object schema, SAXParserFactory factory, Context context){
		Schema s = getSchema(schema, context);
		if (s != null){
			factory.setSchema(s);
		}
	}

	public void setSchema(Object schema, DocumentBuilderFactory factory, Context context){
		Schema s = getSchema(schema, context);
		if (s != null){
			factory.setSchema(s);
		}
	}
}