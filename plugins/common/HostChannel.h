#ifndef __H_HostChannel
#define __H_HostChannel
/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include "Debug.h"

// Sun's cstdio doesn't define a bunch of stuff
#include <stdio.h>
#include <string>

#include "ByteOrder.h"
#include "Socket.h"
#include "Platform.h"
#include "Message.h"
#include "ReturnMessage.h"
#include "Value.h"
#include "SessionHandler.h"

class HostChannel {
  Socket sock;
  static ByteOrder byteOrder;
  SessionHandler* handler;

public:
  ~HostChannel() {
    if (isConnected()) {
      disconnectFromHost();
    }
    Debug::log(Debug::Debugging) << "HostChannel destroyed" << Debug::flush;
  }

  // Connects to the OOPHM server (socket operations only).
  bool connectToHost(const char* host, unsigned port);

  // Negotiates protocol version and transport selection.
  bool init(SessionHandler* handler, int minVersion, int maxVersion,
      const std::string& hostedHtmlVersion);

  bool disconnectFromHost();

  bool isConnected() const {
    return sock.isConnected();
  }

  bool readBytes(void* data, size_t dataLen) {
    char* ptr = static_cast<char*>(data);
    while(dataLen > 0) {
      if (!readByte(*ptr++)) {
        return false;
      }
      --dataLen;
    }
    return true;
  }

  bool sendBytes(const void* data, size_t dataLen) {
    const char* ptr = static_cast<const char*>(data);
    while(dataLen > 0) {
      if (!sendByte(*ptr++)) {
        return false;
      }
      --dataLen;
    }
    return true;
  }

  // TODO: don't pass out-params by reference as it makes the call site misleading
  bool readInt(int32_t& data);
  bool sendInt(const int32_t data);

  bool readShort(short& data);
  bool sendShort(const short data);

  bool readLong(int64_t& data);
  bool sendLong(const int64_t data);

  bool readFloat(float& data);
  bool sendFloat(const float data);

  bool readDouble(double& doubleRef);
  bool sendDouble(const double data);

  bool readByte(char& data) {
    if (!isConnected()) {
      handler->disconnectDetected();
      return false;
    }
    int c = sock.readByte();
    if (c < 0) {
      handler->disconnectDetected();
      return false;
    }
    data = static_cast<char>(c);
    return true;
  }

  bool sendByte(const char data) {
    if (!isConnected()) {
      handler->disconnectDetected();
      return false;
    }
    if (!sock.writeByte(data)) {
      handler->disconnectDetected();
      return false;
    }
    return true;
  }

  bool readStringLength(uint32_t& data);
  bool readStringBytes(char* data, const uint32_t len);
  bool readString(std::string& strRef);
  bool sendString(const char* data, const uint32_t len) {
    return sendInt(len) && sendBytes(data, len);
  }

  bool sendString(const std::string& str) {
    return sendString(str.c_str(), static_cast<uint32_t>(str.length()));
  }

  bool readValue(gwt::Value& valueRef);
  bool sendValue(const gwt::Value& value);

  ReturnMessage* reactToMessages(SessionHandler* handler, bool expectReturn);

  bool reactToMessages(SessionHandler* handler) {
    return !reactToMessages(handler, false);
  }

  bool flush() {
    if (!sock.isConnected()) {
      handler->disconnectDetected();
      return false;
    }
    if (!sock.flush()) {
      handler->disconnectDetected();
      return false;
    }
    return true;
  }

  ReturnMessage* reactToMessagesWhileWaitingForReturn(SessionHandler* handler) {
    return reactToMessages(handler, true);
  }
};
#endif
