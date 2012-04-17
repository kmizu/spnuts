/*
 * BorderLayoutMapping.java
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import java.awt.*;

/**
 * BorderLayout mapping of <a href="/pnuts/doc/hierarchicalLayout.html">Hierarchical Layout</a>.
 *
 * @version	1.1
 * @author Toyokazu Tomatsu
 * @author Nathan Sweet (misc@n4te.com)
 */
public class BorderLayoutMapping extends Layout {

	public Container createContainer(Container container, Object[] format){
		if (!(format[1] instanceof Object[])){
			throw new LayoutException("Element after the BorderLayout class must be an array of constructor arguments.");
		}
		Object args[] = (Object[])format[1];
		BorderLayout lm = null;
		if (args.length == 0){
			lm = new BorderLayout();
		} else if (args.length == 2){
			lm = new BorderLayout(((Integer)args[0]).intValue(), ((Integer)args[1]).intValue());
		}
		container.setLayout(lm);
		for (int i = 2; i < format.length; i++){
			if (!(format[i] instanceof Object[])) throw new LayoutException("BorderLayout requires all elements to be an array.");
			Object[] a = (Object[])format[i];
			if (!(a[0] instanceof String)) throw new LayoutException("BorderLayout requires the first element to be a String.");
			Component compToAdd;
			if (a[1] == null){
				compToAdd = makePanel(container);
			} else if (isArray(a[1])){
				compToAdd = Layout.layout(makePanel(container), (Object[])a[1]);
			} else if (a[1] instanceof Component){
				compToAdd = (Component)a[1];
			} else{
				throw new LayoutException("BorderLayout requires the second element to be a Component, array, or null.");
			}
			container.add((String)a[0], compToAdd);
			if (a.length > 2) {
				if (!(compToAdd instanceof Container))
					throw new LayoutException("Component must be a Container when a third element is used: " + a[0]);
				if (!(a[2] instanceof Object[])) throw new LayoutException("Third element must be an array: " + a[2]);
				Layout.layout((Container)compToAdd, (Object[])a[2]);
			}
		}
		return container;
	}
}
