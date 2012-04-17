/*
 * DynamicPage.java
 *
 * Copyright (c) 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import pnuts.lang.Context;
import pnuts.lang.Pnuts;
import pnuts.lang.Runtime;
import pnuts.lang.ParseException;
import pnuts.lang.PnutsException;
import pnuts.ext.CachedScript;
import pnuts.compiler.Compiler;
import java.io.*;
import java.util.StringTokenizer;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import org.pnuts.servlet.protocol.pea.Handler;

/**
 * A class responsible for dynamic page generation.
 */
public class DynamicPage extends CachedScript {

	private final static int BUFFER_SIZE = 4096;
	private final static String PRINT = "\nprint(";
	private final static String EOL= "\")\n";

	public static void convert(File infile, File outfile, String enc, Context context)
		throws IOException
		{
			Reader in = null;
			Writer out = null;

			try {
				try {
					if (enc == null){
						enc = context.getScriptEncoding();
					}
					if (enc != null){
						in = new InputStreamReader(new FileInputStream(infile), enc);
						out = new OutputStreamWriter(new FileOutputStream(outfile), enc);
					} else {
						in = new FileReader(infile);
						out = new FileWriter(outfile);
					}
					convert(in,
						out,
						infile.getAbsoluteFile().getParentFile().toURL(),
						enc,
						context);
				} finally {
					if (in != null){
						in.close();
					}
				}
			} finally {
				if (out != null){
					out.close();
				}
			}
		}
	public static void convert(Reader input, Writer output, URL baseloc, String enc, Context context)
		throws IOException
        {
            convert(input, output, baseloc, enc, context,  null);
        }
        
	public static void convert(Reader input, Writer output, URL baseloc, String enc, Context context, Set scriptURLs)
		throws IOException
	{
            convert(input, output, baseloc, enc, context, scriptURLs, new boolean[1]);
	}


	static void convert(Reader input, Writer output, URL baseloc, String enc, Context context, Set scriptURLs, boolean[] escape)
		throws IOException
		{
			char[] buf = new char[BUFFER_SIZE];
			int state = 0; // 0==outside, 1==<, 2==<%, 3==<%?, 4==%
			int type = -1; // 0=="<%-", 1=="<% ", 2=="<%=", 3=="<%@"
			boolean printing = false;
			CharArrayWriter s1 = new CharArrayWriter();
			CharArrayWriter s2 = new CharArrayWriter();
			int p1 = -1;  // beginning of outer text
			int p2 = -1;
			boolean quote = false;

			int n;
			while ((n = input.read(buf)) != -1){
				p1 = 0;
				for (int i = 0; i < n; i++){
					char c = (char)buf[i];
					switch (state){
					case 0: // outside
						if (c == '<'){
							state = 1;
						}
						break;
					case 1: // <
						if (c == '%'){
							state = 2;
							if (i - p1 >= 0){
								if (!printing){
									output.write(PRINT);
									output.write('"');
									printing = true;
								} else {
									if (quote){
										output.write('"');
									}
									output.write(',');
									output.write('"');
								}
								quote = true;
								if (s1.size() > 0){
								    if (i - p1 > 1){
									s1.write(buf, p1, i - 1 - p1);
								    }
								    char[] ca = s1.toCharArray();
								    if (i == 0){
									encode(output, ca, 0, ca.length - 1);
								    } else {
									encode(output, ca);
								    }
									s1.reset();
								} else {
								    if (i - p1 > 1){
									encode(output, buf, p1, i - 1 - p1);
								    }
								}
							}
							continue;
						} else {
							state = 0;
						}
						break;
					case 2:
						if (c == '-'){
							type = 0;
							p2 = i + 1;
						} else if (c == '='){
							type = 2;
							p2 = i + 1;
						} else if (c == '@'){
							type = 3;
							p2 = i + 1;
						} else {
							type = 1;
							p2 = i;
						}
						state = 3;
						break;
					case 3:
						if (c == '%'){
							state = 4;
						}
						break;
					case 4:
						if (c == '>'){
							state = 0;
							if (i - p2 >= 1){
							    if (i - p2 > 1){
								s2.write(buf, p2, i - 1 - p2);
							    }
								if (type == 2){  // "<%="
									if (!printing){
										output.write(PRINT);
										printing = true;
									} else {
										if (quote){
											output.write('"');
										}
										output.write(',');
									}
									quote = false;
									if (escape[0]){
									    output.write("escape(");
									}
									s2.writeTo(output);
									if (escape[0]){
									    output.write(")");
									}
								} else if (type == 1){   // "<% "
									if (printing){
										output.write(EOL);
										printing = false;
									}
									s2.writeTo(output);
								} else if (type == 3){   // "<%@"
									if (printing){
										output.write(EOL);
										printing = false;
									}
									char[] b = s2.toCharArray();
									executeDirective(b, output, baseloc, enc, context, scriptURLs, escape);
									output.write('\n');
								} else if (type == 0){  // "<%-"
									if (printing){
										output.write(EOL);
										printing = false;
									}
								}
								s2.reset();
							}
							p1 = i + 1;
							p2 = -1;
							type = -1;
						} else {
							state = 3;
						}
						break;
					} // switch (state)
				}	 // for

				if ((state == 0 || state == 1) && p1 >= 0){
					s1.write(buf, p1, n - p1);
					p1 = 0;
				} else if ((state == 2 || state == 3) && p2 >= 0){
					s2.write(buf, p2, n - p2);
					p2 = 0;
				} else if ((state == 4) && p2 >= 0){
					s2.write(buf, p2, n - p2 - 1);
					p2 = 0;
				}
			}
			if (state == 0 || state == 1){
				if (s1.size() > 0){
					if (!printing){
						output.write(PRINT);
						output.write('"');
						printing = true;
					} else {
						if (quote){
							output.write('"');
						}
						output.write(',');
						output.write('"');
					}
					quote = true;
					encode(output, s1.toCharArray());
				}
				if (printing){
					if (quote){
						output.write('"');
					}
					output.write(')');
				}
			} else if (s2.size() > 0){
				if (printing){
					output.write(EOL);
				}
				s2.writeTo(output);
				if (quote){
					output.write('"');
				}
				output.write(')');
			}
		}

