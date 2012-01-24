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

#include "InvokeSpecialMessage.h"
#include "HostChannel.h"
#include "SessionHandler.h"
#include "scoped_ptr/scoped_ptr.h"

InvokeSpecialMessage::~InvokeSpecialMessage() {
  delete[] args;
}

char InvokeSpecialMessage::getType() const {
  return TYPE;
}

/**
 * Receive an InvokeSpecial message from the channel (note that the message
 * type has already been read).  Caller is responsible for destroying
 * returned message.  Returns null on error.
 */
InvokeSpecialMessage* InvokeSpecialMessage::receive(HostChannel& channel) {
  char dispatchId;
  if (!channel.readByte(dispatchId)) {
    // TODO(jat): error handling
    printf("Failed to read method name\n");
    return 0;
  }
  int numArgs;
  if (!channel.readInt(numArgs)) {
    // TODO(jat): error handling
    printf("Failed to read #args\n");
    return 0;
  }
  scoped_array<gwt::Value> args(new gwt::Value[numArgs]);
  for (int i = 0; i < numArgs; ++i) {
    if (!channel.readValue(args[i])) {
      // TODO(jat): error handling
      printf("Failed to read arg[%d]\n", i);
      return 0;
    }
  }
  
  SessionHandler::SpecialMethodId id =
    static_cast<SessionHandler::SpecialMethodId>(dispatchId);
  return new InvokeSpecialMessage(id, numArgs, args.release());
}

/**
 * Request the server perform a special method and return the result.
 */
bool InvokeSpecialMessage::send(HostChannel& channel, int dispatchId,
    int numArgs, const gwt::Value* args) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendByte(dispatchId)) return false;
  if (!channel.sendInt(numArgs)) return false;
  for (int i = 0; i < numArgs; ++i) {
    if (!channel.sendValue(args[i])) return false;
  }
  return true;
}
