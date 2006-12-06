# NOTE: THIS MAKEFILE DOES NOT WORK IN OUR BUILD SYSTEM. IT IS USED
# TO DO THE ISOLATED BUILD OF THE MAC LIBS AND DEPENDS ON A DIFFERENT
# DIRECTORY STRUCTURE. IT IS PUT HERE FOR ARCHIVE ONLY.

GWT_LIB=../dist/libgwt-ll.jnilib

CC = g++
ARCHS = -arch i386 -arch ppc
CFLAGS = -Wall -c $(ARCHS) -DCARBON -I /System/Library/Frameworks/JavaVM.framework/Headers -fno-exceptions -fno-rtti
LFLAGS = -bundle $(ARCHS) -isysroot /Developer/SDKs/MacOSX10.4u.sdk
WEBKITCFLAGS = $(CFLAGS)  -I../WebKit-15921/JavaScriptCore
WEBKITLFLAGS = $(LFLAGS) -framework JavaScriptCore -F../dist/Frameworks
GWT_OBJECTS = gwt-ll.o
WEBKIT_OBJECTS = gwt-webkit.o DispWrapper.o FuncWrapper.o FunctionObject.o

all: $(GWT_LIB)

%.o: %.cpp
	$(CC) -c -o $@ $< $(WEBKITCFLAGS)

gwt-ll.o: ../gwt-ll-core/gwt-ll.cpp
	$(CC) $(CFLAGS) gwt-ll.cpp

$(GWT_LIB): $(GWT_OBJECTS) $(WEBKIT_OBJECTS)
	$(CC) -o $(WEBKIT_LIB) $(WEBKITLFLAGS) $(WEBKIT_OBJECTS)

install: all
	cp *.jnilib $(OUTPUT_DIR)

clean:
	rm -f $(GWT_LIB) $(WEBKIT_LIB) *.o
