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

#include <cstdarg>
#include <vector>
#include "ChooseTransportMessage.h"
#include "HostChannel.h"
#include "scoped_ptr/scoped_ptr.h"

ChooseTransportMessage::~ChooseTransportMessage() {
}

char ChooseTransportMessage::getType() const {
  return TYPE;
}

/**
 * Receive an ChooseTransport message from the channel (note that the message
 * type has already been read).  Caller is responsible for destroying
 * returned message.  Returns null on error.
 */
ChooseTransportMessage* ChooseTransportMessage::receive(HostChannel& channel) {
  int length;
  if (!channel.readInt(length)) {
    // TODO(jat): error handling
    printf("Failed to read transport\n");
    return 0;
  }
  std::vector<std::string> transports;
  for (int i = 0; i < length; ++i) {
    std::string transport;
    if (!channel.readString(transport)) {
      // TODO(jat): error handling
      printf("Failed to read transport\n");
      return 0;
    }
    transports.push_back(transport);
  }
  return new ChooseTransportMessage(transports);
}

/**
 * Send this ChooseTransport message on the channel.
 */
bool ChooseTransportMessage::send(HostChannel& channel,
    const std::vector<std::string>& transports) {
  if (!channel.sendByte(TYPE)) return false;
  int n = transports.size();
  if (!channel.sendInt(n)) return false;
  for (int i = 0; i < n; ++i) {
    if (!channel.sendString(transports[i])) return false;
  }
  return true;
}
