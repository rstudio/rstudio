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

include ../config.mk

# Make variables intended to be settable from the command line:
#   DEFAULT_FIREFOX_LIBS	points to /usr/lib/firefox or equivalent
#   PLUGIN_SDKS			points to GWT /plugin-sdks directory
#   GECKO_PLATFORM		XPCOM ABI (ie, Linux_x86_64-gcc3)
#

ifeq ($(OS),mac)
DEFAULT_FIREFOX_LIBS ?= /Applications/Firefox.app/Contents/MacOS
RUN_PATH_FLAG = -executable_path
DLL_SUFFIX = .dylib
DLLFLAGS += -bundle $(ALLARCHCFLAGS) -mmacosx-version-min=10.5
CFLAGS += $(ALLARCHCFLAGS)
CXXFLAGS += $(ALLARCHCFLAGS)
TARGET_PLATFORM = Darwin-gcc3
# Mac puts multiple architectures into the same files
GECKO_PLATFORM = Darwin-gcc3
else
ifeq ($(OS),linux)
DEFAULT_FIREFOX_LIBS ?= /usr/lib/firefox
RUN_PATH_FLAG = -rpath-link
DLL_SUFFIX = .so
DLLFLAGS += -shared -m$(FLAG32BIT)
TARGET_PLATFORM = Linux_$(ARCH)-gcc3
else
ifeq ($(OS),sun)
TARGET_PLATFORM = SunOS_$(ARCH)-sunc
RUN_PATH_FLAG = -rpath-link
DLLFLAGS ?= ""
endif
endif
endif

export FLAG32BIT

ifeq ($(BROWSER),)
$(warning Defaulting to FF3 build [set with BROWSER=ff3, ff3+, ff35, ff36, ff40, ff50, ff60, ff70, ff80, ff90, ff100, f110, ff120, ff130, ff140])
BROWSER=ff3
endif

# Gold doesn't play nice with xpcom libs
LINKER=$(shell ld -v | awk '{print $$2}')
ifeq ($(LINKER), gold)
ifeq ($(wildcard /usr/lib/compat-ld),)
$(error cannot link with gold, use the BFD ld instead)
else
CFLAGS += -B/usr/lib/compat-ld/
CXXFLAGS += -B/usr/lib/compat-ld/
DLLFLAGS += -B/usr/lib/compat-ld/
endif
endif

CFLAGS += -DBROWSER=$(BROWSER)
GECKO_MINOR_VERSION=
GECKO_DLLFLAGS = -lxpcomglue_s -lxpcom
NSPR_DLLFLAGS = -lnspr4
MOZJS_DLLFLAGS = -lxul

#defaults to post-FF4
MOZALLOC_DLLFLAGS = -lmozalloc

#default ALLARCHFLAGS for post-FF4
ALLARCHCFLAGS = -arch i386 -arch x86_64 -Xarch_i386 -DFLAG32BIT=32 -Xarch_x86_64 -DFLAG32BIT=64

# Python xpidl tool is the new hotness post-FF9
XPIDL_TOOL = python

