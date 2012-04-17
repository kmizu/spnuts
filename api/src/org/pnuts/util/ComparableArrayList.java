/*
 * ComparableArrayList.java
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.util;

import java.util.*;
import pnuts.lang.Runtime;

public class ComparableArrayList extends ArrayList implements Comparable {

	public int compareTo(Object o) {
		return Runtime.compareObjects(this, o);
	}
}
