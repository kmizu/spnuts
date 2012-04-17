/*
 * ObjectDesc.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package org.pnuts.lang;

import java.lang.reflect.*;
import java.util.*;

public interface ObjectDesc {

    public Method[] getMethods();

    public void handleProperties(PropertyHandler handler);
}
