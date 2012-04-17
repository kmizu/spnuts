/*
 * ClassSpec.java
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package pnuts.compiler;

import pnuts.lang.Context;

public class ClassSpec {
        Class compileTimeClass;
        String className;
        
        private ClassSpec() {
        }

        public static ClassSpec create(Class compileTimeClass){
                ClassSpec c = new ClassSpec();
                c.compileTimeClass = compileTimeClass;
                c.className = compileTimeClass.getName();
                return c;
        }
        
        public static ClassSpec create(String className, Context context){
                ClassSpec c = new ClassSpec();
                c.compileTimeClass = context.resolveClass(className);
                if (c.compileTimeClass != null){
                        c.className = c.compileTimeClass.getName();
                } else {
                        c.className = className;
                }
                return c;
        }
}
