/*
 * GridBagLayoutMapping.java
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import java.awt.*;
import java.util.*;

/**
 * GridBagLayout mapping of <a href="/pnuts/doc/hierarchicalLayout.html">Hierarchical Layout</a>.
 *
 * @author Toyokazu Tomatsu
 * @author Nathan Sweet (misc@n4te.com)
 */
public class GridBagLayoutMapping extends Layout {

	public Container createContainer(Container container, Object[] format){
		GridBagLayout lm = new GridBagLayout();
		container.setLayout(lm);

		for (int i = 1; i < format.length; i++){
			GridBagConstraints c = new GridBagConstraints();
			if (!(format[i] instanceof Object[])){
				throw new LayoutException("GridBagLayout requires elements to be an array.");
			}
			Object a[] = (Object[])format[i];
			if (!(a[0] instanceof String)){
				throw new LayoutException("GridBagLayout requires the first element to be a String.");
			}
			Map table = PnutsLayout.str2table((String)a[0]);
			String g;
			if ((g = (String)table.get("gridx")) != null){
				c.gridx = Integer.parseInt(g);
			}
			if ((g = (String)table.get("gridy")) != null){
				c.gridy = Integer.parseInt(g);
			}
			if ((g = (String)table.get("gridwidth")) != null){
				c.gridwidth = Integer.parseInt(g);
			}
			if ((g = (String)table.get("gridheight")) != null){
				c.gridheight = Integer.parseInt(g);
			}
			if ((g = (String)table.get("weightx")) != null){
				c.weightx = Double.parseDouble(g);
			}
			if ((g = (String)table.get("weighty")) != null){
				c.weighty = Double.parseDouble(g);
			}
			if ((g = (String)table.get("anchor")) != null){
				try {
					c.anchor = ((Integer)GridBagConstraints.class.getField(g).get(null)).intValue();
				} catch (Exception e){}
			}
			if ((g = (String)table.get("fill")) != null){
				try {
					c.fill = ((Integer)GridBagConstraints.class.getField(g).get(null)).intValue();
				} catch (Exception e){}
			}
			if ((g = (String)table.get("insets")) != null){
				StringTokenizer st = new StringTokenizer(g, ":");
				c.insets = new Insets(Integer.parseInt(st.nextToken()),
									  Integer.parseInt(st.nextToken()),
									  Integer.parseInt(st.nextToken()),
									  Integer.parseInt(st.nextToken()));
			}
			if ((g = (String)table.get("ipadx")) != null){
				c.ipadx = Integer.parseInt(g);
			}
			if ((g = (String)table.get("ipady")) != null){
				c.ipady = Integer.parseInt(g);
			}
			Component comp = null;
			if (a[1] instanceof Object[]){
				comp = Layout.layout(makePanel(container), (Object[])a[1]);
			} else if (a[1] instanceof Component){
				comp = (Component)a[1];
			} else{
				throw new LayoutException("GridBagLayout requires the second element to be a Component or array.");
			}
			lm.setConstraints(comp, c);
			container.add(comp);
			if (a.length > 2) {
				if (!(comp instanceof Container))
					throw new LayoutException("Component must be a Container when a third element is used: " + a[0]);
				if (!(a[2] instanceof Object[])) throw new LayoutException("Third element must be an array: " + a[2]);
				Layout.layout((Container)comp, (Object[])a[2]);
			}
		}
		return container;
	}
}
