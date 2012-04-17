/*
 * FlowLayoutMapping.java
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.awt;

import java.awt.*;

/**
 * FlowLayout mapping of <a href="/pnuts/doc/hierarchicalLayout.html">Hierarchical Layout</a>.
 *
 * @author Toyokazu Tomatsu
 * @author Nathan Sweet (misc@n4te.com)
 */
public class FlowLayoutMapping extends Layout {

	public Container createContainer(Container container, Object[] format){
		if (!(format[1] instanceof Object[])){
			throw new LayoutException("Element after the FlowLayout class must be an array of constructor arguments: " + format[1]);
		}
		Object args[] = (Object[])format[1];
		FlowLayout lm = null;
		if (args.length == 0){
			lm = new FlowLayout();
		} else if (args.length == 1){
			lm = new FlowLayout(((Integer)args[0]).intValue());
		} else if (args.length == 3){
			lm = new FlowLayout(((Integer)args[0]).intValue(),
								((Integer)args[1]).intValue(),
								((Integer)args[2]).intValue());
		}
		container.setLayout(lm);
		for (int i = 2; i < format.length; i++){
			Object a = format[i];
			if (a == null){
				container.add("", makePanel(container));
			} else if (isArray(a)){
				Object nestedArray[] = (Object[])format[i];
				if (nestedArray[0] instanceof Class)
					container.add("", Layout.layout(makePanel(container), (Object[])a));
				else if (nestedArray[0] instanceof Container) {
					if (!(nestedArray[1] instanceof Object[])) throw new LayoutException("Second element must be an array: " + nestedArray[1]);
					container.add("", (Container)nestedArray[0]);
					Layout.layout((Container)nestedArray[0], (Object[])nestedArray[1]);
				} else {
					throw new LayoutException("FlowLayout requires an array element to start with a Class or Container: " + nestedArray[0]);
				}
			} else if (a instanceof Component){
				container.add("", (Component)a);
			} else{
				throw new LayoutException("FlowLayout requires elements to be a Component, array, or null.");
			}
		}
		return container;
	}
}