	/*
	 * <%@ include file=""%>
	 * <%@ include expr=""%>
	 */
	static void executeDirective(char[] b, Writer output, URL baseloc, String enc, Context context, Set scriptURLs, boolean[] escape)
		throws IOException
		{
			String s = new String(b);
			StringTokenizer st = new StringTokenizer(s, " ");
			String s1 = st.nextToken();
			String s2 = null;
			if (st.hasMoreTokens()){
			    s2 = st.nextToken();
			}
			if ("include".equals(s1)){
				int idx = s2.indexOf('=');
				if (idx < 0){
					throw new RuntimeException("corrupted directive");
				}
				String attr = s2.substring(0, idx).trim();
				String value;
				int len = s2.length();
				if (s2.charAt(idx + 1) == '"' && s2.charAt(len - 1) == '"'){
					value = s2.substring(6, len - 1);
				} else {
					value = s2.substring(5);
				}
				if ("file".equals(attr)){
					if (baseloc != null){
						URL url = new URL(baseloc, value);
                                                if (scriptURLs != null){
                                                    scriptURLs.add(url);
                                                }
						Reader reader;
						if (enc != null){
							reader = new InputStreamReader(url.openStream(), enc);
						} else {
							reader = Runtime.getScriptReader(url.openStream(), context);
						}
						convert(reader, output, baseloc, enc, context, scriptURLs, escape);
					} else {
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
                                                URL url = cl.getResource(value);
                                                if (scriptURLs != null && url != null){
                                                    scriptURLs.add(url);
                                                }
						InputStream in = null;
                                                try {
                                                    if (url != null){
                                                        in = url.openStream();
                                                    }
                                                } catch (IOException e) {
                                                    // ignore
                                                }
						Reader reader;
						if (in != null){
							if (enc != null){
								reader = new InputStreamReader(in, enc);
							} else {
								reader = Runtime.getScriptReader(in, context);
							}
							convert(reader, output, null, enc, context, scriptURLs, escape);
						} else {
							throw new FileNotFoundException(value);
						}
					}
				} else if ("expr".equals(attr)){
					String v = String.valueOf(Pnuts.eval(value, context));
					output.write(PRINT);
					output.write('"');
					encode(output, v.toCharArray());
					output.write(EOL);
				} else {
					throw new RuntimeException("unsupported include type");
				}
			} else if ("escape".equals(s1)){  /* <%@escape%> */
			    escape[0] = true;
			} else if ("no-escape".equals(s1)){ /* <%@no-escape%> */
			    escape[0] = false;
			} else {
				throw new RuntimeException("not supported directive");
			}
		}

