# Copyright 2006 Google Inc.
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
GWT_ROOT = ../../
OBJDIR  := $(GWT_ROOT)build/out/jni/win32/
OUTDIR  := $(GWT_ROOT)build/jni/win32/
OUT     := $(OUTDIR)gwt-ll.dll
STAGING := $(GWT_ROOT)build/staging/gwt-windows-0.0.0/

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
SRCDIRS := ./:../core/
VPATH   := .:../core
SRCS    := gwt-ll.cpp IE6.cpp
OBJS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.o))
DEPS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.d))

##
# Include path configuration
##
SYSINCS := \
    $(JAVA_HOME)/include \
    $(JAVA_HOME)/include/win32
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
$(OUT): $(OBJS) $(OBJDIR)libwininet.a
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
