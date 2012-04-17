/*
 * @(#)CommandListener.java 1.2 04/12/06
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.tools;


/**
 * @see pnuts.tools.DebugContext
 */
public interface CommandListener {
    /**
     * Some kind of event raised, e.g. the line number has changed.
     */
    void signal(CommandEvent event);
}
