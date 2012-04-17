/*
 * PnutsServlet.java
 *
 * Copyright (c) 1997-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Locale;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsImpl;
import pnuts.lang.Context;
import pnuts.lang.Runtime;
import pnuts.lang.Package;
import pnuts.compiler.Compiler;
import pnuts.compiler.CompilerPnutsImpl;

/**
 * Servlet for Pnuts scripting
 *
 * <pre>
 * Initparams:
 *   'module'   the module to be use()'d
 *   'compile'  scripts are compiled if true
 * </pre>
 */
public class PnutsServlet extends HttpServlet {

	public final static String SYMBOL_THIS = "this".intern();
	public final static String SYMBOL_REQUEST = "request".intern();
	public final static String SYMBOL_RESPONSE = "response".intern();
	public final static String SERVLET_REQUEST = "request".intern();
	public final static String SERVLET_RESPONSE = "response".intern();
	public final static String SERVLET_BUFFER = "pnuts.servlet.buffer".intern();
	public final static String EXECUTE_LATEST_SCRIPT = "pnuts.servlet.latest.script".intern();
	public final static String SERVLET_WRITER = "pnuts.servlet.writer".intern();
	public final static String SERVLET_MULTIPART_PARAM = "pnuts.servlet.multipart.param".intern();
	public final static String SERVLET_PARAM = "pnuts.servlet.parameters".intern();
	public final static String SERVLET_COOKIE = "pnuts.servlet.cookies".intern();
	public final static String SERVLET_FILE = "pnuts.servlet.file".intern();
	public final static String SERVLET_BASEDIR = "pnuts.servlet.dir".intern();
	public final static String REQUEST_SCOPE = "pnuts.servlet.request.scope".intern();
	public final static String SERVLET_COMPILER = "pnuts.servlet.compiler".intern();
	private final static String LOCALE_KEY = "pnuts$lib$locale".intern();
	private final static String TIMEZONE_KEY = "pnuts$lib$timezone".intern();
	
	boolean debug;
	boolean buffering;
	boolean checkUpdate = true;
	boolean compile = true;
	Compiler compiler;
	File scriptFile;
	String encoding;
	boolean isolation;
       URL baseURL;
       String errorPage;

	private PnutsServletContext pnutsServletContext;
	private Hashtable contexts = new Hashtable();
	private Context context = new Context();
	private Package pkg;

	public PnutsServlet(){
	}

