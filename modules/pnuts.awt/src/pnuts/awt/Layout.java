/*
 * Layout.java
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.awt.*;

import javax.swing.JComboBox;
import javax.swing.JScrollPane;

/**
 * Manager class of Hierarchical Layout.
 */
public abstract class Layout {
	private static Hashtable mappingTable = new Hashtable();
	private final static String swingPkgName = "javax.swing";

	static {
		// default mapping
		registerLayoutManager(BorderLayout.class, BorderLayoutMapping.class);
		registerLayoutManager(CardLayout.class, CardLayoutMapping.class);
		registerLayoutManager(GridLayout.class, GridLayoutMapping.class);
		registerLayoutManager(GridBagLayout.class, GridBagLayoutMapping.class);
		registerLayoutManager(FlowLayout.class, FlowLayoutMapping.class);
		registerLayoutManager(PnutsLayout.class, PnutsLayoutMapping.class);
	}

	private static Class jpanelClass = null;
	private static boolean jPanelFlg = false;

	private static Class findJPanelClass(){
		if (!jPanelFlg){
			jPanelFlg = true;
			try {
				jpanelClass = Class.forName("javax.swing.JPanel");
				return jpanelClass;
			} catch (ClassNotFoundException e){ /* ignore */ }
		}
		return jpanelClass;
	}

	/**
	 * Register a Layout Mapping
	 *
	 * @param	clazz	Class of the mapping
	 * @param	layout	The definition of the mapping
	 *				  This class should be subclass of Layout class.
	 */
	public static void registerLayoutManager(Class clazz, Class layout){
		if (Layout.class.isAssignableFrom(layout)){
			mappingTable.put(clazz, layout);
		}
	}

	protected static boolean isArray(Object obj){
		return obj.getClass().isArray();
	}

	protected Layout(){}

	/**
	 * Define how to make a container.
	 * This class should be defined in a subclass of Layout class.
	 */
	public abstract Container createContainer(Container container, Object[] fmt);

	/**
	 * Layout components using format
	 */
	public static Container layout(Object[] format){
		return layout(new Panel(), format);
	}

	/**
	 * Layout components in the <em>container</em> using format fmt.
	 */
	public static Container layout(Container container, Object[] fmt){
		if (container instanceof JScrollPane) {
			Component view = ((JScrollPane)container).getViewport().getView();
			if (view != null && view instanceof Container)
				container = (Container)view;
		}
		if (fmt[0] instanceof Class){
			Layout layout = null;
			try {
				Object map = mappingTable.get(fmt[0]);
				if (map instanceof Layout){
					return ((Layout)map).createContainer(container, fmt);
				} else if (map instanceof Class){
					layout = (Layout)((Class)map).newInstance();
					mappingTable.put(fmt[0], layout);
					return layout.createContainer(container, fmt);
				} else{
					throw new LayoutException("Mapping not defined: " + fmt[0]);
				}
			} catch (Exception ex){
				LayoutException layoutEx = new LayoutException(ex.getClass().getName() + ": " + ex.getMessage());
				ex.printStackTrace();
				layoutEx.initCause(ex);
				throw layoutEx;
			}
		} else {
			for (int i = 0; i < fmt.length; i++){
				Object c = fmt[i];
				if (c instanceof Component){
					container.add((Component)c);
				} else if (c instanceof Object[]){
					Object[] array = (Object[])c;
					if (!(array[0] instanceof Component))
						throw new LayoutException("First element must be a Component: " + c);
					if (array.length > 1){
						container.add((Component)array[0], array[1]);
						if (array.length > 2){
							if (!(array[0] instanceof Container))
								throw new LayoutException("Component must be a Container when a third element is used: " + array[0]);
							if (!(array[2] instanceof Object[]))
								throw new LayoutException("Third element must be an array: " + array[2]);
							Layout.layout((Container)array[0], (Object[])array[2]);
						}
					} else {
						container.add((Component)array[0]);
					}
				} else{
					throw new LayoutException("Element must be a Component or array: " + c);
				}
			}
			return container;
		}
	}

	protected static Container makePanel(Container prototype) {
		try {
			if (prototype == null){
				return new Panel();
			}
			Class cl = prototype.getClass();
			int guiType = 0; // 0:AWT, 1:javax.swing
			String name = cl.getName();
			if (name.startsWith(swingPkgName)){
				guiType = 1;
			}

			if (prototype instanceof Window){
				if (guiType == 1){
					cl = findJPanelClass();
					if (cl == null){
						cl = Panel.class;
					}
				} else {
					cl = Panel.class;
				}
			}
			Container ret = (Container)cl.newInstance();
			ret.setForeground(prototype.getForeground());
			ret.setBackground(prototype.getBackground());
			ret.setCursor(prototype.getCursor());
			ret.setFont(prototype.getFont());
			if (guiType == 1){
				Method m = null;
				Class t_noarg[] = new Class[]{};
				Object noarg[] = new Object[]{};
				Class t_boolean[] = new Class[]{Boolean.TYPE};
				Class t_string[] = new Class[]{String.class};
				Class clazz = prototype.getClass();
				Object val = null;

				m = clazz.getMethod("isDoubleBuffered", t_noarg);
				val = m.invoke(prototype, noarg);
				m = cl.getMethod("setDoubleBuffered", t_boolean);
				m.invoke(ret, new Object[]{val});

				m = clazz.getMethod("getToolTipText", t_noarg);
				val = m.invoke(prototype, noarg);
				m = cl.getMethod("setToolTipText", t_string);
				m.invoke(ret, new Object[]{val});

				m = clazz.getMethod("isOpaque", t_noarg);
				val = m.invoke(prototype, noarg);
				m = cl.getMethod("setOpaque", t_boolean);
				m.invoke(ret, new Object[]{val});

				m = clazz.getMethod("getAutoscrolls", t_noarg);
				val = m.invoke(prototype, noarg);
				m = cl.getMethod("setAutoscrolls", t_boolean);
				m.invoke(ret, new Object[]{val});
			}
			return ret;
		} catch (Exception e){
			return new Panel();
		}
	}
}
