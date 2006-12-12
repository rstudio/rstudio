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
OBJDIR  := $(GWT_ROOT)build/out/jni/linux/
OUTDIR  := $(GWT_ROOT)build/jni/linux/
OUT     := $(OUTDIR)libgwt-ll.so

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
SRCDIRS := ./:../core/
VPATH   := .:../core
SRCS    := gwt-ll.cpp Moz.cpp
OBJS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.o))
DEPS    := $(addprefix $(OBJDIR),$(SRCS:.cpp=.d))

##
# Include path configuration
##
SYSINCS := \
    $(JAVA_HOME)/include \
    $(JAVA_HOME)/include/linux \
	$(GWT_TOOLS)/sdk/mozilla-1.7.12/include \
	$(GWT_TOOLS)/sdk/mozilla-1.7.12/include/extra

INCS := $(addprefix -i ,$(INCS)) $(addprefix -isystem ,$(SYSINCS))

##
# Libraries and library path
##
LIBS    = xpcomglue_s
LIBPATH = -L$(GWT_TOOLS)/sdk/mozilla-1.7.12/lib
LIBS     := $(addprefix -l,$(LIBS))

# for notes on auto-dependency generation, see
#   http://make.paulandlesley.org/autodep.html
# -MP obviates the need for sed hackery
CFLAGS   := -Os -fPIC -fno-omit-frame-pointer -fno-strict-aliasing -D_REENTRANT -c -MMD -MP -Wno-system-headers $(CFLAGS)
LDFLAGS  := -s -fPIC -fno-omit-frame-pointer -fno-strict-aliasing -D_REENTRANT -Wl,-shared-gcc $(LDFLAGS)

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
	@[ -d $(OUTDIR) ] || mkdir -p $(OUTDIR)
	$(LD) -shared $(LDFLAGS) $(LIBPATH) -o $@ $^ $(LIBS)
	$(STRIP) --strip-unneeded $@

##
# Clean rule
##
clean:
	@-rm -rf $(OBJDIR) $(OUT)
