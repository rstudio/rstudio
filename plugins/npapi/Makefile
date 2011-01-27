# Copyright 2010 Google Inc.
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

ifeq ($(OS),mac)
RUN_PATH_FLAG = -executable_path
DLL_SUFFIX = .dylib
DLLFLAGS = -bundle $(ARCHCFLAGS) -mmacosx-version-min=10.5
CFLAGS += -DXP_MACOSX $(ARCHCFLAGS)
CXXFLAGS += -DXP_MACOSX $(ARCHCFLAGS)
# Mac puts multiple architectures into the same files
TARGET_PLATFORM = Darwin-gcc3
CHROME_PATH ?= /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome
PLATFORM_DIR_SUFFIX = gwtDev.plugin/Contents/MacOS/
else
ifeq ($(OS),linux)
RUN_PATH_FLAG = -rpath-link
DLL_SUFFIX = .so
DLLFLAGS = -shared -m$(FLAG32BIT)
TARGET_PLATFORM = Linux_$(ARCH)-gcc3
CHROME_PATH ?= /opt/google/chrome/chrome
else
endif
endif


export FLAG32BIT

CFLAGS += -DBROWSER_NPAPI -DXP_UNIX -fshort-wchar
CXXFLAGS = $(CXXONLYFLAGS) $(CFLAGS)
DIR = $(shell pwd)

DEPEND = g++ -MM -MT'$$(OBJ_OUTDIR)/$(patsubst %.cpp,%.o,$(src))' \
  -I. -I../common $(src)

COMMON            = ../common/libcommon$(FLAG32BIT).a

OBJ_OUTDIR        = build/$(TARGET_PLATFORM)
EXTENSION_OUTDIR  = prebuilt/gwt-dev-plugin
PLATFORM_DIR      = $(EXTENSION_OUTDIR)/$(TARGET_PLATFORM)
PLATFORM_DIR_SUFFIX ?= ""

INSTALLER_CRX     = prebuilt/gwt-dev-plugin.crx
DLL               = $(OBJ_OUTDIR)/libGwtDevPlugin$(DLL_SUFFIX)
GWTDEV_CRX_PEM   ?= $(CURDIR)/gwt-dev-plugin.pem
CHROME_COMMAND    = $(CHROME_PATH) --pack-extension=$(CURDIR)/$(EXTENSION_OUTDIR) --no-message-box --user-data-dir=$(CURDIR)/$(OBJ_OUTDIR)/user-data --pack-extension-key=$(GWTDEV_CRX_PEM)

#DLLFLAGS += 

VERSION ?= 1.0.$(shell ./getversion)
ifeq ($(VERSION), 1.0.)
$(error You must specify the version if you are not in an svn checkout)
endif

.PHONY: default all crx lib common clean depend install install-platform \
	versioned-files devmodeoptions

default:: lib versioned-files devmodeoptions

all:: common lib install-platform crx

lib:: $(OBJ_OUTDIR) $(EXTENSION_OUTDIR) $(DLL)
crx:: $(EXTENSION_OUTDIR) $(INSTALLER_CRX)

linuxplatforms:
	$(MAKE) lib ARCH=x86
	$(MAKE) lib ARCH=x86_64

macplatforms:
	$(MAKE) lib

HDRS =	\
	LocalObjectTable.h \
	NPVariantWrapper.h \
	Plugin.h \
	ScriptableInstance.h \
	mozincludes.h \
	JavaObject.h

SRCS =	\
	main.cpp \
	Plugin.cpp \
	LocalObjectTable.cpp \
	JavaObject.cpp \
	npn_bindings.cpp \
	ScriptableInstance.cpp

OBJS = $(patsubst %.cpp,$(OBJ_OUTDIR)/%.o,$(SRCS))

$(OBJS): $(OBJ_OUTDIR)

$(OBJ_OUTDIR)::
	@mkdir -p $@

