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
PLATFORM:= linux
PROGNAME:= gwt-ll
PROG    := lib$(PROGNAME).so
PROGDIR := ./
OBJDIR  := $(PROGDIR)objs/
OUT     := $(PROGDIR)image/$(PROG)

##
# Tools
##
CXX      := gcc
AR       := ar
STRIP    := strip
LD       := $(CXX)

##
# List of source, object, and dependency paths plus the path to them
##
SRCDIRS := ./:../gwt-ll-core/
VPATH   := .:../gwt-ll-core
SRCS    := gwt-ll.cpp Moz.cpp
OBJS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.o))
DEPS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.d))

##
# Include path configuration
##
SYSINCS := \
    $(ROOTDIR)../../third_party/j2sdk1.4.2_09/include \
    $(ROOTDIR)../../third_party/j2sdk1.4.2_09/include/$(PLATFORM) \
	$(ROOTDIR)../../third_party/gecko-sdk-linux/include \
	$(ROOTDIR)../../third_party/gecko-sdk-linux/include/extra

INCS := $(addprefix -i ,$(INCS)) $(addprefix -isystem ,$(SYSINCS))

##
# Libraries and library path
##
LIBS    = xpcomglue_s
LIBPATH = -L../../third_party/gecko-sdk-linux/lib
LIBS     := $(addprefix -l,$(LIBS))

# for notes on auto-dependency generation, see
#   http://make.paulandlesley.org/autodep.html
# -MP obviates the need for sed hackery
CFLAGS   := -Os -fPIC -fno-omit-frame-pointer -fno-strict-aliasing -D_REENTRANT -c -MMD -MP -Wno-system-headers $(CFLAGS)
LDFLAGS  := -s -fPIC -fno-omit-frame-pointer -fno-strict-aliasing -D_REENTRANT -Wl,-shared-gcc $(LDFLAGS)

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
all: $(OUT)

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
clean:
	@-rm -f $(CLEANFILES)
	@-rm -rf $(CLEANDIRS)
