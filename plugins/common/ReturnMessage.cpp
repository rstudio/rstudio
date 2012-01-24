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

#include "ReturnMessage.h"
#include "HostChannel.h"

char ReturnMessage::getType() const {
  return TYPE;
}
  
ReturnMessage* ReturnMessage::receive(HostChannel& channel) {
  char isException;
  if (!channel.readByte(isException)) {
    // TODO(jat): error handling
    return 0;
  }
  gwt::Value retval;
  if (!channel.readValue(retval)) {
    // TODO(jat): error handling
    return 0;
  }
  return new ReturnMessage(isException != 0, retval);
}

bool ReturnMessage::send(HostChannel& channel, bool isException, const gwt::Value& retval) {
  if (!channel.sendByte(TYPE)) return false;
  if (!channel.sendByte(isException ? 1 : 0)) return false;
  return channel.sendValue(retval);
}
