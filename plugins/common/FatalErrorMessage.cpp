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

#include "FatalErrorMessage.h"
#include "HostChannel.h"
#include "scoped_ptr/scoped_ptr.h"

FatalErrorMessage::~FatalErrorMessage() {
}

char FatalErrorMessage::getType() const {
  return TYPE;
}

/**
 * Receive an FatalError message from the channel (note that the message
 * type has already been read).  Caller is responsible for destroying
 * returned message.  Returns null on error.
 */
FatalErrorMessage* FatalErrorMessage::receive(HostChannel& channel) {
  std::string error;
  if (!channel.readString(error)) {
    // TODO(jat): error handling
    printf("Failed to read error message\n");
    return 0;
  }
  return new FatalErrorMessage(error);
}

/**
 * Send a fatal error message on the channel.
 */
bool FatalErrorMessage::send(HostChannel& channel, const std::string& error) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendString(error)) return false;
  return true;
}
