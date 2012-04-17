/*
 * @(#)DynamicPageServlet.java 1.2 04/12/06
 *
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import pnuts.lang.Pnuts;
import pnuts.lang.Runtime;
import pnuts.lang.Context;
import org.pnuts.servlet.protocol.pea.Handler;

/**
 * Dynamic Web page with embeded scripts.
 *
 * This servlet converts the dynamic page to a Pnuts script, when needed, then
 * forward to the script.
 *
 */
public class DynamicPageServlet extends PnutsServlet {

	private String workdir;

	public void init() throws ServletException {
		ServletConfig conf = getServletConfig();
		String workdir = conf.getInitParameter("workdir");
		if (workdir != null){
			this.workdir = workdir;
		}
		super.init();
	}

	protected Pnuts parseFile(File file, String encoding, PnutsServletContext psc)
		throws IOException
		{
                        Context context = psc.context;
			FileInputStream fin = new FileInputStream(file);
			Reader reader;
			if (encoding != null){
				reader = new InputStreamReader(fin, encoding);
			} else {
				reader = Runtime.getScriptReader(fin, context);
			}
			StringWriter sw = new StringWriter();
                        Set scriptURLs = new HashSet();
                        scriptURLs.add(file.toURL());
			try {
				DynamicPage.convert(reader, sw, null, null, context, scriptURLs);
                                psc.scriptURLs = scriptURLs;
			} finally {
				reader.close();
			}
			URL url = new URL(null, "pea:" + file.toURL(), new Handler(context, scriptURLs));
			return Pnuts.parse(new StringReader(sw.toString()), url, context);
		}

	void do_service(HttpServletRequest request,
					HttpServletResponse response)
		throws ServletException, IOException
		{
			if (workdir == null){
				super.do_service(request, response);
				return;
			}
			try {
				String s_path = request.getServletPath();
				String path = "";
				if (s_path != null){
					path = s_path;
				}
				if (path == null){
					path = request.getPathInfo();
				}
				String real_path = getServletContext().getRealPath(path);

				File file = null;
				if (real_path != null){
					file = new File(real_path);
				}
				if (file == null || !file.exists()){
					if (debug){
						throw new FileNotFoundException(real_path);
					} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
				}
				String filename = file.getName();
				if (filename.endsWith(".pnut")){
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "");
					return;
				}
				int idx = filename.lastIndexOf('.');
				String s;
				if (idx > 0){
					s = filename.substring(0, idx) + ".pnut";
				} else {
					s = filename + ".pnut";
				}
				File dir = file.getParentFile();
				if (workdir != null){
					dir = new File(dir, workdir);
					if (!dir.exists()){
						dir.mkdirs();
					}
				}
				File outputFile = new File(dir, s);
		
				if (!outputFile.exists() || outputFile.lastModified() < file.lastModified()){
					DynamicPage.convert(file, outputFile, encoding, new Context());
				}
				request.getRequestDispatcher(s).forward(request, response);

			} catch (Throwable e){
				if (debug){
					e.printStackTrace();
				} else {
					System.err.println(e);
				}
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "");
			}
		}

}
