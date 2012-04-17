/*
 * @(#)PnutsJspTag.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.servlet;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.io.*;
import pnuts.lang.*;
import pnuts.lang.Package;
import pnuts.compiler.Compiler;

/**
 * JSP Tag Library for Pnuts
 *
 */
public class PnutsJspTag extends BodyTagSupport {

	private Pnuts parsed;
	private Context context;
	private static Compiler compiler = new Compiler(null, false, true);
	public final static String CONTEXT_ATTRIBUTE_NAME = "pnuts.lang.Context";

	public int doEndTag() throws JspException {
		try {
			if (bodyContent == null){
				return EVAL_PAGE;
			}
			Object ctx_attr = pageContext.getAttribute(CONTEXT_ATTRIBUTE_NAME);
			if (ctx_attr == null){
				context = new Context(new Package());
				pageContext.setAttribute(CONTEXT_ATTRIBUTE_NAME, context);
			} else {
				context = (Context)ctx_attr;
			}
			if (parsed == null){
				try {
					BodyContent bodyContent = getBodyContent();
					parsed = Pnuts.parse(bodyContent.getReader());
					parsed = compiler.compile(parsed, context);
				} catch (ParseException pe){
					throw new JspException("Script Error: " + pe);
				} catch (ClassFormatError cfe){
				}
			}
			context.setWriter(getPreviousOut());
			parsed.run(context);
		} catch(java.lang.Exception e) {
			throw new JspException("IO Error: " + e.getMessage());
		}
		return EVAL_PAGE;
	}
}
