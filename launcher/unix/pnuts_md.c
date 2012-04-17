/*
 * @(#)pnuts_md.c 1.2 03/10/22
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <jni.h>

#define PNUTS_JAVA_COMMAND "java"
#define MAX_PATH 512

static char pnuts_home[MAX_PATH];
static char* argv0;

extern int spawnPnuts(int argc, char** argv);
extern char* find_command(char* name, char* path);

static char*
_getPnutsHome(char* cmd)
{
    char* p0, *p, p1[MAX_PATH];
    static char p2[MAX_PATH];

    p0 = find_command(cmd, getenv("PATH"));
    p = strrchr(p0, '/');
    if (p){
	*p = 0;
    }
    p = strrchr(p0, '/');
    if (p){
	*p = 0;
    }
    strcpy(p1, p0);
    
    return (char*)realpath(p1, p2);
}

char* getPnutsHome()
{
    static char* pnuts_home = 0;

    if (pnuts_home == 0){
	pnuts_home = _getPnutsHome(argv0);
    }
    return pnuts_home;
}

int spawnPnuts_md(char** vmArgv)
{
    pid_t pid;
    int stat;

    if ((pid = vfork()) == -1){
	exit(-1);
    } else if (pid == 0){
        vmArgv[0] = getenv("PNUTS_JAVA_COMMAND");
	if (vmArgv[0] == 0){
	  vmArgv[0] = PNUTS_JAVA_COMMAND;
	}
	if (execvp(vmArgv[0], vmArgv) == -1){
	  fprintf(stderr, "%s cannot be started\n", vmArgv[0]);
	  exit(-1);
	}
    } else {
        wait(&stat);
    }
    return 0;
}

int main (int argc, char** argv)
{
    argv0 = *argv;
    return spawnPnuts(argc, argv);
}
