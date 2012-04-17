/*
 * URLRewriteServlet.java
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class URLRewriteServlet extends HttpServlet {
	private final static boolean DEBUG = false;
	private final static String DEFAULT_CONFIGURATION = "url_rewrite.conf";
	
	private String pattern;
	private String replacement;
	private List rewriteRule;
	private List excludedPatterns;
	private boolean verbose;
	private boolean validating;

	public void init() throws ServletException {
		ServletConfig conf = getServletConfig();
		String file = conf.getInitParameter("configuration");
		if (file == null){
			file = DEFAULT_CONFIGURATION;
		}
		String verbose = conf.getInitParameter("verbose");
		if (verbose != null && "true".equals(verbose.toLowerCase())){
			this.verbose = true;
		}
		String validating = conf.getInitParameter("validating");
		if (validating != null && "true".equals(validating.toLowerCase())){
			this.validating = true;
		}		
		readConfiguration(file);
	}
	
       protected void service(HttpServletRequest request,
                               HttpServletResponse response)
                throws ServletException, IOException
       {
	   String queryString = request.getQueryString();
	   String uri = request.getRequestURI();
	   String contextPath = request.getContextPath();
	   if (uri.startsWith(contextPath)){
	       uri = uri.substring(contextPath.length());
	   }
	   if (queryString != null && queryString.length() > 0){
	       uri = uri + "?" + queryString;
	   }
	   String newURI = rewriteURL(uri);
	   request.getRequestDispatcher(newURI).forward(request, response);
       }
	
	void readConfiguration(String configFile){
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL url = cl.getResource(configFile);
		readConfiguration(url);
	}
	
	void readConfiguration(URL url)  {
		try {
			this.rewriteRule = new ArrayList();
			this.excludedPatterns = new ArrayList();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(this.validating);
			DocumentBuilder builder = factory.newDocumentBuilder();	
			builder.setErrorHandler(new ErrorHandler(){
				    public void warning (SAXParseException exception) throws SAXException {
						System.err.println(exception);
				    }
				    public void error (SAXParseException exception) throws SAXException {
						System.err.println(exception);
				    }
				    public void fatalError (SAXParseException exception) throws SAXException {
						System.err.println(exception);
				    }
			});
			Document doc = builder.parse(new InputSource(url.toString()));
			Element elem = doc.getDocumentElement();
			NodeList rules = elem.getElementsByTagName("rewrite-rule");
			int n = rules.getLength();
			for (int i = 0; i < n; i++){
				Element rule = (Element)rules.item(i);
				NodeList pattern = rule.getElementsByTagName("pattern");
				NodeList replacement = rule.getElementsByTagName("replacement");
				
				String patternString = null;
				for (int j = 0; j < pattern.getLength(); j++){
					Node patternNode = pattern.item(j);
					if (patternNode.getNodeType() == Node.ELEMENT_NODE){
						NodeList nodes = patternNode.getChildNodes();
						for (int k = 0; k < nodes.getLength(); k++){
							Node t = nodes.item(k);
							if (t.getNodeType() == Node.TEXT_NODE){
								patternString = t.getNodeValue();
							}
						}
					}
				}
				
				String replacementString = null;
				for (int j = 0; j < replacement.getLength(); j++){
					Node replacementNode = replacement.item(j);
					if (replacementNode.getNodeType() == Node.ELEMENT_NODE){
						NodeList nodes = replacementNode.getChildNodes();
						for (int k = 0; k < nodes.getLength(); k++){
							Node t = nodes.item(k);
							if (t.getNodeType() == Node.TEXT_NODE){
								replacementString = t.getNodeValue();
							}
						}
					}
				}
				if (patternString != null && replacementString != null){
					addRule(patternString, replacementString);
				}
			}
			NodeList patterns = elem.getElementsByTagName("exclude-pattern");
			n = patterns.getLength();
			for (int i = 0; i < n; i++){
				Node node = patterns.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE){
				    NodeList nodes = node.getChildNodes();
				    for (int k = 0; k < nodes.getLength(); k++){
					Node t = nodes.item(k);
					if (t.getNodeType() == Node.TEXT_NODE){
					    String pattern = t.getNodeValue();
					    if (pattern != null){
						pattern = pattern.trim();
						if (pattern.length() > 0){
						    addExcludedPattern(pattern);
						}
					    }
					}
				    }
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	void addRule(String pattern, String replacement){
		if (DEBUG){
			System.err.println("addRule " + pattern  + " ==> " + replacement);
		}
		this.rewriteRule.add(new RewriteRule(Pattern.compile(pattern), replacement));
	}

       void addExcludedPattern(String pattern){
		if (DEBUG){
		    System.err.println("addExcludedPattern " + pattern);
		}
		this.excludedPatterns.add(Pattern.compile(pattern));
       }

	String rewriteURL(String url){
		int n = excludedPatterns.size();
		for (int i = 0; i < n; i++){
		    Pattern pattern = (Pattern)excludedPatterns.get(i);
		    Matcher m = pattern.matcher(url);
		    if (m.find()){
			return url;
		    }
		}
		n = rewriteRule.size();
		for (int i = 0; i < n; i++){
			RewriteRule rule = (RewriteRule)rewriteRule.get(i);
			Pattern pattern = rule.pattern;
			String replacement = rule.replacement;
			String newURL = rewriteURL(url, pattern, replacement);
			if (newURL != null){
				if (verbose){
					System.err.println(url + " => " + newURL);
				}
				return newURL;
			}
		}
		return url;
	}
	
	String rewriteURL(String url, Pattern pattern, String replacement){
		Matcher m = pattern.matcher(url);
		if (m.find()){
			return m.replaceFirst(replacement);
		} else {
			return null;
		}
	}
	
	static class RewriteRule {
		Pattern pattern;
		String replacement;
		
		RewriteRule(Pattern pattern, String replacement){
			this.pattern = pattern;
			this.replacement = replacement;
		}
	}
}
