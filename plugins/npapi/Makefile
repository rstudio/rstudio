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
DLLFLAGS = -bundle $(ALLARCHCFLAGS)
CFLAGS += $(ALLARCHCFLAGS)
CXXFLAGS += $(ALLARCHCFLAGS)
# Mac puts multiple architectures into the same files
TARGET_PLATFORM = Darwin-gcc3
else
ifeq ($(OS),linux)
RUN_PATH_FLAG = -rpath-link
DLL_SUFFIX = .so
DLLFLAGS = -shared -m$(FLAG32BIT)
TARGET_PLATFORM = Linux_$(ARCH)-gcc3
else
ifeq ($(OS),sun)
TARGET_PLATFORM = SunOS_$(ARCH)-sunc
RUN_PATH_FLAG = -rpath-link
DLLFLAGS=
endif
endif
endif


export FLAG32BIT

CFLAGS += -DBROWSER_NPAPI -fshort-wchar
CXXFLAGS = $(CXXONLYFLAGS) $(CFLAGS)
DIR = $(shell pwd)

DEPEND = g++ -MM -MT'$$(OBJ_OUTDIR)/$(patsubst %.cpp,%.o,$(src))' \
  -I. -I../common $(src)

COMMON            = ../common/libcommon$(FLAG32BIT).a

OBJ_OUTDIR        = build/$(TARGET_PLATFORM)
EXTENSION_OUTDIR  = prebuilt/gwt-dev-plugin
PLATFORM_DIR      = $(EXTENSION_OUTDIR)/$(TARGET_PLATFORM)

INSTALLER_CRX     = prebuilt/gwt-dev-plugin.crx
DLL               = $(OBJ_OUTDIR)/libGwtDevPlugin$(DLL_SUFFIX)

#DLLFLAGS += 

#VERSION ?= 0.9.$(shell ./getversion).$(shell date +%Y%m%d%H%M%S)
VERSION ?= 0.9.$(shell ./getversion)

.PHONY: default all crx lib common clean depend install install-platform \
	versioned-files

default:: lib versioned-files

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

$(INSTALLER_CRX): $(EXTENSION_OUTDIR) versioned-files
	@mkdir -p $(EXTENSION_OUTDIR)/components
	#(cd prebuilt/extension; find . \( -name .svn -prune \) -o -print | cpio -pmdua ../../$(EXTENSION_OUTDIR))
	-rm $(INSTALLER_CRX)
	(cd $(EXTENSION_OUTDIR) && zip -r -D -9 $(DIR)/$(INSTALLER_CRX) * -x '*/.svn/*' -x 'META-INF/*')

versioned-files::
	sed -e s/GWT_DEV_PLUGIN_VERSION/$(VERSION)/ manifest-template.json >prebuilt/gwt-dev-plugin/manifest.json
	#sed -e s/GWT_DEV_PLUGIN_VERSION/$(VERSION)/ updates-template.xml >prebuilt/updates.xml

$(DLL): $(OBJS) $(COMMON)
	$(CXX) -m$(FLAG32BIT) -o $@ $(OBJS) $(COMMON) $(DLLFLAGS) 
	@mkdir -p $(PLATFORM_DIR)
	cp $(DLL) $(PLATFORM_DIR)/

$(OBJ_OUTDIR)/%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c -o $@ -I. -I../common $<

common $(COMMON):
	(cd ../common && $(MAKE))

clean:
	rm -rf build

install-platform:
	@-mkdir -p $(PLATFORM_DIR)
	-cp $(DLL) $(PLATFORM_DIR)/
ifeq ($(OS),mac)
	@-mkdir -p $(subst $(EXTENSION_OUTDIR),prebuilt/extension-$(BROWSER),$(subst x86,ppc,$(PLATFORM_DIR)))/components
	-cp $(DLL) $(subst $(EXTENSION_OUTDIR),prebuilt/extension-$(BROWSER),$(subst x86,ppc,$(PLATFORM_DIR)))/components
endif

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