	static void encode(Writer output, char[] buf) throws IOException {
		encode(output, buf, 0, buf.length);
	}

	static void encode(Writer output, char[] src, int offset, int size)
		throws IOException
	{
		char[] buf = new char[512];
		int bufsize = buf.length;
		int pos = 0;

		for (int i = offset; i < offset + size; i++){
			if (pos + 2 > bufsize){
				output.write(buf, 0, pos);
				pos = 0;
			}
			int c = src[i];
			switch (c) {
			case '\\': buf[pos++] = '\\'; buf[pos++] = '\\'; break;
			case '"':  buf[pos++] = '\\'; buf[pos++] = '"'; break;
			default:
				buf[pos++] = (char)c;
			}
		}
		if (pos > 0){
			output.write(buf, 0, pos);
		}
	}

	private Set scriptURLs;
	private boolean checkUpdates;

	public DynamicPage(URL scriptURL, String encoding, Context context)
		throws IOException, ParseException 
	{
		this(scriptURL, encoding, context, true);
	}

	public DynamicPage(URL scriptURL, String encoding, Context context, boolean checkUpdates)
		throws IOException, ParseException 
	{
		super(scriptURL, encoding, context);
		this.checkUpdates = checkUpdates;
	}

	protected boolean needToUpdate(){
		if (scriptURLs == null){
			return true;
		}
		if (checkUpdates){
			try {
				for (Iterator it = scriptURLs.iterator(); it.hasNext(); ){
					URL url = (URL)it.next();
					long mod = url.openConnection().getLastModified();
					if (mod > parsedTime){
						return true;
					}
				}
			} catch (IOException e){
				// ignore
				e.printStackTrace();
			}
		}
		return false;
	}

	private Reader getReader(InputStream in, Context context)
		throws UnsupportedEncodingException 
	{
		if (encoding != null){
			return new InputStreamReader(in, encoding);
		} else {
			return Runtime.getScriptReader(in, context);
		}
	}

	static URL getDynamicPageURL(URL url, String encoding, Context context, Set scriptURLs){
		try {
			String specURL;
			if (encoding == null){
				specURL = "pea:" + url.toExternalForm();
			} else {
				specURL = "pea:" + url.toExternalForm() + "!charset=" + encoding;
			}
			return new URL(null, specURL, new Handler(context, scriptURLs));
		} catch (MalformedURLException mue){
			mue.printStackTrace();
			return null;
		}
	}

	protected Compiler getCompiler(){
		return new Compiler(null, false);
	}

	protected Pnuts compile(Pnuts parsed, Context context){
		Compiler compiler = getCompiler();
		if (compiler != null){
			return (Pnuts)compiler.compile(parsed, context);
		} else {
			return parsed;
		}
	}

	protected void update(Context context) throws IOException, ParseException {
		try {
			Set scriptURLs = new HashSet();
			scriptURLs.add(scriptURL);
			URL convertedURL = getDynamicPageURL(scriptURL, encoding, context, scriptURLs);
			InputStream in = null;
			try {
				in = convertedURL.openStream();
				StringWriter sw = new StringWriter();
				Pnuts parsed = Pnuts.parse(getReader(in, context), convertedURL, context);
				this.script = compile(parsed, context);
				this.scriptURLs = scriptURLs;
				this.parsedTime = System.currentTimeMillis();
			} finally {
				if (in != null){
					in.close();
				}
			}
		} catch (IOException e){
			throw new PnutsException(e, context);
		}
	}
}
