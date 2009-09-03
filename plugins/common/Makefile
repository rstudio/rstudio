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

HDRS= HostChannel.h InvokeMessage.h LoadModuleMessage.h Message.h \
      ReturnMessage.h Value.h BrowserChannel.h Debug.h DebugLevel.h \
      SessionHandler.h ServerMethods.h Socket.h AllowedConnections.h \
      LoadJsniMessage.h InvokeSpecialMessage.h FreeValueMessage.h \
      ByteOrder.h FatalErrorMessage.h CheckVersionsMessage.h \
      ChooseTransportMessage.h SwitchTransportMessage.h \
      ProtocolVersionMessage.h

SRCS=	HostChannel.cpp LoadModuleMessage.cpp InvokeMessage.cpp \
	ReturnMessage.cpp ServerMethods.cpp Debug.cpp Socket.cpp \
	AllowedConnections.cpp LoadJsniMessage.cpp InvokeSpecialMessage.cpp \
	FreeValueMessage.cpp FatalErrorMessage.cpp CheckVersionsMessage.cpp \
      ChooseTransportMessage.cpp SwitchTransportMessage.cpp \
      ProtocolVersionMessage.cpp

LIBCOMMON= libcommon$(FLAG32BIT).a
OBJDIR= obj$(FLAG32BIT)
OBJS= $(patsubst %.cpp,$(OBJDIR)/%.o,$(SRCS))

all:: $(OBJDIR) $(LIBCOMMON)

$(OBJDIR):
	-mkdir $@

$(LIBCOMMON): $(OBJS)
	$(AR) $(ARFLAGS) $@ $(OBJS)

$(OBJDIR)/%.o: %.cpp
	$(CXX) $(BASECFLAGS) $(ALLARCHCFLAGS) -c $< -o $@

$(OBJDIR)/%.o: DebugLevel.h

.PHONY: clean depend testdebug

testdebug:
	(cd testing; CXX=$(CXX) CXXFLAGS="$(CFLAGS) -m32" ./testdebug)
	(cd testing; CXX=$(CXX) CXXFLAGS="$(CFLAGS) -m64" ./testdebug)

clean:
	rm -rf obj32 obj64 libcommon32.a libcommon64.a

depend:
	g++ -MM $(CFLAGS) $(SRCS) >>Makefile
#	makedepend -- $(CFLAGS) -- $(SRCS)

# DO NOT DELETE
HostChannel.o: HostChannel.cpp Debug.h Platform.h DebugLevel.h \
  ByteOrder.h CheckVersionsMessage.h Message.h BrowserChannel.h Value.h \
  ProtocolVersionMessage.h ChooseTransportMessage.h \
  SwitchTransportMessage.h FreeValueMessage.h HostChannel.h Socket.h \
  AllowedConnections.h ReturnMessage.h SessionHandler.h LoadJsniMessage.h \
  InvokeMessage.h InvokeSpecialMessage.h QuitMessage.h \
  scoped_ptr/scoped_ptr.h
LoadModuleMessage.o: LoadModuleMessage.cpp Debug.h Platform.h \
  DebugLevel.h LoadModuleMessage.h Message.h BrowserChannel.h \
  HostChannel.h ByteOrder.h Socket.h AllowedConnections.h ReturnMessage.h \
  Value.h SessionHandler.h scoped_ptr/scoped_ptr.h
InvokeMessage.o: InvokeMessage.cpp InvokeMessage.h Message.h \
  BrowserChannel.h Value.h Debug.h Platform.h DebugLevel.h HostChannel.h \
  ByteOrder.h Socket.h AllowedConnections.h ReturnMessage.h \
  SessionHandler.h scoped_ptr/scoped_ptr.h
ReturnMessage.o: ReturnMessage.cpp ReturnMessage.h Message.h \
  BrowserChannel.h Value.h Debug.h Platform.h DebugLevel.h HostChannel.h \
  ByteOrder.h Socket.h AllowedConnections.h SessionHandler.h
ServerMethods.o: ServerMethods.cpp Debug.h Platform.h DebugLevel.h \
  FreeValueMessage.h Message.h BrowserChannel.h HostChannel.h ByteOrder.h \
  Socket.h AllowedConnections.h ReturnMessage.h Value.h SessionHandler.h \
  InvokeMessage.h InvokeSpecialMessage.h ServerMethods.h \
  scoped_ptr/scoped_ptr.h
Debug.o: Debug.cpp Debug.h Platform.h DebugLevel.h
Socket.o: Socket.cpp Platform.h Socket.h Debug.h DebugLevel.h
AllowedConnections.o: AllowedConnections.cpp Debug.h Platform.h \
  DebugLevel.h AllowedConnections.h
LoadJsniMessage.o: LoadJsniMessage.cpp LoadJsniMessage.h Message.h \
  BrowserChannel.h HostChannel.h Debug.h Platform.h DebugLevel.h \
  ByteOrder.h Socket.h AllowedConnections.h ReturnMessage.h Value.h \
  SessionHandler.h
InvokeSpecialMessage.o: InvokeSpecialMessage.cpp InvokeSpecialMessage.h \
  Message.h BrowserChannel.h SessionHandler.h Value.h Debug.h Platform.h \
  DebugLevel.h HostChannel.h ByteOrder.h Socket.h AllowedConnections.h \
  ReturnMessage.h scoped_ptr/scoped_ptr.h
FreeValueMessage.o: FreeValueMessage.cpp FreeValueMessage.h Message.h \
  BrowserChannel.h HostChannel.h Debug.h Platform.h DebugLevel.h \
  ByteOrder.h Socket.h AllowedConnections.h ReturnMessage.h Value.h \
  SessionHandler.h scoped_ptr/scoped_ptr.h
FatalErrorMessage.o: FatalErrorMessage.cpp FatalErrorMessage.h Message.h \
  BrowserChannel.h Value.h Debug.h Platform.h DebugLevel.h HostChannel.h \
  ByteOrder.h Socket.h AllowedConnections.h ReturnMessage.h \
  SessionHandler.h scoped_ptr/scoped_ptr.h
CheckVersionsMessage.o: CheckVersionsMessage.cpp CheckVersionsMessage.h \
  Message.h BrowserChannel.h Value.h Debug.h Platform.h DebugLevel.h \
  HostChannel.h ByteOrder.h Socket.h AllowedConnections.h ReturnMessage.h \
  SessionHandler.h scoped_ptr/scoped_ptr.h
ChooseTransportMessage.o: ChooseTransportMessage.cpp \
  ChooseTransportMessage.h Message.h BrowserChannel.h Value.h Debug.h \
  Platform.h DebugLevel.h HostChannel.h ByteOrder.h Socket.h \
  AllowedConnections.h ReturnMessage.h SessionHandler.h \
  scoped_ptr/scoped_ptr.h
SwitchTransportMessage.o: SwitchTransportMessage.cpp \
  SwitchTransportMessage.h Message.h BrowserChannel.h Value.h Debug.h \
  Platform.h DebugLevel.h HostChannel.h ByteOrder.h Socket.h \
  AllowedConnections.h ReturnMessage.h SessionHandler.h \
  scoped_ptr/scoped_ptr.h
ProtocolVersionMessage.o: ProtocolVersionMessage.cpp \
  ProtocolVersionMessage.h Message.h BrowserChannel.h Value.h Debug.h \
  Platform.h DebugLevel.h HostChannel.h ByteOrder.h Socket.h \
  AllowedConnections.h ReturnMessage.h SessionHandler.h \
  scoped_ptr/scoped_ptr.h
