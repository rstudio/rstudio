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

#include "InvokeMessage.h"
#include "HostChannel.h"
#include "scoped_ptr/scoped_ptr.h"

InvokeMessage::~InvokeMessage() {
  delete[] args;
}

char InvokeMessage::getType() const {
  return TYPE;
}

/**
 * Receive an Invoke message from the channel (note that the message
 * type has already been read).  Caller is responsible for destroying
 * returned message.  Returns null on error.
 */
InvokeMessage* InvokeMessage::receive(HostChannel& channel) {
  std::string methodName;
  if (!channel.readString(methodName)) {
    // TODO(jat): error handling
    printf("Failed to read method name\n");
    return 0;
  }
  gwt::Value thisRef;
  if (!channel.readValue(thisRef)) {
    // TODO(jat): error handling
    printf("Failed to read thisRef\n");
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
  return new InvokeMessage(thisRef, methodName, numArgs, args.release());
}

/**
 * Request the server invoke a method on an object and return the result.
 * 
 * Note that the method to be invoked is sent as an integer (a dispatch id) to the server.
 */
bool InvokeMessage::send(HostChannel& channel, const gwt::Value& thisRef, int methodDispatchId,
    int numArgs, const gwt::Value* args) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendInt(methodDispatchId)) return false;
  if (!channel.sendValue(thisRef)) return false;
  if (!channel.sendInt(numArgs)) return false;
  for (int i = 0; i < numArgs; ++i) {
    if (!channel.sendValue(args[i])) return false;
  }
  return true;
}
