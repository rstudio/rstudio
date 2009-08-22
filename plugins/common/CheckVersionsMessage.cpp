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

#include "CheckVersionsMessage.h"
#include "HostChannel.h"
#include "scoped_ptr/scoped_ptr.h"

CheckVersionsMessage::~CheckVersionsMessage() {
}

char CheckVersionsMessage::getType() const {
  return TYPE;
}

/**
 * Receive a CheckVersions message from the channel (note that the message
 * type has already been read).  Caller is responsible for destroying
 * returned message.  Returns null on error.
 */
CheckVersionsMessage* CheckVersionsMessage::receive(HostChannel& channel) {
  int minVersion;
  if (!channel.readInt(minVersion)) {
    // TODO(jat): error handling
    printf("Failed to read minimum version\n");
    return 0;
  }
  int maxVersion;
  if (!channel.readInt(maxVersion)) {
    // TODO(jat): error handling
    printf("Failed to read maximum version\n");
    return 0;
  }
  std::string hostedHtmlVersion;
  if (!channel.readString(hostedHtmlVersion)) {
    // TODO(jat): error handling
    printf("Failed to read hosted.html version\n");
    return 0;
  }
  return new CheckVersionsMessage(minVersion, maxVersion, hostedHtmlVersion);
}

/**
 * Send a fatal error message on the channel.
 */
bool CheckVersionsMessage::send(HostChannel& channel, int minVersion,
    int maxVersion, const std::string& hostedHtmlVersion) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendInt(minVersion)) return false;
  if (!channel.sendInt(maxVersion)) return false;
  if (!channel.sendString(hostedHtmlVersion)) return false;
  return true;
}
