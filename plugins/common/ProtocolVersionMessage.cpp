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

#include "ProtocolVersionMessage.h"
#include "HostChannel.h"
#include "scoped_ptr/scoped_ptr.h"

ProtocolVersionMessage::~ProtocolVersionMessage() {
}

char ProtocolVersionMessage::getType() const {
  return TYPE;
}

/**
 * Receive a ProtocolVersion message from the server.
 */
ProtocolVersionMessage* ProtocolVersionMessage::receive(HostChannel& channel) {
  int version;
  if (!channel.readInt(version)) {
    // TODO(jat): error handling
    printf("Failed to read version\n");
    return 0;
  }
  return new ProtocolVersionMessage(version);
}

/**
 * Send a ProtocolVersion message on the channel.
 */
bool ProtocolVersionMessage::send(HostChannel& channel, int version) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendInt(version)) return false;
  return true;
}
