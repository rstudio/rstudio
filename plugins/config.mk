# Copyright 2009 Google Inc.
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

# This Makefile fragment sets the following make variables according to the
# current platform:
#   ARCH - the Mozilla architecture name, such as x86, x86_64, ppc, etc
#   FLAG32BIT - 32 or 64
#   MARCH - the Mac architecture, such as i386 or ppc
#   OS - linux, mac, or sun
#   CFLAGS - appropriate C compiler flags for this platform
#   CXXFLAGS - appropriate C++ compiler flags for this platform
# Also, various stanard make variables are overridden if necessary, such as AR
#
# If ARCH is already set, that is used instead of uname -m to get the
# architecture to build.  This can be used to build a 32-bit plugin on a 64-bit
# platform, for example: make ARCH=x86

ARCH ?= $(shell uname -m)

# default is 32 bits
FLAG32BIT=32

# Figure out 64-bit platforms, canonicalize ARCH and MARCH
ifeq ($(ARCH),x86_64)
FLAG32BIT=64
endif
ifeq ($(ARCH),sparc)
FLAG32BIT=64
endif
ifeq ($(ARCH),alpha)
FLAG32BIT=64
endif
ifeq ($(ARCH),ia64)
FLAG32BIT=64
endif
ifeq ($(ARCH),athlon)
ARCH=x86
endif
ifeq ($(ARCH),i386)
ARCH=x86
endif
ifeq ($(ARCH),i486)
ARCH=x86
endif
ifeq ($(ARCH),i586)
ARCH=x86
endif
ifeq ($(ARCH),i686)
ARCH=x86
endif
ifeq ($(ARCH),i86pc)
ARCH=x86
endif
ifeq ($(ARCH),Macintosh)
ARCH=ppc
endif

MARCH=$(ARCH)
ifeq ($(ARCH),x86)
MARCH=i386
endif

# Default to Debug off
DEBUG ?=
ifeq ($(DEBUG),TRUE)
DEBUGCFLAGS= -g
else
DEBUGCFLAGS=
endif

# Set OS as well as CFLAGS, CXX, and other common make variables
ifeq ($(shell uname),Linux)
OS=linux
BASECFLAGS= $(DEBUGCFLAGS) -O2 -fPIC $(INC) -rdynamic
ARCHCFLAGS= -m$(FLAG32BIT)
ALLARCHCFLAGS= -m$(FLAG32BIT)
endif
ifeq ($(shell uname),Darwin)
OS=mac
BASECFLAGS= $(DEBUGCFLAGS) -O2 -fPIC $(INC) -D__mac -mmacosx-version-min=10.5
ARCHCFLAGS=-arch $(MARCH)
ALLARCHCFLAGS=-arch i386 -arch ppc -arch x86_64
AR=libtool
ARFLAGS=-static -o
endif
ifeq ($(shell uname),SunOS)
OS=sun
ifeq ($(DEBUG),TRUE)
DEBUGCFLAGS= -g0
endif
#CFLAGS=-fast -g0 -Kpic $(INC) -Bdynamic -noex
# SunC appears to miscompile Socket::writeByte by not incrementing the
# buffer pointer, so no optimization for now
#CFLAGS=-g -Kpic $(INC) -Bdynamic -noex
BASECFLAGS= $(DEBUGCFLAGS) -Kpic -noex -xO1 -xlibmil -xlibmopt -features=tmplife -xbuiltin=%all -mt $(INC)
ARCHCFLAGS=
ALLARCHCFLAGS=
CXX= CC
endif
CFLAGS=$(BASECFLAGS) $(ARCHCFLAGS)
CXXFLAGS = $(CFLAGS) 
