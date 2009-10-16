# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

##
# Target settings
##
#JAVA_HOME
#JAVA_HOME=/usr/lib/j2sdk1.5-sun

# set GWT_TOOLS only if it is not already defined
GWT_TOOLS ?= ../../../tools

GWT_ROOT = ../../
OBJDIR  := $(GWT_ROOT)build/out/jni/linux/
OUTDIR  := $(GWT_ROOT)build/jni/linux/
OUT     := $(OUTDIR)libgwt-ll.so
STAGING := $(GWT_ROOT)build/staging/gwt-linux-0.0.0/

##
# The location to get .class files from for javah
##
CLASSDIR := $(GWT_ROOT)build/out/dev/linux/bin

# use this instead if you want to use eclipse to build the class files during
# development
#CLASSDIR := $(GWT_ROOT)eclipse/dev/linux/bin

##
# Tools
##
CXX      := g++
AR       := ar
STRIP    := strip
LD       := $(CXX)
JAVAH    := $(JAVA_HOME)/bin/javah

##
# List of source, object, and dependency paths plus the path to them
##
SRCDIRS := ./:../core/
VPATH   := .:../core
SRCS    := gwt-ll.cpp LowLevelMoz.cpp JsValueMoz.cpp Tracer.cpp \
	ExternalWrapper.cpp NativeWrapper.cpp JsRootedValue.cpp
OBJS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.o))
DEPS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.d))

MOZDIR = $(GWT_TOOLS)/sdk/mozilla-1.7.12
MOZINC = $(MOZDIR)/include
MOZLIB = $(MOZDIR)/lib

##
# Include path configuration
##
SYSINCS := \
    $(JAVA_HOME)/include \
    $(JAVA_HOME)/include/linux \
    $(MOZINC) $(MOZINC)/extra

INCS := $(OBJDIR)
INCS := $(addprefix -I ,$(INCS)) $(addprefix -isystem ,$(SYSINCS))

##
# Libraries and library path
##
LIBS    = xpcomglue_s
LIBPATH = -L$(MOZLIB)
LIBS     := $(addprefix -l,$(LIBS))

# for notes on auto-dependency generation, see
#   http://make.paulandlesley.org/autodep.html
# -MP obviates the need for sed hackery
CFLAGS   := -ggdb -m32 -Os -fPIC -fno-omit-frame-pointer -fno-strict-aliasing -D_REENTRANT -MMD -MP -Wno-system-headers $(CFLAGS)
LDFLAGS  := -ggdb -m32 -s -fPIC -fno-omit-frame-pointer -fno-strict-aliasing -D_REENTRANT -Wl,-shared-gcc $(LDFLAGS)

#-------------------------------------------------------------------------------
# Rules
#-------------------------------------------------------------------------------

##
# default rule
##
all: $(OUT)

# install into prebuilt directory
install: $(OUT)
	cp $(OUT) prebuilt/

##
# Include the dependency rules
##
-include $(DEPS)

##
# javah-generated headers for native methods
##
$(OBJDIR)LowLevelMoz.h : $(CLASSDIR)/com/google/gwt/dev/shell/moz/LowLevelMoz.class
	$(JAVAH) -classpath $(CLASSDIR) -o $(OBJDIR)LowLevelMoz.h com.google.gwt.dev.shell.moz.LowLevelMoz

$(OBJDIR)JsValueMoz.h : $(CLASSDIR)/com/google/gwt/dev/shell/moz/JsValueMoz.class
	$(JAVAH) -classpath $(CLASSDIR) -o $(OBJDIR)JsValueMoz.h com.google.gwt.dev.shell.moz.JsValueMoz

##
# Dependency rules for generated headers
# TODO(jat): autogenerate these and others
##
$(OBJDIR)LowLevelMoz.o: $(OBJDIR)LowLevelMoz.h $(JAVA_HOME)/include/jni.h
$(OBJDIR)JsValueMoz.o: $(OBJDIR)JsValueMoz.h $(JAVA_HOME)/include/jni.h

##
# Compilation rule for cpp files
##
$(OBJDIR)%.o : $(SRCDIR)%.cpp
	@[ -d $(OBJDIR) ] || mkdir -p $(OBJDIR)
	$(CXX) -c $(CFLAGS) $(INCS) -o $@ $<

# generate preprocessed output files upon request for debugging
%.i : $(SRCDIR)%.cpp
	$(CXX) -E $(CFLAGS) $(INCS) -o $@ $<

%.I : $(SRCDIR)%.cpp
	$(CXX) -E -dDI $(CFLAGS) $(INCS) -o $@ $<

##
# Actual output file
##
$(OUT): $(OBJS) $(MOZLIB)/libxpcomglue_s.a
	@[ -d $(OUTDIR) ] || mkdir -p $(OUTDIR)
	$(LD) -shared $(LDFLAGS) $(LIBPATH) -o $@ $^ $(LIBS)
	$(STRIP) --strip-unneeded $@

##
# copy to staging area for hosted-mode development
##
staging: $(OUT)
	@[ -d $(STAGING) ] || mkdir -p $(STAGING)
	cp $(OUT) $(STAGING)

##
# Clean rule
##
clean:
	@-rm -rf $(OBJDIR) $(OUT)
