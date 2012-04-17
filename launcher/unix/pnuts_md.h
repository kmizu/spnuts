/*
 * @(#)pnuts_md.h 1.2 04/04/09
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
#ifndef _PNUTS_MD_H_
#define _PNUTS_MD_H_

#ifdef darwin
#define LIBRARY_PATH "DYLD_LIBRARY_PATH"
#else
#define LIBRARY_PATH "LD_LIBRARY_PATH"
#endif
#define PATHSEP ":"

#endif