	public void init() throws ServletException {
		ServletConfig conf = getServletConfig();
		debug = Boolean.valueOf(conf.getInitParameter("debug")).booleanValue();
		String module = conf.getInitParameter("module");
		String script = conf.getInitParameter("script");
		ServletContext servletContext = getServletContext();
		if (script != null){
		    String path = servletContext.getRealPath(script);
		    File f = new File(path);
		    if (f.exists()){
			this.scriptFile = f;
		    }
		}
		String _compile = conf.getInitParameter("compile");
		boolean compile = this.compile = Boolean.valueOf(_compile).booleanValue();
		if (compile){
			this.compiler = new Compiler(null, false, true);
			context.set(SERVLET_COMPILER, compiler);
		}
		String locale = conf.getInitParameter("locale");
		String timezone = conf.getInitParameter("timezone");

		PnutsImpl pnutsImpl;
		if (compile){
			pnutsImpl = new CompilerPnutsImpl(true, true);
		} else {
			pnutsImpl = PnutsImpl.getDefault();
		}
		context.setImplementation(pnutsImpl);
		String _buffering = conf.getInitParameter("buffering");
		if (_buffering != null){
			buffering = Boolean.valueOf(_buffering).booleanValue();
		} else {
			buffering = true;
		}
		String check = conf.getInitParameter("execute-latest-script");
		if (check != null){
		    checkUpdate = Boolean.valueOf(check).booleanValue();
		}
		if (checkUpdate){
		    context.set(EXECUTE_LATEST_SCRIPT, Boolean.TRUE);
		}
		encoding = conf.getInitParameter("encoding"); // script encoding
		if (encoding != null){
			context.setScriptEncoding(encoding);
		}
		String _isolation = conf.getInitParameter("isolation");
		if (_isolation != null){
			isolation = Boolean.valueOf(_isolation).booleanValue();
		} else {
			isolation = true;
		}

		this.errorPage = conf.getInitParameter("error-page");

		if (locale != null){
			StringTokenizer st = new StringTokenizer(locale, "_");
			int n = st.countTokens();
			Locale loc;
			switch (n){
			case 0:
				loc = new Locale("");
				break;
			case 1:
				loc = new Locale(st.nextToken(), "", "");
				break;
			case 2:
				loc = new Locale(st.nextToken(), st.nextToken(), "");
				break;
			default:
				loc = new Locale(st.nextToken(), st.nextToken(), st.nextToken());
				break;
			}
			context.set(LOCALE_KEY, loc);
		}

		if (timezone != null){
			context.set(TIMEZONE_KEY, TimeZone.getTimeZone(timezone));
		}

		this.pkg = new Package("root", null);
		context.setCurrentPackage(pkg);

		if (module != null){
			StringTokenizer st = new StringTokenizer(module, ",", false);
			while (st.hasMoreTokens()){
				String mod = st.nextToken().trim();
				context.usePackage(mod);
			}
		}
		context.set(SYMBOL_THIS, this);
		context.set(SERVLET_BASEDIR, new File(servletContext.getRealPath("/")));

		String initialScript = conf.getInitParameter("initialScript");
		try {
		       this.baseURL = new File(servletContext.getRealPath("/")).toURL();
			if (initialScript != null){
			    String path = servletContext.getRealPath(initialScript);
			    File f = new File(path);
			    if (f.exists()){
				File dir = f.getParentFile();
				context.set(SERVLET_FILE, f);
				pkg.set(SYMBOL_THIS, this, context);
				ClassLoader ccl = getContextClassLoader();
				URL[] urls = new URL[]{dir.toURL(), baseURL};
				ClassLoader loader = new URLClassLoader(urls, ccl);
				ClassLoader pcl = Pnuts.createClassLoader(context, loader);
				context.setClassLoader(pcl);
				Pnuts.require(initialScript, context);
			    } else {
				System.err.println(path + " is not found");
			    }
			}
			if (this.scriptFile != null){
			    this.pnutsServletContext = createPnutsServletContext(scriptFile);
			}
		} catch (FileNotFoundException e1){
		    e1.printStackTrace();
		} catch (MalformedURLException e2){
		    e2.printStackTrace();
		}
	}

