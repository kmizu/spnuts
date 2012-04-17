INSTALL_DIR=../../../pnuts-1.1/bin
OS = $(shell uname -s)

ifneq (,$(findstring Darwin,$(OS)))
  PLATFORM=darwin
  JDK_INCLUDES = -I /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers 
  CC = cc
  LD = cc
  OVERRIDE_REALPATH = false
else
ifneq (,$(findstring SunOS,$(OS)))
  PLATFORM=solaris
endif
ifneq (,$(findstring Linux,$(OS)))
  PLATFORM=linux
endif
  JDK_INCLUDES = -I $(JDK_HOME)/include -I $(JDK_HOME)/include/$(PLATFORM)
  CC = gcc
  LD = gcc
  OVERRIDE_REALPATH = false
endif

SEP = :
ifeq (,$(PNUTS_HOME))
  PNUTS_HOME=/usr/local/pnuts
endif

CPPFLAGS += $(JDK_INCLUDES)
CPPFLAGS += -I . -I ../share -D$(PLATFORM)
#CPPFLAGS += -DDEBUG
#CPPFLAGS += -DOLDJAVA
CFLAGS = -O -g
#CFLAGS += 
LDFLAGS =
PNUTS_NATIVE_SRCPATH = .:../share

PNUTS_EXECUTABLE_FILES = pnuts pnutsc
PNUTS_TARGET += $(PNUTS_EXECUTABLE_FILES)

PNUTS_CLEAN_TARGET += pnuts pnuts.o pnuts_md.o cpath.o buf.o pnutsc.o pnutsc

PNUTS_CFILES = pnuts.c pnuts_md.c cpath.c
PNUTS_OBJFILES = pnuts.o pnuts_md.o cpath.o
PNUTSC_OBJFILES = pnutsc.o pnuts_md.o cpath.o

ifeq ($(OVERRIDE_REALPATH),true)
  PNUTS_CFILES += realpath.c
  PNUTS_OBJFILES += realpath.o
  PNUTSC_OBJFILES += realpath.o
endif

PNUTS_LIBS =
