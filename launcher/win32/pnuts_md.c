/*
 * @(#)pnuts_md.c 1.1 03/10/10
 *
 * Copyright (c) 1997-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
#include <stdio.h>
#include <stdlib.h>
#include <malloc.h>
#include <process.h>
#include <windows.h>
#include <jni.h>

#ifdef WIN_MAIN
#define main _main
#define JAVA_PROGRAM "javaw"
#else
#define JAVA_PROGRAM "java"
#endif

static char pnuts_home[MAX_PATH];

extern int spawnPnuts(int argc, char** argv);

char* getPnutsHome()
{
    static int pnuts_home_set = 0;

    if (pnuts_home_set == 0){
	char *p;

	GetModuleFileName(0, pnuts_home, MAX_PATH);
	p = strrchr(pnuts_home, '\\');
	if (p != 0){
	    *p = '\0';
	}
	p = strrchr(pnuts_home, '\\');
	if (p != 0){
	    *p = '\0';
	}
	pnuts_home_set = 1;
    }
    return pnuts_home;
}

int spawnPnuts_md(char** vmArgv)
{
    vmArgv[0] = getenv("PNUTS_JAVA_COMMAND");
    if (vmArgv[0] == 0){
        vmArgv[0] = JAVA_PROGRAM;
    }
    return spawnvp(_P_WAIT, vmArgv[0], vmArgv);
}

int main (int argc, char** argv)
{
    return spawnPnuts(argc, argv);
}

#ifdef WIN_MAIN

__declspec(dllexport) char **__initenv;

#ifdef SHELL_NOTIFICATION
extern void sh_notify_initialize();
extern HINSTANCE __hInstance;

static char* quit_command[] = {"pshw.exe", "-e", "System::exit(0)", NULL};

void send_quit()
{
    char* pshw_path;

    pshw_path = malloc(strlen(pnuts_home) + 14);
    sprintf(pshw_path, "%s\\bin\\pshw.exe", pnuts_home);
    spawnvp(_P_WAIT, pshw_path, quit_command);
}

#endif

int WINAPI WinMain(HINSTANCE hInstance,
		   HINSTANCE hPrevInstance,
		   LPSTR     lpCmdLine,
		   int       nCmdShow )
{
#ifdef SHELL_NOTIFICATION
    __hInstance = hInstance;
    _beginthread(sh_notify_initialize, 512, NULL);
#endif

    __initenv = _environ;
    return main(__argc, __argv);
}
#endif