ifeq ($(BROWSER),ff3)
XPIDL_TOOL = binary
MOZALLOC_DLLFLAGS =
GECKO_VERSION   = 1.9.0
CFLAGS += -DGECKO_VERSION=1900
MOZJS_DLLFLAGS = -lmozjs
ifeq ($(OS),mac)
ALLARCHCFLAGS = -arch i386 -arch ppc
endif
else
ifeq ($(BROWSER),ff3+)
XPIDL_TOOL = binary
MOZALLOC_DLLFLAGS =
GECKO_VERSION   = 1.9.0
CFLAGS += -DGECKO_VERSION=1901
GECKO_MINOR_VERSION=.10
MOZJS_DLLFLAGS = -lmozjs
ifeq ($(OS),mac)
ALLARCHCFLAGS = -arch i386 -arch ppc
endif
else
ifeq ($(BROWSER),ff35)
XPIDL_TOOL = binary
MOZALLOC_DLLFLAGS =
GECKO_VERSION   = 1.9.1
CFLAGS += -DGECKO_VERSION=1910
MOZJS_DLLFLAGS = -lmozjs
ifeq ($(OS),mac)
ALLARCHCFLAGS = -arch i386 -arch ppc
endif
else
ifeq ($(BROWSER),ff36)
XPIDL_TOOL = binary
MOZALLOC_DLLFLAGS =
GECKO_VERSION   = 1.9.2
CFLAGS += -DGECKO_VERSION=1920
MOZJS_DLLFLAGS = -lmozjs
ifeq ($(OS),mac)
ALLARCHCFLAGS = -arch i386 -arch ppc
endif
else
ifeq ($(BROWSER),ff40)
XPIDL_TOOL = binary
GECKO_VERSION   = 2.0.0
CFLAGS += -DGECKO_VERSION=2000
else
ifeq ($(BROWSER),ff50)
XPIDL_TOOL = binary
GECKO_VERSION   = 5.0.0
CFLAGS += -DGECKO_VERSION=5000
else
ifeq ($(BROWSER),ff60)
XPIDL_TOOL = binary
GECKO_VERSION   = 6.0.0
CFLAGS += -DGECKO_VERSION=6000
else
ifeq ($(BROWSER),ff70)
XPIDL_TOOL = binary
GECKO_VERSION   = 7.0.0
CFLAGS += -DGECKO_VERSION=7000
else
ifeq ($(BROWSER),ff80)
XPIDL_TOOL = binary
GECKO_VERSION   = 8.0.0
CFLAGS += -DGECKO_VERSION=8000
else
ifeq ($(BROWSER),ff90)
GECKO_VERSION   = 9.0.0
CFLAGS += -DGECKO_VERSION=9000
else
ifeq ($(BROWSER),ff100)
GECKO_VERSION   = 10.0.0
CFLAGS += -DGECKO_VERSION=10000
else
ifeq ($(BROWSER),ff110)
GECKO_VERSION   = 11.0.0
CFLAGS += -DGECKO_VERSION=11000
else
ifeq ($(BROWSER),ff120)
GECKO_VERSION   = 12.0.0
CFLAGS += -DGECKO_VERSION=12000
else
ifeq ($(BROWSER),ff130)
GECKO_VERSION   = 13.0.0
CFLAGS += -DGECKO_VERSION=13000
else
ifeq ($(BROWSER),ff140)
GECKO_VERSION   = 14.0.0
CFLAGS += -DGECKO_VERSION=14000
else
$(error Unrecognized BROWSER of $(BROWSER) - options are ff3, ff3+, ff35, ff36, ff40, ff50, ff60, ff70, ff80, ff90, ff100, ff110, ff120, ff130)
endif
endif
endif
endif
endif
endif
endif
endif
endif
endif
endif
endif
endif
endif
endif

CFLAGS += -fshort-wchar
CXXFLAGS = $(CXXONLYFLAGS) $(CFLAGS)
DIR = $(shell pwd)

# Set $PLUGIN_SDKS if it isn't in the default location
PLUGIN_SDKS ?= ../../../plugin-sdks
GECKO_PLATFORM ?= $(TARGET_PLATFORM)

COMMON            = ../common/libcommon$(FLAG32BIT).a

OBJ_OUTDIR        = build/$(TARGET_PLATFORM)-$(BROWSER)
EXTENSION_OUTDIR  = prebuilt/extension
FF_PLATFORM_DIR   = $(EXTENSION_OUTDIR)/lib/$(TARGET_PLATFORM)/$(BROWSER)

INSTALLER_XPI     = prebuilt/gwt-dev-plugin.xpi
FF_DLL            = $(OBJ_OUTDIR)/libgwt_dev_$(BROWSER)$(DLL_SUFFIX)
#FF_TYPELIB        = build/IOOPHM.xpt
#FF_HEADER         = $(OBJ_OUTDIR)/IOOPHM.h
FF_TYPELIB        = prebuilt/extension/components/IOOPHM.xpt
FF_HEADER         = prebuilt/$(BROWSER)/include/IOOPHM.h
INSTALL_RDF       = $(EXTENSION_OUTDIR)/install.rdf

SDK_PATH          = $(PLUGIN_SDKS)/gecko-sdks
GECKO_SDK         = $(SDK_PATH)/gecko-$(GECKO_VERSION)
GECKO_PLAT_INC    = $(GECKO_SDK)/$(GECKO_PLATFORM)/include
GECKO_LIBS        = $(GECKO_SDK)/$(GECKO_PLATFORM)/lib$(GECKO_MINOR_VERSION)

