/*
 * @(#)PnutsLayoutMapping.java 1.2 04/12/06
 * 
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * See the file "LICENSE.txt" for information on usage and redistribution of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package pnuts.awt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.awt.*;
import java.io.*;
import java.util.Stack;

/**
 * <a href="/pnuts/doc/PnutsLayout.html">PnutsLayout</a> mapping of <a href="/pnuts/doc/hierarchicalLayout.html">Hierarchical
 * Layout</a>.
 * 
 * @version 1.1
 * @author Toyokazu Tomatsu
 */
public class PnutsLayoutMapping extends Layout {

	static Class[] paramType = new Class[] {String.class};
	Stack prototypeOfLabel = new Stack();

	private static Class jlabelClass = null;
	private static boolean jLabelFlg = false;
	private final static String swingPkgNames[] = {"com.sun.java.swing", "javax.swing"};

	private static Class findJLabelClass (String pkgName) {
		if (!jLabelFlg) {
			jLabelFlg = true;
			try {
				jlabelClass = Class.forName(pkgName + ".JLabel");
				return jlabelClass;
			} catch (ClassNotFoundException e) { /* ignore */
			}
		}
		return jlabelClass;
	}

	Component getPrototypeOfLabel () {
		if (prototypeOfLabel.size() > 0) {
			return (Component)prototypeOfLabel.peek();
		}
		return null;
	}

	Component makeLabel (String str, Container hint) {
		try {
			Container prototypeOfPanel = null;
			Component prototype = getPrototypeOfLabel();
			if (prototype != null) {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bout);
				out.writeObject(prototype);
				ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
				ObjectInputStream in = new ObjectInputStream(bin);
				Component c = (Component)in.readObject();
				Method m = c.getClass().getMethod("setText", paramType);
				m.invoke(c, new Object[] {str});
				return c;
			} else if (hint != null) {
				return createLabel(str, hint);
			} else {
				return new Label(str);
			}
		} catch (Exception e) {
			throw new LayoutException("can't create Label");
		}
	}

	Component createLabel (String str, Component hint) {
		String name = hint.getClass().getName();
		Object[] params = new Object[] {str};
		Class cl = null;
		try {
			int type = -1;
			if (name.startsWith(swingPkgNames[0])) {
				type = 0;
			} else if (name.startsWith(swingPkgNames[1])) {
				type = 1;
			}
			if (type == 0 || type == 1) {
				cl = findJLabelClass(swingPkgNames[type]);
				if (cl == null) {
					cl = Label.class;
				}
			} else {
				cl = Label.class;
			}
			Constructor con = cl.getConstructor(paramType);
			return (Component)con.newInstance(params);
		} catch (Exception e) {
			throw new LayoutException("can't create Label object");
		}
	}

	void markLabel (Component label) {
		if (label instanceof Label) {
			prototypeOfLabel.setElementAt(label, prototypeOfLabel.size() - 1);
		} else {
			try {
				String name = label.getClass().getName();
				Class cl = null;
				int type = -1;
				if (name.startsWith(swingPkgNames[0])) {
					type = 0;
				} else if (name.startsWith(swingPkgNames[1])) {
					type = 1;
				}
				if (type == 0 || type == 1) {
					cl = findJLabelClass(swingPkgNames[type]);
				}
				if (cl != null && cl.isInstance(label)) {
					prototypeOfLabel.setElementAt(label, prototypeOfLabel.size() - 1);
				}
			} catch (Exception e) {
				throw new LayoutException("can't create Label");
			}
		}
	}

	void initLabel (Container container) {
		if (prototypeOfLabel.size() == 0) {
			prototypeOfLabel.push(createLabel("", container));
		} else {
			prototypeOfLabel.push(prototypeOfLabel.peek());
		}
	}

	public Container createContainer (Container container, Object[] format) {
		initLabel(container);
		if (!(format[1] instanceof String)) {
			throw new LayoutException("Element after the PnutsLayout class must be a String.");
		}
		container.setLayout(new PnutsLayout((String)format[1]));
		for (int i = 2; i < format.length; i++) {
			if (format[i] == null) {
				container.add(makePanel(container), "");
			} else if (format[i] instanceof String) {
				container.add(makeLabel((String)format[i], container), "");
			} else if (isArray(format[i])) {
				Component compToAdd;
				Object addParam;
				Object a[] = (Object[])format[i];
				if (a[0] == null) {
					compToAdd = makePanel(container);
					addParam = a[1];
				} else if (isArray(a[0])) {
					compToAdd = Layout.layout(makePanel(container), (Object[])a[0]);
					addParam = a[1];
				} else if (a[0] instanceof Class) {
					compToAdd = Layout.layout(makePanel(container), a);
					addParam = "";
				} else if (a[0] instanceof String) {
					compToAdd = makeLabel((String)a[0], container);
					addParam = a[1];
				} else if (a[0] instanceof Component) {
					compToAdd = (Component)a[0];
					addParam = a[1];
					markLabel(compToAdd);
				} else {
					throw new LayoutException(
						"PnutsLayout requires array elements to contain a class, String, Component, array, or null.");
				}
				
				container.add(compToAdd, addParam);
				
				if (!(a[0] instanceof Class) && a.length > 2) {
					if (!(compToAdd instanceof Container))
						throw new LayoutException("Component must be a Container when a third element is used: " + a[0]);
					if (!(a[2] instanceof Object[])) throw new LayoutException("Third element must be an array: " + a[2]);
					Layout.layout((Container)compToAdd, (Object[])a[2]);
				}

			} else if (format[i] instanceof Component) {
				markLabel((Component)(format[i]));
				container.add((Component)format[i], "");
			} else {
				throw new LayoutException("PnutsLayout requires elements to be a String, Component, array, or null.");
			}
		}
		prototypeOfLabel.pop();
		return container;
	}
}