	protected void doGet(HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {
		do_service(request, response);
	}
	
	protected void doPost(HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {
		do_service(request, response);
	}

	protected void doPut(HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {
		do_service(request, response);
	}	
	
	protected void doDelete(HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException, IOException {
		do_service(request, response);
	}	

	/*
	 * Get the ClassLoader by which ServletContext finds resources and classes.
	 * There is no standard way to get this.  We use Thread.getContextClassLoader()
	 * here, since Tomcat 3.3a, 4.0  and Resin2.0.5 use it in J2SE environment. 
	 */
	protected ClassLoader getContextClassLoader(){
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * The following context-variables are defined
	 *
	 * <dl>
	 * <dt>pnuts.servlet.request
	 * <dt>pnuts.servlet.response
	 * <dt>pnuts.servlet.file
	 * </dl>
	 */
	void do_service(HttpServletRequest request,
					HttpServletResponse response)
		throws ServletException, IOException
	{
			Context c = null;
			Package p = null;
			Pnuts script = null;
			long time = 0L;
			try {
				File file = scriptFile;
				if (file == null){
				    String path = "";
				    String servletPath = request.getServletPath();
				    if (servletPath != null){
					path = servletPath;
				    }
				    if (path == null){
					path = request.getPathInfo();
				    }
				    String real_path = getServletContext().getRealPath(path);

				    if (real_path != null){
					file = new File(real_path);
				    }
				}
				if (file == null || !file.exists()){
					if (debug){
						throw new FileNotFoundException();
					} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
				}
						
				PnutsServletContext ctx = this.pnutsServletContext;
				if (ctx == null){
					synchronized (contexts){
						ctx = (PnutsServletContext)contexts.get(file);
						if (ctx == null){
							ctx = createPnutsServletContext(file);
							c = ctx.context;
							p = ctx.basePackage;
							contexts.put(file, ctx);
							if (debug){
								c.setVerbose(true);
							}
						} else {
							c = ctx.context;
							p = ctx.basePackage;
							script = ctx.script;
							time = ctx.time;
						}
					}
				} else {
					c = ctx.context;
					p = ctx.basePackage;
					script = ctx.script;
					time = ctx.time;
				}
				c = (Context)c.clone();
				Thread.currentThread().setContextClassLoader(c.getClassLoader());

				boolean buffering = this.buffering;
				ResponseWriter rw = new ResponseWriter(response, c, buffering);
				c.setWriter(rw);
				p.set(SYMBOL_THIS, this, c);

				Package pkg = new Package(null, p);
				pkg.set(SYMBOL_RESPONSE, response, c);
				pkg.set(SYMBOL_REQUEST, request, c);
				c.set(SERVLET_RESPONSE, response);
				c.set(SERVLET_REQUEST, request);
				c.set(REQUEST_SCOPE, pkg);

				c.setCurrentPackage(pkg);

				if (script == null){
					script = readScript(file, encoding, ctx);
				} else if (checkUpdate){
                                        if (ctx.needToUpdate()){
						script = readScript(file, encoding, ctx);
					}
				}
				script.run(c);

				if (buffering){
					rw.flushBuffer();
				}

			} catch (Throwable e){
				if (debug){
					e.printStackTrace();
				} else {
					System.err.println(e);
				}
				if (errorPage != null){
					response.sendRedirect(errorPage);
				} else {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "");
				}
			}
	}

	PnutsServletContext createPnutsServletContext(File file) throws MalformedURLException{
		ClassLoader ccl = getContextClassLoader();
		File dir = file.getParentFile();
		URL[] urls = new URL[]{dir.toURL(), baseURL};
		ClassLoader loader = new URLClassLoader(urls, ccl);
		ClassLoader pcl = Pnuts.createClassLoader(context, loader);
		Context c;
		if (isolation){
			c = new Context(context);
		} else{
			c = (Context)context.clone();
		}
		c.setClassLoader(pcl);
		c.set(SERVLET_FILE, file);
		c.set(SYMBOL_THIS, this);
		Package p;
		if (isolation){
			p = (Package)pkg.clone();
		} else {
			p = pkg;
		}
		PnutsServletContext ctx = new PnutsServletContext(c, p);
		if (debug){
			c.setVerbose(true);
		}
		return ctx;
	}

	protected Pnuts parseFile(File file, String encoding, PnutsServletContext psc)
		throws IOException
		{
			URL scriptURL = file.toURL();
			Reader reader = null;
			if (encoding != null){
				reader = new InputStreamReader(new FileInputStream(file), encoding);
			} else {
				reader = Runtime.getScriptReader(new FileInputStream(file), context);
			}
			try {
				Pnuts result = Pnuts.parse(reader, scriptURL, psc.context);
                                Set scriptURLs = new HashSet();
                                scriptURLs.add(scriptURL);
                                psc.scriptURLs = scriptURLs;
                                return result;
			} finally {
				if (reader != null){
					reader.close();
				}
			}
		}

	protected Pnuts readScript(File file, String encoding, PnutsServletContext psc)
		throws IOException
		{
			if (debug){
				System.out.println("readScript " + file);
			}

			Pnuts expr = parseFile(file, encoding, psc);
			if (compile){
				try {
					expr = compiler.compile(expr,  psc.context);
				} catch (ClassFormatError e){
					if (debug){
						System.out.println("compile failed (interpreting)");
					}
				}
			}
                        psc.script = expr;
                        psc.time = System.currentTimeMillis();
			return expr;
		}

	public String getServletInfo(){
		return "Pnuts Servlet for servlet scripting. See http://pnuts.org/ for more information";
	}
}
