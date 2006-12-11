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
# External WebKit products.
## 
WEBKIT_REDIST=$(GWT_TOOLS)/redist/webkit/WebKit-418.9.tar.gz
WEBKIT_INCLUDE=$(GWT_TOOLS)/sdk/WebKit-418.9

##
# External SWT products.
##
SWT_ORIGINAL_SRC = $(GWT_TOOLS)/lib/eclipse/org.eclipse.swt.carbon-macosx-3.2.1.src.zip
SWT_PATCH_DIR = ./swt-build
SWT_LIBS=$(SWT_PATCH_DIR)/libswt-webkit-carbon-3235.jnilib \
	$(SWT_PATCH_DIR)/libswt-agl-carbon-3235.jnilib \
	$(SWT_PATCH_DIR)/libswt-carbon-3235.jnilib \
	$(SWT_PATCH_DIR)/libswt-pi-carbon-3235.jnilib


##
# Built products.
##
GWT_LL_LIB=libgwt-ll.jnilib
GWT_WEBKIT_LIB=libgwt-webkit.jnilib

##
# Tools.
##
CC = g++
TAR = tar
SHELL = /bin/sh
FIX_INSTALL_NAME = $(SHELL) ./fix-install-name.sh
LOCALIZE_FRAMEWORK_INSTALL_NAMES = /usr/bin/env ruby ./localize-framework-install-names.rb

##
# Compile configuration.
##
ARCHS = -arch i386 -arch ppc
CFLAGS = -Wall -c $(ARCHS) -DCARBON -I/System/Library/Frameworks/JavaVM.framework/Headers -fno-exceptions -fno-rtti
LFLAGS = -bundle $(ARCHS) -isysroot /Developer/SDKs/MacOSX10.4u.sdk

##
# JavaScriptCore options.
##
JSCORE_CFLAGS = $(CFLAGS)  -I$(WEBKIT_INCLUDE)/JavaScriptCore
JSCORE_LFLAGS = $(LFLAGS) -framework JavaScriptCore -F./Frameworks
JSCORE_INSTALL_NAME = "@loader_path/Frameworks/JavaScriptCore.framework/Versions/A/JavaScriptCore"
WEBCORE_INSTALL_NAME = "@loader_path/Frameworks/WebCore.framework/Versions/A/WebCore"
WEBKIT_INSTALL_NAME = "@loader_path/Frameworks/WebKit.framework/Versions/A/WebKit"

##
# Intermediates
##
GWT_LL_OBJECTS = gwt-ll.o gwt-args.o
GWT_WEBKIT_OBJECTS = gwt-webkit.o DispWrapper.o FuncWrapper.o FunctionObject.o

#-------------------------------------------------------------------------------
# Rules
#-------------------------------------------------------------------------------

##
# Default rule.
##
all: $(GWT_LL_LIB) $(GWT_WEBKIT_LIB) $(SWT_LIBS)

##
# Copy WebKit binary frameworks locally.
##
Frameworks: $(WEBKIT_REDIST)
	$(TAR) -zxvf $(WEBKIT_REDIST)
	$(LOCALIZE_FRAMEWORK_INSTALL_NAMES)

##
# Rule for cpp files.
##
%.o: %.cpp
	$(CC) -c -o $@ $< $(JSCORE_CFLAGS)

##
# Rule for gwt-ll objects.
##
gwt-ll.o: ../core/gwt-ll.cpp
	$(CC) -c -o gwt-ll.o $(CFLAGS) ../core/gwt-ll.cpp

gwt-args.o: gwt-args.cpp
	$(CC) -c -o gwt-args.o $(CFLAGS) gwt-args.cpp

##
# Rule for final lib for gwt-ll.
##
$(GWT_LL_LIB): $(GWT_LL_OBJECTS)
	$(CC) -o $(GWT_LL_LIB) $(LFLAGS) $(GWT_LL_OBJECTS)

##
# Rule for final lib for gwt-webkit.
##
$(GWT_WEBKIT_LIB): Frameworks $(GWT_WEBKIT_OBJECTS)
	$(CC) -o $(GWT_WEBKIT_LIB) $(JSCORE_LFLAGS) $(GWT_WEBKIT_OBJECTS)
	$(FIX_INSTALL_NAME) JavaScriptCore $(JSCORE_INSTALL_NAME) $(GWT_WEBKIT_LIB)

##
# Unpack and patch SWT source.
##
$(SWT_PATCH_DIR): $(SWT_ORIGINAL_SRC) ./org.eclipse.swt/webkit.c ./org.eclipse.swt/make_macosx.mak
	unzip $(SWT_ORIGINAL_SRC) -d $(SWT_PATCH_DIR)
	cp ./org.eclipse.swt/webkit.c ./org.eclipse.swt/make_macosx.mak $(SWT_PATCH_DIR)

##
# Build SWT.
##
$(SWT_LIBS):$(SWT_PATCH_DIR) Frameworks
	make -C $(SWT_PATCH_DIR) -f make_macosx.mak
	$(FIX_INSTALL_NAME) WebKit $(WEBKIT_INSTALL_NAME) $(SWT_PATCH_DIR)/libswt-webkit-carbon-3235.jnilib
	$(FIX_INSTALL_NAME) WebCore $(WEBCORE_INSTALL_NAME) $(SWT_PATCH_DIR)/libswt-webkit-carbon-3235.jnilib
	$(FIX_INSTALL_NAME) JavaScriptCore $(JSCORE_INSTALL_NAME) $(SWT_PATCH_DIR)/libswt-webkit-carbon-3235.jnilib

##
# Clean rule.
##
clean:
	@-rm -f $(GWT_LL_LIB) $(GWT_WEBKIT_LIB) *.o
	@-rm -rf ./Frameworks $(SWT_PATCH_DIR)