ifeq ($(XPIDL_TOOL),python)
XPIDL             = $(GECKO_SDK)/bin/xpidl.py
XPIDL_HEADER      = $(GECKO_SDK)/bin/header.py
XPIDL_TYPELIBS    = $(GECKO_SDK)/bin/typelib.py
else
XPIDL             = $(GECKO_SDK)/$(GECKO_PLATFORM)/bin/xpidl
XPIDL_HEADER      = $(GECKO_SDK)/$(GECKO_PLATFORM)/bin/xpidl -m header
XPIDL_TYPELIBS    = $(GECKO_SDK)/$(GECKO_PLATFORM)/bin/xpidl -m typelib
endif

XPIDL_FLAGS       = -I$(GECKO_SDK)/idl

DLLFLAGS += \
		-L$(GECKO_LIBS) \
		-Wl,$(RUN_PATH_FLAG),$(GECKO_LIBS) \
		$(GECKO_DLLFLAGS) $(NSPR_DLLFLAGS) \
		$(MOZALLOC_DLLFLAGS) $(MOZJS_DLLFLAGS)

INC += -I$(GECKO_PLAT_INC) -I$(GECKO_SDK)/include -I$(dir $(FF_HEADER))

VERSION ?= 1.0.$(shell ./getversion).$(shell date +%Y%m%d%H%M%S)

.PHONY: all xpi lib common browser clean depend install install-platform find-ff-libs

all:: common lib xpi

lib:: browser $(OBJ_OUTDIR) $(EXTENSION_OUTDIR) $(FF_DLL)
xpi:: $(EXTENSION_OUTDIR) $(INSTALLER_XPI)

find-ff-libs::
ifeq ($(FIREFOX_LIBS),)
	$(warning Using firefox libraries at $(GECKO_LIBS))
	$(eval FIREFOX_LIBS = $(GECKO_LIBS))
endif

browser:: find-ff-libs
#	if [ ! -r $(GECKO_LIBS)/libxpcom.so ]
#	then
#	    $(error Missing Firefox libraries at $(GECKO_LIBS))
#	fi

generate-install:: $(EXTENSION_OUTDIR) install-template.rdf
	sed -e s/GWT_DEV_PLUGIN_VERSION/$(VERSION)/ install-template.rdf >$(INSTALL_RDF)

linuxplatforms:
	$(MAKE) lib BROWSER=ff3 ARCH=x86
	$(MAKE) lib BROWSER=ff3+ ARCH=x86
	$(MAKE) lib BROWSER=ff35 ARCH=x86
	$(MAKE) lib BROWSER=ff36 ARCH=x86
	$(MAKE) lib BROWSER=ff40 ARCH=x86
	$(MAKE) lib BROWSER=ff50 ARCH=x86
	$(MAKE) lib BROWSER=ff60 ARCH=x86
	$(MAKE) lib BROWSER=ff70 ARCH=x86
	$(MAKE) lib BROWSER=ff80 ARCH=x86
	$(MAKE) lib BROWSER=ff90 ARCH=x86
	$(MAKE) lib BROWSER=ff100 ARCH=x86
	$(MAKE) lib BROWSER=ff110 ARCH=x86
	$(MAKE) lib BROWSER=ff120 ARCH=x86
	$(MAKE) lib BROWSER=ff130 ARCH=x86
	$(MAKE) lib BROWSER=ff140 ARCH=x86
	$(MAKE) lib BROWSER=ff3 ARCH=x86_64
	$(MAKE) lib BROWSER=ff3+ ARCH=x86_64
	$(MAKE) lib BROWSER=ff35 ARCH=x86_64
	$(MAKE) lib BROWSER=ff36 ARCH=x86_64
	$(MAKE) lib BROWSER=ff40 ARCH=x86_64
	$(MAKE) lib BROWSER=ff50 ARCH=x86_64
	$(MAKE) lib BROWSER=ff60 ARCH=x86_64
	$(MAKE) lib BROWSER=ff70 ARCH=x86_64
	$(MAKE) lib BROWSER=ff80 ARCH=x86_64
	$(MAKE) lib BROWSER=ff90 ARCH=x86_64
	$(MAKE) lib BROWSER=ff100 ARCH=x86_64
	$(MAKE) lib BROWSER=ff110 ARCH=x86_64
	$(MAKE) lib BROWSER=ff120 ARCH=x86_64
	$(MAKE) lib BROWSER=ff130 ARCH=x86_64
	$(MAKE) lib BROWSER=ff140 ARCH=x86_64

