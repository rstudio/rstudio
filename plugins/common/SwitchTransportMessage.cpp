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

#include "SwitchTransportMessage.h"
#include "HostChannel.h"
#include "scoped_ptr/scoped_ptr.h"

SwitchTransportMessage::~SwitchTransportMessage() {
}

char SwitchTransportMessage::getType() const {
  return TYPE;
}

/**
 * Receive a SwitchTransport message from the server.
 */
SwitchTransportMessage* SwitchTransportMessage::receive(HostChannel& channel) {
  std::string transport;
  if (!channel.readString(transport)) {
    // TODO(jat): error handling
    printf("Failed to read transport\n");
    return 0;
  }
  return new SwitchTransportMessage(transport);
}

/**
 * Send a fatal error message on the channel.
 */
bool SwitchTransportMessage::send(HostChannel& channel,
    const std::string& transport) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendString(transport)) return false;
  return true;
}
