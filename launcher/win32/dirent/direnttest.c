//
// extern char* findJarFiles(char* dir);
//
#include <dirent.h>
#include <errno.h>
#include <string.h>

#ifdef WIN32
#include <malloc.h>
#define strcasecmp stricmp
#endif

#define BUF_MARGIN 256

typedef struct {
    char* block;
    int pos;
    int size;
} buffer_t;


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
	free(buf->block);
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

char* findJarFiles(char* dir)
{
    DIR* dirp;
    struct dirent* e;
    int len0, len;
    buffer_t buf;

    buf.size = 0;
    buf.pos = 0;
    len = len0 = strlen(dir);

    dirp = opendir(dir);
    if (!dirp){
	perror("opendir");
	exit(-1);
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
		append(&buf, ";");
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

#ifdef UNITTEST
void main(int argc, char** argv)
{
    char *p;
    if (argc < 2){
	return;
    }
    p = findJarFiles(*++argv);
    if (p){
	printf("%s\n", p);
    }
}
#endif