macplatforms:
	$(MAKE) lib BROWSER=ff3
	$(MAKE) lib BROWSER=ff35
	$(MAKE) lib BROWSER=ff36
	$(MAKE) lib BROWSER=ff40
	$(MAKE) lib BROWSER=ff50
	$(MAKE) lib BROWSER=ff60
	$(MAKE) lib BROWSER=ff70
	$(MAKE) lib BROWSER=ff80
	$(MAKE) lib BROWSER=ff90
	$(MAKE) lib BROWSER=ff100
	$(MAKE) lib BROWSER=ff110
	$(MAKE) lib BROWSER=ff120
	$(MAKE) lib BROWSER=ff130
	$(MAKE) lib BROWSER=ff140

SRCS =	\
		ExternalWrapper.cpp \
		ModuleOOPHM.cpp \
		FFSessionHandler.cpp \
		JavaObject.cpp \
		JSRunner.cpp \
		Preferences.cpp \
		XpcomDebug.cpp

FF_OBJS = $(patsubst %.cpp,$(OBJ_OUTDIR)/%.o,$(SRCS))

$(FF_OBJS): $(OBJ_OUTDIR)

$(OBJ_OUTDIR)::
	@mkdir -p $@

$(INSTALLER_XPI): $(FF_TYPELIB) $(EXTENSION_OUTDIR) generate-install $(shell find prebuilt/extension $(EXTENSION_OUTDIR))
	@mkdir -p $(EXTENSION_OUTDIR)/components
	#(cd prebuilt/extension; find . \( -name .svn -prune \) -o -print | cpio -pmdua ../../$(EXTENSION_OUTDIR))
	-rm $(INSTALLER_XPI)
	(cd $(EXTENSION_OUTDIR) && zip -r -D -9 $(DIR)/$(INSTALLER_XPI) * -x '*/.svn/*' -x 'META-INF/*')

$(FF_TYPELIB): IOOPHM.idl
	[ ! -x $(XPIDL) -o \( -e $(FF_TYPELIB) -a ! -w $(FF_TYPELIB) \) ] || $(XPIDL_TYPELIBS) $(XPIDL_FLAGS) -o $@ $<

$(FF_HEADER): IOOPHM.idl $(OBJ_OUTDIR)
	[ ! -x $(XPIDL) -o \( -e $(FF_HEADER) -a ! -w $(FF_HEADER) \) ] || $(XPIDL_HEADER) $(XPIDL_FLAGS) -o $@ $<

$(FF_DLL): $(FF_OBJS) $(COMMON)
	$(CXX) -o $@ $(FF_OBJS) $(COMMON) $(DLLFLAGS)
	@mkdir -p $(FF_PLATFORM_DIR)
	cp $(FF_DLL) $(FF_PLATFORM_DIR)/

$(OBJ_OUTDIR)/%.o: %.cpp $(FF_HEADER)
	$(CXX) $(CXXFLAGS) -c -o $@ -I. -I../common $<

common $(COMMON):
	(cd ../common && $(MAKE))

clean:
	rm -rf build
	(cd ../common && $(MAKE) clean)

install-platform:
ifdef BROWSER
	@-mkdir -p $(FF_PLATFORM_DIR)
	-cp $(FF_DLL) $(FF_PLATFORM_DIR))/
ifeq ($(OS),mac)
	@-mkdir -p $(subst $(EXTENSION_OUTDIR),prebuilt/extension-$(BROWSER),$(subst x86,ppc,$(FF_PLATFORM_DIR)))/components
	-cp $(FF_DLL) $(subst $(EXTENSION_OUTDIR),prebuilt/extension-$(BROWSER),$(subst x86,ppc,$(FF_PLATFORM_DIR)))/components
endif
else
	@$(MAKE) $@ BROWSER=ff3
	@$(MAKE) $@ BROWSER=ff3+
	@$(MAKE) $@ BROWSER=ff35
	@$(MAKE) $@ BROWSER=ff36
	@$(MAKE) $@ BROWSER=ff40
	@$(MAKE) $@ BROWSER=ff50
endif

DEPEND = g++ -MM -MT'$$(OBJ_OUTDIR)/$(patsubst %.cpp,%.o,$(src))' \
  -I. -I../common -isystem$(dir $(FF_HEADER)) -isystem$(GECKO_SDK)/include $(src) &&
