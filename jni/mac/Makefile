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
# Built products.
##
GWT_LL_LIB=libgwt-ll.jnilib
GWT_WEBKIT_LIB=libgwt-webkit.jnilib

##
# Tools.
##
CC = g++
TAR = tar
FIX_INSTALL_NAME = ./fix-install-name.sh
SHELL = /bin/sh

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
all: $(GWT_LL_LIB) $(GWT_WEBKIT_LIB)

##
# Copy WebKit binary frameworks locally.
##
Frameworks: $(WEBKIT_REDIST)
	$(TAR) -zxvf $(WEBKIT_REDIST)

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
	$(SHELL) $(FIX_INSTALL_NAME) $(JSCORE_INSTALL_NAME)

##
# Clean rule.
##
clean:
	@-rm -f $(GWT_LL_LIB) $(GWT_WEBKIT_LIB) *.o
	@-rm -rf ./Frameworks
