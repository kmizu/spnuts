/*
 * %W% %E%
 *
 * Copyright (c) 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
#include <stdio.h>
#include <stdlib.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <pnuts.h>

static char **java_args;
static int num_args;

#ifdef DEBUG
void dumpArgv(char** argv)
{
    char**p = argv;
    for (;;){
	if (*p){
	    printf("<%s>\n", *p++);
	} else {
	    break;
	}
    }
}
#endif

static void ensureSize(buffer_t* buf, int size)
{
    if (buf->size < size){
	int newsize = size + BUF_MARGIN;	
	char* newbuf = malloc(newsize);
	
	if (newbuf == 0){
	    perror("malloc");
	    exit(-1);
	}
	if (buf->size > 0){
	    memcpy(newbuf, buf->block, buf->size);
	}
	if (buf->block != 0){
	    free(buf->block);
	}
	buf->block = newbuf;
	buf->size = newsize;
    }
}

static void append(buffer_t* buf, char* str)
{
    int needed;
    int len;

    len = strlen(str);
    needed = buf->pos + len + 1;
    ensureSize(buf, needed);
    strcpy(buf->block + buf->pos, str);
    buf->pos += len;
}

static char* findJarFiles(char* dir)
{
    DIR* dirp;
    struct dirent* e;
    int len0, len;
    buffer_t buf;
    int need_separator = 0;

    buf.size = 0;
    buf.pos = 0;
    buf.block = 0;
    len = len0 = strlen(dir);

#ifdef DEBUG
    printf("opendir(%s)\n", dir);
#endif
    dirp = opendir(dir);
    if (!dirp){
	return 0;
    }
    while (dirp){
	errno = 0;
	if ((e = readdir(dirp)) != 0){
	    char* name;
	    char* p;

	    name = e->d_name;
	    p = strrchr(name, '.');
	    if (p == 0){
		continue;
	    }
	    if (strcasecmp(".jar", p) == 0 || strcasecmp(".zip", p) == 0){
	      if (need_separator){
		append(&buf, PATHSEP);
	      } else {
		need_separator = 1;
	      }
	      append(&buf, dir);
	      append(&buf, "/");
	      append(&buf, name);
	    }
	} else {
	    closedir(dirp);
	    break;
	}
    }
    if (buf.pos > 0){
	return buf.block;
    } else {
	return 0;
    }
}

static void set_library_path(char* pnuts_home)
{
  char* lib;
  char* buf;

  lib = getenv(LIBRARY_PATH);
  if (lib){
    buf = malloc(7 + strlen(lib) + strlen(pnuts_home) + strlen(LIBRARY_PATH));
    if (buf == 0){
      perror("malloc");
      exit(-1);
    }
    sprintf(buf, LIBRARY_PATH "=%s" PATHSEP "%s/lib", lib, pnuts_home);
  } else {
    buf = malloc(6 + strlen(pnuts_home) + strlen(LIBRARY_PATH));
    if (buf == 0){
      perror("malloc");
      exit(-1);
    }
    sprintf(buf, LIBRARY_PATH "=%s/lib", pnuts_home);
  }
  putenv(buf);
}


/*
 * pnuts { -e expression |
           -r resource |
           -f initfile |
           -m moduleName |
           -v  }*
         { fileName { arg1, ... }}
 */