depend: browser $(OBJ_OUTDIR) $(FF_HEADER)
	($(foreach src,$(SRCS),$(DEPEND)) true) >>Makefile
#	makedepend -- $(CFLAGS) -- $(SRCS)

# DO NOT DELETE
$(OBJ_OUTDIR)/ExternalWrapper.o: ExternalWrapper.cpp ExternalWrapper.h \
  mozincludes.h FFSessionHandler.h SessionData.h \
  ../common/SessionHandler.h ../common/BrowserChannel.h ../common/Value.h \
  ../common/Debug.h ../common/Platform.h ../common/DebugLevel.h \
  ../common/BrowserChannel.h ../common/scoped_ptr/scoped_ptr.h \
  ../common/LoadModuleMessage.h ../common/Message.h \
  ../common/BrowserChannel.h ../common/HostChannel.h ../common/Debug.h \
  ../common/ByteOrder.h ../common/Platform.h ../common/Socket.h \
  ../common/Platform.h ../common/Debug.h ../common/AllowedConnections.h \
  ../common/Platform.h ../common/Message.h ../common/ReturnMessage.h \
  ../common/Message.h ../common/BrowserChannel.h ../common/Value.h \
  ../common/Value.h ../common/SessionHandler.h ../common/SessionHandler.h \
  ../common/ServerMethods.h ../common/Value.h
$(OBJ_OUTDIR)/ModuleOOPHM.o: ModuleOOPHM.cpp ../common/Debug.h \
  ../common/Platform.h ../common/DebugLevel.h ExternalWrapper.h \
  mozincludes.h FFSessionHandler.h SessionData.h \
  ../common/SessionHandler.h ../common/BrowserChannel.h ../common/Value.h \
  ../common/Debug.h ../common/BrowserChannel.h \
  ../common/scoped_ptr/scoped_ptr.h
$(OBJ_OUTDIR)/FFSessionHandler.o: FFSessionHandler.cpp FFSessionHandler.h \
  mozincludes.h SessionData.h ../common/SessionHandler.h \
  ../common/BrowserChannel.h ../common/Value.h ../common/Debug.h \
  ../common/Platform.h ../common/DebugLevel.h ../common/BrowserChannel.h \
  ../common/HostChannel.h ../common/Debug.h ../common/ByteOrder.h \
  ../common/Platform.h ../common/Socket.h ../common/Platform.h \
  ../common/Debug.h ../common/AllowedConnections.h ../common/Platform.h \
  ../common/Message.h ../common/ReturnMessage.h ../common/Message.h \
  ../common/BrowserChannel.h ../common/Value.h ../common/Value.h \
  ../common/SessionHandler.h JavaObject.h JSRunner.h XpcomDebug.h \
  ../common/scoped_ptr/scoped_ptr.h RootedObject.h \
  ../common/InvokeMessage.h ../common/Message.h \
  ../common/BrowserChannel.h ../common/Value.h ../common/ServerMethods.h \
  ../common/Value.h
$(OBJ_OUTDIR)/JavaObject.o: JavaObject.cpp JavaObject.h mozincludes.h \
  FFSessionHandler.h SessionData.h ../common/SessionHandler.h \
  ../common/BrowserChannel.h ../common/Value.h ../common/Debug.h \
  ../common/Platform.h ../common/DebugLevel.h ../common/BrowserChannel.h \
  ../common/ServerMethods.h ../common/Value.h XpcomDebug.h \
  ../common/HostChannel.h ../common/Debug.h ../common/ByteOrder.h \
  ../common/Platform.h ../common/Socket.h ../common/Platform.h \
  ../common/Debug.h ../common/AllowedConnections.h ../common/Platform.h \
  ../common/Message.h ../common/ReturnMessage.h ../common/Message.h \
  ../common/BrowserChannel.h ../common/Value.h ../common/Value.h \
  ../common/SessionHandler.h ../common/InvokeMessage.h \
  ../common/Message.h ../common/BrowserChannel.h ../common/Value.h \
  ../common/scoped_ptr/scoped_ptr.h
$(OBJ_OUTDIR)/JSRunner.o: JSRunner.cpp JSRunner.h mozincludes.h
$(OBJ_OUTDIR)/XpcomDebug.o: XpcomDebug.cpp XpcomDebug.h mozincludes.h \
  JavaObject.h