$(INSTALLER_CRX): $(GWTDEV_CRX_PEM) $(EXTENSION_OUTDIR) versioned-files
	$(shell $(CHROME_COMMAND))

versioned-files::
	sed -e s/GWT_DEV_PLUGIN_VERSION/$(VERSION)/ manifest-template.json >prebuilt/gwt-dev-plugin/manifest.json
	#sed -e s/GWT_DEV_PLUGIN_VERSION/$(VERSION)/ updates-template.xml >prebuilt/updates.xml

$(DLL): $(OBJS) $(COMMON)
	$(CXX) -m$(FLAG32BIT) -o $@ $(OBJS) $(COMMON) $(DLLFLAGS) 
	@mkdir -p $(PLATFORM_DIR)
	cp $(DLL) $(PLATFORM_DIR)/$(PLATFORM_DIR_SUFFIX)

$(OBJ_OUTDIR)/%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c -o $@ -I. -I../common $<

common $(COMMON):
	(cd ../common && $(MAKE))

clean:
	rm -rf build

devmodeoptions:
	(ant -f DevModeOptions/build.xml prebuilt)

depend: $(OBJ_OUTDIR)
	($(foreach src,$(SRCS),$(DEPEND)) true) >>Makefile
#	makedepend -- $(CFLAGS) -- $(SRCS)

# DO NOT DELETE
$(OBJ_OUTDIR)/main.o $(OBJ_OUTDIR)/Plugin.o \
 $(OBJ_OUTDIR)/LocalObjectTable.o $(OBJ_OUTDIR)/JavaObject.o \
 $(OBJ_OUTDIR)/npn_bindings.cpp $(OBJ_OUTDIR)/ScriptableInstance.o: \
 main.cpp ../common/Debug.h ../common/Platform.h ../common/DebugLevel.h \
 mozincludes.h npapi/npapi.h npapi/nphostapi.h npapi/npapi.h \
 npapi/npruntime.h npapi/npruntime.h NPObjectWrapper.h Plugin.h \
 ../common/HostChannel.h ../common/Debug.h ../common/ByteOrder.h \
 ../common/Socket.h ../common/Message.h ../common/ReturnMessage.h \
 ../common/BrowserChannel.h ../common/Value.h ../common/SessionHandler.h \
 ../common/LoadModuleMessage.h ../common/HostChannel.h LocalObjectTable.h \
 ../common/SessionHandler.h ../common/HashMap.h ScriptableInstance.h \
 ../common/scoped_ptr/scoped_ptr.h
$(OBJ_OUTDIR)/main.o $(OBJ_OUTDIR)/Plugin.o \
 $(OBJ_OUTDIR)/LocalObjectTable.o $(OBJ_OUTDIR)/JavaObject.o \
 $(OBJ_OUTDIR)/npn_bindings.cpp $(OBJ_OUTDIR)/ScriptableInstance.o: \
 Plugin.cpp Plugin.h ../common/Debug.h ../common/Platform.h \
 ../common/DebugLevel.h mozincludes.h npapi/npapi.h npapi/nphostapi.h \
 npapi/npapi.h npapi/npruntime.h npapi/npruntime.h NPObjectWrapper.h \
 ../common/HostChannel.h ../common/Debug.h ../common/ByteOrder.h \
 ../common/Socket.h ../common/Message.h ../common/ReturnMessage.h \
 ../common/BrowserChannel.h ../common/Value.h ../common/SessionHandler.h \
 ../common/LoadModuleMessage.h ../common/HostChannel.h LocalObjectTable.h \
 ../common/SessionHandler.h ../common/HashMap.h ScriptableInstance.h \
 ../common/InvokeMessage.h ../common/ReturnMessage.h \
 ../common/ServerMethods.h ../common/scoped_ptr/scoped_ptr.h \
 NPVariantWrapper.h ../common/Platform.h ../common/Value.h JavaObject.h
