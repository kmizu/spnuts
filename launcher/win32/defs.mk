CC = cl
LD = link
RC = rc
REGEDIT = regedit

INSTALL_DIR=../../../pnuts-1.1/bin

CPPFLAGS = -I "$(JDK_HOME)/include" -I "$(JDK_HOME)/include/win32" -I . -I ../share -DWIN32 -nologo -I dirent/include -I .
#CPPFLAGS += -DDEBUG
#CFLAGS = $(CPPFLAGS) -O1 -MT
CFLAGS = $(CPPFLAGS) -Zi -Od -MD

CC_OUT = -Fo
LD_OUT = -out:
LDFLAGS = -MAP -LIBPATH:dirent/lib
PNUTS_NATIVE_SRCPATH = .:../share/

SEP = ;
ifeq (,$(PNUTS_HOME))
  PNUTS_HOME=/pnuts
endif

PNUTS_EXECUTABLE_FILES = pnuts.exe pnutsw.exe pnutsc.exe

PNUTS_TARGET += $(PNUTS_EXECUTABLE_FILES)

PNUTS_CLEAN_TARGET += \
  pnuts.exe pnutsw.exe pnuts.obj pnutsw.obj pnuts_md.obj \
  pnuts.map pnutsw.map pnuts.pdb pnutsw.pdb pnuts.ilk pnutsw.ilk vc60.pdb \
  pnutsw.exp pnutsw.lib uf.obj \
  pnutsc.exe pnutsc.obj \
  pnuts.map pnutsc.map pnutsw.map

PNUTS_JNI = false
PNUTS_CFILES = pnuts.c pnuts_md.c
PNUTS_OBJFILES = pnuts.obj pnuts_md.obj
PNUTSW_OBJFILES = pnuts.obj pnutsw.obj
PNUTSC_OBJFILES = pnutsc.obj pnuts_md.obj

ifeq ($(PNUTS_JNI),true)
 PNUTS_CFILES += pnuts_jni.c
 PNUTS_OBJFILES += pnuts_jni.obj
 PNUTSW_OBJFILES += pnuts_jni.obj
 CPPFLAGS += -DPNUTS_JNI
endif

PNUTS_LIBS = advapi32.lib dirent.lib
PNUTSC_LIBS = advapi32.lib dirent.lib
