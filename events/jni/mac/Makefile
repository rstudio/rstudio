# Copyright 2008 Google Inc.
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

CC = g++
JAVAH = javah

##
# Try a GWT_TOOLS default if it isn't set
##
GWT_TOOLS ?= ../../../tools
GWT_ROOT ?= ../..

##
# The location to get .class files for javah
##
CLASSDIR := $(GWT_ROOT)/build/out/dev/mac/bin

##
# External SWT products.
##
SWT_ORIGINAL_SRC = $(GWT_TOOLS)/lib/eclipse/org.eclipse.swt.carbon-macosx-3.2.1.src.zip
SWT_PATCH_DIR = ./swt-build
SWT_LIBS=$(SWT_PATCH_DIR)/libswt-webkit-carbon-3235.jnilib \
	$(SWT_PATCH_DIR)/libswt-agl-carbon-3235.jnilib \
	$(SWT_PATCH_DIR)/libswt-carbon-3235.jnilib \
	$(SWT_PATCH_DIR)/libswt-pi-carbon-3235.jnilib

GWT_LL_LIB = libgwt-ll.jnilib
GWT_LL_OBJS = gwt-ll.o java-dispatch.o gwt-webkit.o trace.o
GWT_LL_JNI = gwt-webkit.h

TARGETS = $(GWT_LL_LIB) $(SWT_LIBS)
ARCHS = -arch i386 -arch ppc
CFLAGS = -I/System/Library/Frameworks/JavaScriptCore.framework/Headers -I/System/Library/Frameworks/JavaVM.framework/Headers
LFLAGS = -bundle -framework JavaScriptCore

ALL : $(TARGETS)

##
# Unpack and patch SWT source.
##
$(SWT_PATCH_DIR): $(SWT_ORIGINAL_SRC) ./org.eclipse.swt/webkit.c
	unzip $(SWT_ORIGINAL_SRC) -d $(SWT_PATCH_DIR)
	cp ./org.eclipse.swt/webkit.c  $(SWT_PATCH_DIR)

##
# Build SWT.
##
$(SWT_LIBS):$(SWT_PATCH_DIR)
	make -C $(SWT_PATCH_DIR) -f make_macosx.mak

##
# Build core gwt-ll object.
##
gwt-ll.o : ../core/gwt-ll.cpp
	$(CC) -c -Wall -o $@ $(ARCHS) $(CFLAGS) $<

##
# General coff target.
##
%.o : %.cpp
	$(CC) -c -Wall -o $@ $(ARCHS) $(CFLAGS) $<

##
# Generate JNI Header
##
gwt-webkit.h : $(CLASSDIR)/com/google/gwt/dev/shell/mac/LowLevelSaf.class
	$(JAVAH) -classpath $(CLASSDIR) -o gwt-webkit.h com.google.gwt.dev.shell.mac.LowLevelSaf

##
# Build gwt-ll library.
##
$(GWT_LL_LIB) : $(GWT_LL_JNI) $(GWT_LL_OBJS)
	$(CC) -Wall -o $@ $(ARCHS) $(LFLAGS) $(GWT_LL_OBJS)

clean:
	rm -f $(GWT_LL_JNI) $(GWT_LL_OBJS) $(TARGETS)
	rm -rf $(SWT_PATCH_DIR)