$(OBJ_OUTDIR)/main.o $(OBJ_OUTDIR)/Plugin.o \
 $(OBJ_OUTDIR)/LocalObjectTable.o $(OBJ_OUTDIR)/JavaObject.o \
 $(OBJ_OUTDIR)/npn_bindings.cpp $(OBJ_OUTDIR)/ScriptableInstance.o: \
 LocalObjectTable.cpp mozincludes.h npapi/npapi.h npapi/nphostapi.h \
 npapi/npapi.h npapi/npruntime.h npapi/npruntime.h NPObjectWrapper.h \
 LocalObjectTable.h ../common/Debug.h ../common/Platform.h \
 ../common/DebugLevel.h
$(OBJ_OUTDIR)/main.o $(OBJ_OUTDIR)/Plugin.o \
 $(OBJ_OUTDIR)/LocalObjectTable.o $(OBJ_OUTDIR)/JavaObject.o \
 $(OBJ_OUTDIR)/npn_bindings.cpp $(OBJ_OUTDIR)/ScriptableInstance.o: \
 JavaObject.cpp ../common/Debug.h ../common/Platform.h \
 ../common/DebugLevel.h JavaObject.h mozincludes.h npapi/npapi.h \
 npapi/nphostapi.h npapi/npapi.h npapi/npruntime.h npapi/npruntime.h \
 NPObjectWrapper.h Plugin.h ../common/HostChannel.h ../common/Debug.h \
 ../common/ByteOrder.h ../common/Socket.h ../common/Message.h \
 ../common/ReturnMessage.h ../common/BrowserChannel.h ../common/Value.h \
 ../common/SessionHandler.h ../common/LoadModuleMessage.h \
 ../common/HostChannel.h LocalObjectTable.h ../common/SessionHandler.h \
 ../common/HashMap.h ScriptableInstance.h NPVariantWrapper.h \
 ../common/Platform.h ../common/Value.h
$(OBJ_OUTDIR)/main.o $(OBJ_OUTDIR)/Plugin.o \
 $(OBJ_OUTDIR)/LocalObjectTable.o $(OBJ_OUTDIR)/JavaObject.o \
 $(OBJ_OUTDIR)/npn_bindings.cpp $(OBJ_OUTDIR)/ScriptableInstance.o: \
 npn_bindings.cpp ../common/Debug.h ../common/Platform.h \
 ../common/DebugLevel.h mozincludes.h npapi/npapi.h npapi/nphostapi.h \
 npapi/npapi.h npapi/npruntime.h npapi/npruntime.h NPObjectWrapper.h
$(OBJ_OUTDIR)/main.o $(OBJ_OUTDIR)/Plugin.o \
 $(OBJ_OUTDIR)/LocalObjectTable.o $(OBJ_OUTDIR)/JavaObject.o \
 $(OBJ_OUTDIR)/npn_bindings.cpp $(OBJ_OUTDIR)/ScriptableInstance.o: \
 ScriptableInstance.cpp ScriptableInstance.h ../common/Debug.h \
 ../common/Platform.h ../common/DebugLevel.h mozincludes.h npapi/npapi.h \
 npapi/nphostapi.h npapi/npapi.h npapi/npruntime.h npapi/npruntime.h \
 NPObjectWrapper.h ../common/HostChannel.h ../common/Debug.h \
 ../common/ByteOrder.h ../common/Socket.h ../common/Message.h \
 ../common/ReturnMessage.h ../common/BrowserChannel.h ../common/Value.h \
 ../common/SessionHandler.h ../common/LoadModuleMessage.h \
 ../common/HostChannel.h LocalObjectTable.h ../common/SessionHandler.h \
 ../common/HashMap.h ../common/InvokeMessage.h ../common/ReturnMessage.h \
 ../common/ServerMethods.h ../common/AllowedConnections.h \
 ../common/scoped_ptr/scoped_ptr.h NPVariantWrapper.h \
 ../common/Platform.h ../common/Value.h Plugin.h JavaObject.h
