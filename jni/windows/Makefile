# Copyright 2006 Google Inc. All Rights Reserved.
#
#-------------------------------------------------------------------------------
# Macros
#-------------------------------------------------------------------------------
SHELL   = /bin/sh

##
# Target settings
##
ROOTDIR = ./
PROGNAME:= gwt-ll
PROG    := $(PROGNAME).dll
PROGDIR := ./
OBJDIR  := $(PROGDIR)objs/
OUT     := $(PROGDIR)image/$(PROG)

##
# Tools
##
CXX      := mingw-gcc
AR       := mingw-ar
STRIP    := mingw-strip
LD       := $(CXX)

##
# List of source, object, and dependency paths plus the path to them
##
SRCDIRS := ./:../gwt-ll-core/
VPATH   := .:../gwt-ll-core
SRCS    := gwt-ll.cpp IE6.cpp
OBJS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.o))
DEPS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.d))

##
# Include path configuration
##
SYSINCS := $(ROOTDIR)../../third_party/j2sdk1.4.2_09/include \
           $(ROOTDIR)../../third_party/j2sdk1.4.2_09/include/win32
INCS := $(addprefix -isystem ,$(SYSINCS))

##
# Libraries and library path
##
LIBS    := wininet
LIBPATH := -L$(OBJDIR)
LIBS    := $(addprefix -l,$(LIBS))

# for notes on auto-dependency generation, see
#   http://make.paulandlesley.org/autodep.html
# -MP obviates the need for sed hackery
CFLAGS   := -Os -fno-exceptions -fshort-wchar -c -MMD -MP -Wno-system-headers $(CFLAGS)
LDFLAGS  := -s -Wl,--kill-at $(LDFLAGS)

##
# Clean macros
##
CLEANDIRS := $(CLEANDIRS) $(OBJDIR)
CLEANFILES := $(CLEANFILES) $(OUT)

#-------------------------------------------------------------------------------
# Rules
#-------------------------------------------------------------------------------

##
# default rule
##
all: $(OBJDIR)libwininet.a $(OUT)

##
# Include the dependency rules
##
-include $(DEPS)

##
# Compilation rule for cpp files
##
$(OBJDIR)%.o : $(SRCDIR)%.cpp
	@[ -d $(OBJDIR) ] || mkdir -p $(OBJDIR)
	$(CXX) $(CFLAGS) $(INCS) -o $@ $<

##
# Compilation rule for .def files to lib*.a files
##
$(OBJDIR)lib%.a : $(SRCDIR)%.def
	@[ -d $(OBJDIR) ] || mkdir -p $(OBJDIR)
	mingw-dlltool -k -d $< -l $@
	
##
# Actual output file
##
$(OUT): $(OBJS)
	@[ -d $(PROGDIR) ] || mkdir -p $(PROGDIR)
	@[ -d $(PROGDIR)image ] || mkdir -p $(PROGDIR)image
	$(LD) -shared $(LDFLAGS) $(LIBPATH) -o $@ $^ $(LIBS)
	$(STRIP) --strip-unneeded $@

##
# Clean rule
##
clean: clean-wininet
	@-rm -f $(CLEANFILES)
	@-rm -rf $(CLEANDIRS)
	
clean-wininet:
	@-rm -f $(OBJDIR)libwininet.a
	
