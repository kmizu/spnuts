/*
 * @(#)cpath.c 1.2 04/04/08
 *
 * Copyright (c) 1997-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
#include <limits.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/stat.h>
#include <alloca.h>

static int
isExecutable(char* path)
{
    struct stat sbuf;

    if (access(path, X_OK)){
	return 0;
    } else {
	if (stat(path, &sbuf)){
	    return 0;
	} else {
	    if ((sbuf.st_mode & S_IFMT) == S_IFREG){
		return 1;
	    }
	    return 0;
	}
    }
}

static char*
find_command_0(char* arg0, char* path)
{
    char r_path[PATH_MAX];
    int len;
    char* p = path;
    char* s;
    char* t;
    char* buf;

    while (*p){
	s = strchr(p, ':');
	if (s){
	    int n = s - p;
	    t = alloca(n + 1);
	    strncpy(t, p, n);
	    t[n] = 0;
	} else {
	    t = p;
	}

	len = strlen(arg0) + strlen(t) + 2;
	buf = (char*)alloca(len);
	sprintf(buf, "%s/%s", t, arg0);

	realpath(buf, r_path);

	if (isExecutable(r_path)){
	    return strdup(r_path);
	}

	if (s){
	    p = s + 1;
	} else {
	    break;
	}
    }
    return 0;
}

char*
find_command(char* name, char* path)
{

    if (name[0] == '/'){
	return name;
    } else if (name[0] == '.'){
	if (strncmp("./", name, 2) == 0 ||
	    strncmp("../", name, 3) == 0)
        {
	    char r_path[PATH_MAX];
	    realpath(name, r_path);
	    return strdup(r_path);
	}
    }
    if (strchr(name, '/') != 0){
	char *c = find_command_0(name, ".");
	if (c){
	    return c;
	}
    }
    return find_command_0(name, path);
}