static char**
parseArgs(int* totalArgc, int* jvmArgc, int argc, char** argv)
{
    int i = 0, j = 0, k = 0, n = 0;
    int langArgc;
#ifdef OLDJAVA
    int vmArgc = 3;
#else
    int vmArgc = 3;
#endif
    char** vmArgv;
    char* classpath;
    char* classpath_arg;
    char* ext_arg;
    char* home_arg;
    char* p, *pnuts_home;
    int verbose = 0;
    char* jar_name;
    char* jar_files;
    char* module_dir;
    int use_bootclasspath = 0;
    int classpath_len;
    char* modules;
    char* proxyHost;
    char* proxyPort;

    langArgc = num_args;

    for (i = 1; i < argc; i++){
	if (strncmp(argv[i], "-J", 2) == 0) {
	    vmArgc++;
	    n++;
        } else {
	    langArgc++;
	    if (strncmp(argv[i], "-v", 2) == 0){
		verbose = 1;
	    }
        }
    }
    jar_name = PNUTS_JAR_FILE;

    pnuts_home = (char*)getPnutsHome();

    set_library_path(pnuts_home);

    if (getenv("PNUTS_JDK11_COMPATIBLE") == 0){
	use_bootclasspath = 1;
    }
    modules = getenv("PNUTS_MODULE");
    vmArgc++;

    proxyHost = getenv("HTTP_PROXY_HOST");
    if (proxyHost){
      vmArgc++;
    }
    proxyPort = getenv("HTTP_PROXY_PORT");
    if (proxyPort){
      vmArgc++;
    }

    *totalArgc = vmArgc + langArgc;
    *jvmArgc = vmArgc;

    if ((vmArgv = (char**)malloc((*totalArgc + 1) * sizeof(char*))) == 0){
	fprintf(stderr, "out of memory\n");
	exit(-1);
    }

    p = getenv("PNUTS_JAVA_COMMAND");
    if (p == 0){
      vmArgv[j] = PNUTS_JAVA_COMMAND;
    }
    else {
      vmArgv[j] = p;
    }
    j++;

    module_dir = malloc(strlen(pnuts_home) + 9);
    if (module_dir == 0){
	fprintf(stderr, "out of memory\n");
	exit(-1);
    }
    sprintf(module_dir, "%s/" PNUTS_MODULE_DIR, pnuts_home);

    ext_arg = malloc(strlen(module_dir) + 22);
    if (ext_arg == 0){
	fprintf(stderr, "out of memory\n");
	exit(-1);
    }
    sprintf(ext_arg, "-Djava.endorsed.dirs=%s", module_dir);
    vmArgv[j++] = ext_arg;

    if (proxyHost){
      char *p = malloc(strlen(proxyHost) + 18);
      sprintf(p, "-Dhttp.proxyHost=%s", proxyHost);
      vmArgv[j++] = p;
    }
    if (proxyPort){
      char *p = malloc(strlen(proxyPort) + 18);
      sprintf(p, "-Dhttp.proxyPort=%s", proxyPort);
      vmArgv[j++] = p;
    }


    home_arg = malloc(14 + strlen(pnuts_home));
    if (home_arg == 0){
	fprintf(stderr, "out of memory\n");
	exit(-1);
    }
    sprintf(home_arg, "-Dpnuts.home=%s", pnuts_home);
    vmArgv[j++] = home_arg;

    if (use_bootclasspath){
	classpath_arg = malloc(strlen(pnuts_home) +
			       strlen(jar_name) +
			       24);
	if (classpath_arg == 0){
	    fprintf(stderr, "out of memory\n");
	    exit(-1);
	}
	sprintf(classpath_arg,
		"-Xbootclasspath/a:%s/lib/%s",
		pnuts_home,
		jar_name);
	vmArgv[j++] = classpath_arg;
    }

    if (modules){
        char* p = malloc(23 + strlen(modules));
	if (p == 0){
	    fprintf(stderr, "out of memory\n");
	    exit(-1);
	}
	sprintf(p, "-Dpnuts.tools.modules=%s", modules);
	vmArgv[j++] = p;
    } else {
	vmArgv[j++] = "-Dpnuts.tools.modules=" PNUTS_DEFAULT_MODULE;
    }

    memcpy(vmArgv + j + n, java_args, sizeof(char*) * num_args);
    k = j + n + num_args;

    for (i = 1; i < argc; i++){
	if (strncmp(argv[i], "-J", 2) == 0) {
	    vmArgv[j++] = (char*)strdup(argv[i] + 2);
	} else {
 	    vmArgv[k++] = (char*)strdup(argv[i]);
        }
    }
    vmArgv[k] = 0;

    return vmArgv;
}

/*
 * Starts Pnuts as a child process
 */
int spawnPnuts(int argc, char** argv)
{
    char **vmArgv;
    int totalArgc, jvmArgc;

    java_args = (char**)malloc(sizeof(char**) + 1);
    java_args[0] = PNUTS_MAIN_CLASS;
    num_args = 1;

    vmArgv = parseArgs(&totalArgc, &jvmArgc, argc, argv);
#ifdef DEBUG
    {
	char *cp = getenv("CLASSPATH");
	if (cp){
	    printf("CLASSPATH=%s\n", cp);
	}
    }
    dumpArgv(vmArgv);
#endif
    return spawnPnuts_md(vmArgv);
}
